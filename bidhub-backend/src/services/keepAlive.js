/**
 * Keep-Alive Service for Render Cold Start Prevention
 * 
 * This service helps prevent the Render free tier from spinning down
 * by periodically pinging the health endpoint. When combined with external
 * cron jobs, this significantly reduces cold start delays.
 */

const { pool } = require('../config/database');
const AuctionEndService = require('./auctionEndService');

/**
 * Internal keep-alive ping to maintain connection pool
 * Runs every 5 minutes when enabled
 */
class KeepAliveService {
  constructor() {
    this.interval = null;
    this.auctionCheckInterval = null;
    this.enabled = process.env.KEEP_ALIVE_ENABLED === 'true';
    this.intervalMs = parseInt(process.env.KEEP_ALIVE_INTERVAL_MS) || 5 * 60 * 1000; // 5 minutes
    this.auctionCheckIntervalMs = parseInt(process.env.AUCTION_CHECK_INTERVAL_MS) || 60 * 1000; // 1 minute
  }

  /**
   * Start the keep-alive service
   */
  start() {
    if (!this.enabled) {
      console.log('Keep-alive service disabled via KEEP_ALIVE_ENABLED=false');
      return;
    }

    console.log(`Starting keep-alive service (interval: ${this.intervalMs / 1000}s)`);

    // Immediate ping on start
    this.ping();

    // Set up interval
    this.interval = setInterval(() => {
      this.ping();
    }, this.intervalMs);
    
    // Start auction end checking
    this.startAuctionEndChecking();
  }
  
  /**
   * Start checking for ended auctions periodically
   */
  startAuctionEndChecking() {
    console.log(`Starting auction end checking (interval: ${this.auctionCheckIntervalMs / 1000}s)`);
    
    // Check immediately on start
    this.checkEndedAuctions();
    
    // Set up interval
    this.auctionCheckInterval = setInterval(() => {
      this.checkEndedAuctions();
    }, this.auctionCheckIntervalMs);
  }
  
  /**
   * Check for ended auctions and process them
   */
  async checkEndedAuctions() {
    try {
      await AuctionEndService.processEndedAuctions();
    } catch (error) {
      console.error('[Keep-Alive] Error checking ended auctions:', error.message);
    }
  }

  /**
   * Stop the keep-alive service
   */
  stop() {
    if (this.interval) {
      clearInterval(this.interval);
      this.interval = null;
    }
    if (this.auctionCheckInterval) {
      clearInterval(this.auctionCheckInterval);
      this.auctionCheckInterval = null;
    }
    console.log('Keep-alive service stopped');
  }

  /**
   * Perform a keep-alive ping
   */
  async ping() {
    try {
      // Ping database to keep connection alive
      await pool.query('SELECT 1');

      if (process.env.NODE_ENV === 'development') {
        console.log('[Keep-Alive] Database ping successful');
      }
    } catch (error) {
      console.error('[Keep-Alive] Database ping failed:', error.message);
    }
  }

  /**
   * Trigger manual keep-alive ping
   */
  async manualPing() {
    await this.ping();
  }

  /**
   * Get service status
   */
  getStatus() {
    return {
      enabled: this.enabled,
      interval_ms: this.intervalMs,
      running: this.interval !== null
    };
  }
}

// Singleton instance
const keepAliveService = new KeepAliveService();

module.exports = keepAliveService;

