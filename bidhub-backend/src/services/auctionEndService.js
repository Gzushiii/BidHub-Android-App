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
      // Use SELECT ... FOR UPDATE SKIP LOCKED to prevent duplicate processing
      // Note: SKIP LOCKED requires MySQL 8.0.1+ or MariaDB 10.6+
      // If your database doesn't support it, remove "SKIP LOCKED" (may cause slight delays)
      const [endedAuctions] = await connection.query(
        `SELECT id, title, seller_id, seller_email, end_date, current_price, starting_price
         FROM items 
         WHERE status = 'active' 
         AND end_date <= NOW()
         FOR UPDATE SKIP LOCKED`
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
      // Find the highest bidder with proper tie-breaking
      // Tie-breaking: If amounts are equal, earliest bid wins
      const [highestBids] = await connection.query(
        `SELECT b.id, b.bidder_id, b.bidder_email, b.amount, b.placed_at,
                u.id as user_id, u.email, u.alias
         FROM bids b
         JOIN users u ON b.bidder_id = u.id
         WHERE b.item_id = ?
         AND b.status IN ('active', 'winning')
         ORDER BY b.amount DESC, b.placed_at ASC, b.id ASC
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
        
        // Notify seller that auction ended with no bids
        await this.notifySellerNoBids(connection, item);
        return;
      }
      
      const winningBid = highestBids[0];
      const winningAmount = Number(winningBid.amount);
      
      // CRITICAL: Transfer credits to seller
      // The bidder's credits were already deducted when they placed the bid
      // Now we transfer those credits to the seller
      if (item.seller_id && winningAmount > 0) {
        // Lock seller's row
        await connection.query(
          `SELECT id FROM users WHERE id = ? FOR UPDATE`,
          [item.seller_id]
        );
        
        // Transfer credits to seller
        await connection.query(
          `UPDATE users 
           SET credits = credits + ?,
               balance_version = COALESCE(balance_version, 0) + 1
           WHERE id = ?`,
          [winningAmount, item.seller_id]
        );
        
        // Record credit transaction for seller
        await connection.query(
          `INSERT INTO credit_transactions 
           (user_id, type, amount, status, reference, transaction_date, idempotency_key)
           VALUES (?, 'auction_sale', ?, 'completed', ?, NOW(), ?)
           ON DUPLICATE KEY UPDATE status = 'completed'`,
          [
            item.seller_id,
            winningAmount,
            `AUCTION_SALE_ITEM_${item.id}`,
            `SALE_${item.id}_${item.seller_id}_${Date.now()}`
          ]
        );
        
        console.log(`Transferred ₱${winningAmount} to seller ${item.seller_id} for item ${item.id}`);
      }
      
      // Update item status to ended and set winner
      await connection.query(
        `UPDATE items 
         SET status = 'ended', 
             winner_id = ?, 
             winner_email = ?,
             current_price = ?
         WHERE id = ?`,
        [winningBid.bidder_id, winningBid.bidder_email, winningAmount, item.id]
      );
      
      // Update winning bid status
      await connection.query(
        `UPDATE bids SET status = 'WINNING' WHERE id = ?`,
        [winningBid.id]
      );
      
      // Mark all other bids as lost (including outbid ones)
      await connection.query(
        `UPDATE bids 
         SET status = 'LOST' 
         WHERE item_id = ? 
         AND id != ? 
         AND status IN ('active', 'winning', 'outbid')`,
        [item.id, winningBid.id]
      );
      
      // Send notification to winner
      await this.notifyAuctionWinner(connection, item, winningBid);
      
      // Send notifications to other bidders (they lost)
      await this.notifyAuctionLosers(connection, item, winningBid.bidder_id);
      
      // Notify seller of successful sale
      await this.notifySellerAuctionEnded(connection, item, winningBid);
      
      console.log(`Auction ${item.id} ended. Winner: ${winningBid.bidder_email} (₱${winningAmount})`);
      
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
      const [winners] = await pool.query(
        `SELECT i.winner_id, i.winner_email, i.current_price as winning_bid, u.alias, u.email
         FROM items i
         LEFT JOIN users u ON i.winner_id = u.id
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

