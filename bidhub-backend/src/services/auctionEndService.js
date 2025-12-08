const { pool } = require('../config/database');
const BidHubNotificationManager = require('./notificationService');

/**
 * Service to detect ended auctions and notify winners
 * This service should be run periodically (e.g., via cron job or scheduled task)
 */
class AuctionEndService {
  /**
   * Check for ended auctions and process winners
   * Uses individual transactions per auction to prevent one failure from blocking others
   */
  static async processEndedAuctions() {
    const connection = await pool.getConnection();
    
    try {
      // Find all active auctions that have ended
      // Note: Removed SKIP LOCKED for compatibility with older MySQL versions
      // This may cause slight delays if multiple processes run simultaneously, but ensures compatibility
      const [endedAuctions] = await connection.query(
        `SELECT id, title, seller_id, seller_email, end_date, current_price, starting_price
         FROM items 
         WHERE status = 'active' 
         AND end_date <= NOW()
         FOR UPDATE`
      );
      
      console.log(`Found ${endedAuctions.length} ended auctions to process`);
      
      let processedCount = 0;
      let failedCount = 0;
      
      // Process each auction in its own transaction to prevent cascading failures
      for (const item of endedAuctions) {
        const itemConnection = await pool.getConnection();
        try {
          await itemConnection.beginTransaction();
          
          // Re-check status with lock to prevent race conditions
          const [lockedItems] = await itemConnection.query(
            `SELECT id, status, seller_id, seller_email, title, end_date
             FROM items 
             WHERE id = ? 
             AND status = 'active'
             FOR UPDATE`,
            [item.id]
          );
          
          if (lockedItems.length === 0) {
            // Item was already processed or status changed
            await itemConnection.rollback();
            console.log(`Auction ${item.id} already processed or status changed, skipping`);
            continue;
          }
          
          await this.processAuctionEnd(itemConnection, lockedItems[0]);
          await itemConnection.commit();
          processedCount++;
          
        } catch (error) {
          await itemConnection.rollback();
          console.error(`Error processing auction ${item.id}:`, error);
          failedCount++;
          // Continue processing other auctions even if one fails
        } finally {
          itemConnection.release();
        }
      }
      
      console.log(`Successfully processed ${processedCount} ended auctions. ${failedCount} failed.`);
      
    } catch (error) {
      console.error('Error finding ended auctions:', error);
      throw error;
    } finally {
      connection.release();
    }
  }
  
  /**
   * Process a single ended auction
   * Handles winner determination, credit transfer to seller, and notifications
   */
  static async processAuctionEnd(connection, item) {
    try {
      // Use the EndAuction stored procedure to handle credit transfer
      // This ensures consistency and prevents duplicate transfers
      let resultData = {};
      let winnerId = null;
      let winningAmount = 0;
      let sellerNewBalance = 0;
      
      try {
        const [procedureResult] = await connection.query(
          'CALL EndAuction(?)',
          [item.id]
        );
        
        resultData = procedureResult[0]?.[0] || {};
        winnerId = resultData.winner_id;
        winningAmount = resultData.winning_amount || 0;
        sellerNewBalance = resultData.seller_new_balance || 0;
      } catch (procError) {
        // If stored procedure doesn't exist, determine winner manually
        console.warn('EndAuction stored procedure not available, determining winner manually:', procError.message);
        
        // Find the highest bid manually
        const [highestBids] = await connection.query(
          `SELECT bidder_id, amount 
           FROM bids 
           WHERE item_id = ? AND status IN ('active', 'winning')
           ORDER BY amount DESC, created_at DESC 
           LIMIT 1`,
          [item.id]
        );
        
        if (highestBids.length > 0) {
          winnerId = highestBids[0].bidder_id;
          winningAmount = highestBids[0].amount;
          
          // Update item status
          await connection.query(
            'UPDATE items SET status = ? WHERE id = ?',
            ['ended', item.id]
          );
        }
      }
      
      if (winnerId) {
        // Get winner details for notifications
        const [winningBids] = await connection.query(
          `SELECT b.id, b.bidder_id, b.bidder_email, b.amount, b.placed_at,
                  u.id as user_id, u.email, u.alias
           FROM bids b
           JOIN users u ON b.bidder_id = u.id
           WHERE b.item_id = ? AND b.bidder_id = ? AND b.status = 'won'
           LIMIT 1`,
          [item.id, winnerId]
        );
        
        if (winningBids.length > 0) {
          const winningBid = winningBids[0];
          
          // Send notification to winner
          await this.notifyAuctionWinner(connection, item, winningBid);
          
          // Send notifications to other bidders (they lost)
          await this.notifyAuctionLosers(connection, item, winnerId);
          
          // Notify seller of successful sale
          await this.notifySellerAuctionEnded(connection, item, winningBid);
          
          console.log(`Auction ${item.id} ended. Winner: ${winningBid.email} (₱${winningAmount}). Seller balance: ₱${sellerNewBalance}`);
        }
      } else {
        // No bids - notify seller
        await this.notifySellerNoBids(connection, item);
        console.log(`Auction ${item.id} ended with no bids`);
      }
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
      
      // Store notification in database for the winner (if table exists)
      try {
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
      } catch (notificationError) {
        // If notifications table doesn't exist, just log and continue
        console.warn('Notifications table not available, skipping database notification:', notificationError.message);
      }
      
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
        
        // Store notification (if table exists)
        try {
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
        } catch (notificationError) {
          console.warn('Notifications table not available, skipping database notification:', notificationError.message);
        }
        
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
   * Notify seller that auction ended with no bids
   */
  static async notifySellerNoBids(connection, item) {
    try {
      if (!item.seller_id || !item.seller_email) {
        return;
      }

      const notificationData = {
        type: 'auction_ended_no_bids',
        item_id: item.id,
        item_title: item.title
      };

      try {
        await connection.query(
          `INSERT INTO notifications 
           (user_id, user_email, type, title, message, data, created_at) 
           VALUES (?, ?, ?, ?, ?, ?, NOW())`,
          [
            item.seller_id,
            item.seller_email,
            'auction_ended_no_bids',
            'Auction Ended - No Bids',
            `Your auction for "${item.title}" ended with no bids.`,
            JSON.stringify(notificationData)
          ]
        );
      } catch (notificationError) {
        console.warn('Notifications table not available, skipping database notification:', notificationError.message);
      }

      if (BidHubNotificationManager) {
        await BidHubNotificationManager.sendAuctionEndedNoBidsNotification(
          item.seller_id,
          item.seller_email,
          item.title
        );
      }
    } catch (error) {
      console.error('Error notifying seller (no bids):', error);
      // Don't throw - notification failure shouldn't block auction processing
    }
  }

  /**
   * Notify seller that auction ended with a winner
   */
  static async notifySellerAuctionEnded(connection, item, winningBid) {
    try {
      if (!item.seller_id || !item.seller_email) {
        return;
      }

      const notificationData = {
        type: 'auction_ended_winner',
        item_id: item.id,
        item_title: item.title,
        winning_amount: winningBid.amount,
        winner_email: winningBid.bidder_email,
        winner_alias: winningBid.alias
      };

      try {
        await connection.query(
          `INSERT INTO notifications 
           (user_id, user_email, type, title, message, data, created_at) 
           VALUES (?, ?, ?, ?, ?, ?, NOW())`,
          [
            item.seller_id,
            item.seller_email,
            'auction_ended_winner',
            'Auction Ended - Item Sold',
            `Your auction for "${item.title}" ended. Winner: ${winningBid.alias || winningBid.bidder_email} (₱${winningBid.amount.toFixed(2)})`,
            JSON.stringify(notificationData)
          ]
        );
      } catch (notificationError) {
        console.warn('Notifications table not available, skipping database notification:', notificationError.message);
      }

      if (BidHubNotificationManager) {
        await BidHubNotificationManager.sendAuctionEndedWinnerNotification(
          item.seller_id,
          item.seller_email,
          item.title,
          winningBid.amount,
          winningBid.alias || winningBid.bidder_email
        );
      }
    } catch (error) {
      console.error('Error notifying seller (winner):', error);
      // Don't throw - notification failure shouldn't block auction processing
    }
  }

  /**
   * Get auction winner details for an item
   */
  static async getAuctionWinner(itemId) {
    try {
      // Handle both winner_id/winner_email columns and fallback to current_bidder_id
      const [winners] = await pool.query(
        `SELECT 
           COALESCE(i.winner_id, i.current_bidder_id) as winner_id,
           COALESCE(i.winner_email, u.email) as winner_email,
           COALESCE(i.current_price, i.current_bid, i.starting_price, i.starting_bid, 0) as winning_bid,
           u.alias, u.email
         FROM items i
         LEFT JOIN users u ON COALESCE(i.winner_id, i.current_bidder_id) = u.id
         WHERE i.id = ? AND i.status = 'ended'
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

