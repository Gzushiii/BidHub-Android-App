const { pool } = require('../config/database');
const BidHubNotificationManager = require('./notificationService');

/**
 * Service to detect ended auctions and notify winners
 * This service should be run periodically (e.g., via cron job or scheduled task)
 */
class AuctionEndService {
  /**
   * Check for ended auctions and process winners
   */
  static async processEndedAuctions() {
    const connection = await pool.getConnection();
    
    try {
      await connection.beginTransaction();
      
      // Find all active auctions that have ended
      const [endedAuctions] = await connection.query(
        `SELECT id, title, seller_id, seller_email, end_date, current_price, starting_price
         FROM items 
         WHERE status = 'active' 
         AND end_date <= NOW()`
      );
      
      console.log(`Found ${endedAuctions.length} ended auctions to process`);
      
      for (const item of endedAuctions) {
        await this.processAuctionEnd(connection, item);
      }
      
      await connection.commit();
      console.log('Successfully processed all ended auctions');
      
    } catch (error) {
      await connection.rollback();
      console.error('Error processing ended auctions:', error);
      throw error;
    } finally {
      connection.release();
    }
  }
  
  /**
   * Process a single ended auction
   */
  static async processAuctionEnd(connection, item) {
    try {
      // Find the highest bidder
      const [highestBids] = await connection.query(
        `SELECT b.id, b.bidder_id, b.bidder_email, b.amount, u.id as user_id, u.email, u.alias
         FROM bids b
         JOIN users u ON b.bidder_id = u.id
         WHERE b.item_id = ?
         ORDER BY b.amount DESC, b.placed_at ASC
         LIMIT 1`,
        [item.id]
      );
      
      if (highestBids.length === 0) {
        // No bids - mark item as ended with no winner
        await connection.query(
          `UPDATE items SET status = 'ended' WHERE id = ?`,
          [item.id]
        );
        console.log(`Auction ${item.id} ended with no bids`);
        return;
      }
      
      const winningBid = highestBids[0];
      
      // Update item status to ended
      await connection.query(
        `UPDATE items SET status = 'ended', winner_id = ?, winner_email = ? WHERE id = ?`,
        [winningBid.bidder_id, winningBid.bidder_email, item.id]
      );
      
      // Update winning bid status
      await connection.query(
        `UPDATE bids SET status = 'WINNING' WHERE id = ?`,
        [winningBid.id]
      );
      
      // Mark other bids as lost
      await connection.query(
        `UPDATE bids SET status = 'LOST' WHERE item_id = ? AND id != ?`,
        [item.id, winningBid.id]
      );
      
      // Send notification to winner
      await this.notifyAuctionWinner(connection, item, winningBid);
      
      // Send notifications to other bidders (they lost)
      await this.notifyAuctionLosers(connection, item, winningBid.bidder_id);
      
      console.log(`Auction ${item.id} ended. Winner: ${winningBid.bidder_email} (₱${winningBid.amount})`);
      
    } catch (error) {
      console.error(`Error processing auction end for item ${item.id}:`, error);
      throw error;
    }
  }
  
  /**
   * Notify auction winner
   */
  static async notifyAuctionWinner(connection, item, winningBid) {
    try {
      // Prepare notification payload
      const notificationData = {
        type: 'auction_won',
        item_id: item.id,
        item_title: item.title,
        winning_amount: winningBid.amount,
        next_steps: [
          'Contact the seller to arrange payment and delivery',
          'Complete payment within 48 hours',
          'Confirm delivery address with seller'
        ],
        seller_email: item.seller_email,
        seller_id: item.seller_id
      };
      
      // Store notification in database for the winner
      await connection.query(
        `INSERT INTO notifications 
         (user_id, user_email, type, title, message, data, created_at) 
         VALUES (?, ?, ?, ?, ?, ?, NOW())`,
        [
          winningBid.user_id,
          winningBid.email,
          'auction_won',
          'Congratulations! You Won the Auction',
          `You won "${item.title}" for ₱${winningBid.amount.toFixed(2)}`,
          JSON.stringify(notificationData)
        ]
      );
      
      // Send push notification (if notification service is available)
      if (BidHubNotificationManager) {
        await BidHubNotificationManager.sendAuctionWonNotification(
          winningBid.user_id,
          winningBid.email,
          item.title,
          winningBid.amount,
          notificationData
        );
      }
      
    } catch (error) {
      console.error('Error notifying auction winner:', error);
      // Don't throw - notification failure shouldn't block auction processing
    }
  }
  
  /**
   * Notify bidders who lost the auction
   */
  static async notifyAuctionLosers(connection, item, winnerId) {
    try {
      // Get all bidders except the winner
      const [losers] = await connection.query(
        `SELECT DISTINCT b.bidder_id, b.bidder_email, u.id as user_id, u.email
         FROM bids b
         JOIN users u ON b.bidder_id = u.id
         WHERE b.item_id = ? AND b.bidder_id != ?`,
        [item.id, winnerId]
      );
      
      for (const loser of losers) {
        const notificationData = {
          type: 'auction_lost',
          item_id: item.id,
          item_title: item.title
        };
        
        // Store notification
        await connection.query(
          `INSERT INTO notifications 
           (user_id, user_email, type, title, message, data, created_at) 
           VALUES (?, ?, ?, ?, ?, ?, NOW())`,
          [
            loser.user_id,
            loser.email,
            'auction_lost',
            'Auction Ended',
            `The auction for "${item.title}" has ended. You were outbid.`,
            JSON.stringify(notificationData)
          ]
        );
        
        // Send push notification
        if (BidHubNotificationManager) {
          await BidHubNotificationManager.sendAuctionLostNotification(
            loser.user_id,
            loser.email,
            item.title
          );
        }
      }
      
    } catch (error) {
      console.error('Error notifying auction losers:', error);
      // Don't throw - notification failure shouldn't block auction processing
    }
  }
  
  /**
   * Get auction winner details for an item
   */
  static async getAuctionWinner(itemId) {
    try {
      const [winners] = await pool.query(
        `SELECT i.winner_id, i.winner_email, b.amount as winning_bid, u.alias, u.email
         FROM items i
         LEFT JOIN bids b ON i.winner_id = b.bidder_id AND i.id = b.item_id
         LEFT JOIN users u ON i.winner_id = u.id
         WHERE i.id = ? AND i.status = 'ended'
         ORDER BY b.amount DESC
         LIMIT 1`,
        [itemId]
      );
      
      return winners.length > 0 ? winners[0] : null;
      
    } catch (error) {
      console.error('Error getting auction winner:', error);
      return null;
    }
  }
}

module.exports = AuctionEndService;

