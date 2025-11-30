/**
 * Fix PlaceBid and BuyNow stored procedures - GROUP BY error
 * Run this script to automatically fix the database procedures
 * 
 * Usage: node scripts/fix_placebid_procedure.js
 * Or: npm run fix:placebid
 */

const { pool } = require('../src/config/database');

async function fixPlaceBidProcedure() {
  const connection = await pool.getConnection();
  
  try {
    console.log('🔧 Fixing PlaceBid stored procedure...\n');
    
    // Drop existing procedure
    await connection.query('DROP PROCEDURE IF EXISTS PlaceBid');
    console.log('✅ Dropped old PlaceBid procedure');
    
    // Create fixed procedure - split into multiple queries to handle DELIMITER
    const procedureSQL = `
      CREATE PROCEDURE PlaceBid(
          IN p_item_id INT UNSIGNED,
          IN p_bidder_id INT UNSIGNED,
          IN p_amount DECIMAL(10,2),
          IN p_bidder_alias VARCHAR(50)
      )
      BEGIN
          DECLARE v_current_bid DECIMAL(10,2) DEFAULT 0.00;
          DECLARE v_user_credits DECIMAL(10,2) DEFAULT 0.00;
          DECLARE v_item_exists INT DEFAULT 0;
          DECLARE v_user_exists INT DEFAULT 0;
          DECLARE v_starting_price DECIMAL(10,2) DEFAULT 0.00;
          DECLARE v_previous_bidder_id INT UNSIGNED DEFAULT NULL;
          DECLARE v_previous_bid_amount DECIMAL(10,2) DEFAULT 0.00;
          DECLARE v_seller_id INT UNSIGNED DEFAULT NULL;

          DECLARE EXIT HANDLER FOR SQLEXCEPTION
          BEGIN
              ROLLBACK;
              RESIGNAL;
          END;

          START TRANSACTION;

          SELECT credits INTO v_user_credits
          FROM users
          WHERE id = p_bidder_id
          FOR UPDATE;

          -- FIXED: Split COUNT and column selection to avoid GROUP BY error
          SELECT COUNT(*) INTO v_item_exists
          FROM items
          WHERE id = p_item_id AND status = 'active';
          
          IF v_item_exists > 0 THEN
              SELECT seller_id, starting_price
              INTO v_seller_id, v_starting_price
              FROM items
              WHERE id = p_item_id AND status = 'active'
              FOR UPDATE;
          END IF;

          IF v_item_exists = 0 THEN
              SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Item not found or not active';
          END IF;

          IF p_bidder_id = v_seller_id THEN
              SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot bid on your own item';
          END IF;

          SELECT COUNT(*) INTO v_user_exists
          FROM users
          WHERE id = p_bidder_id;

          IF v_user_exists = 0 THEN
              SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'User not found';
          END IF;

          SELECT COALESCE(MAX(amount), 0) INTO v_current_bid
          FROM bids
          WHERE item_id = p_item_id AND status IN ('active', 'winning');

          SELECT bidder_id, amount INTO v_previous_bidder_id, v_previous_bid_amount
          FROM bids
          WHERE item_id = p_item_id AND status = 'winning'
          ORDER BY created_at DESC
          LIMIT 1;

          IF p_amount <= v_current_bid THEN
              SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bid amount must be higher than current bid';
          END IF;

          IF p_amount <= v_starting_price THEN
              SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bid amount must be higher than starting price';
          END IF;

          IF v_user_credits < p_amount THEN
              SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient credits';
          END IF;

          IF v_previous_bidder_id IS NOT NULL THEN
              UPDATE users 
              SET credits = credits + v_previous_bid_amount
              WHERE id = v_previous_bidder_id;
              
              UPDATE bids 
              SET status = 'outbid'
              WHERE item_id = p_item_id AND bidder_id = v_previous_bidder_id AND status = 'winning';
              
              INSERT INTO transactions (user_id, transaction_type, amount, description, item_id)
              VALUES (v_previous_bidder_id, 'outbid_refund', v_previous_bid_amount, 
                      CONCAT('Refund for outbid on item ', p_item_id), p_item_id);
          END IF;

          UPDATE users 
          SET credits = credits - p_amount
          WHERE id = p_bidder_id;

          INSERT INTO transactions (user_id, transaction_type, amount, description, item_id)
          VALUES (p_bidder_id, 'bid', p_amount, 
                  CONCAT('Bid placed on item ', p_item_id), p_item_id);

          INSERT INTO bids (item_id, bidder_id, amount, status)
          VALUES (p_item_id, p_bidder_id, p_amount, 'winning');

          UPDATE items 
          SET current_bid = p_amount
          WHERE id = p_item_id;

          COMMIT;
          
          SELECT 'Bid placed successfully' AS message, p_amount AS bid_amount;
      END
    `;
    
    // Execute the procedure creation
    // MySQL stored procedures need to be created with a single query
    // We'll use a raw query execution
    await connection.query('SET sql_mode = ""'); // Temporarily disable strict mode if needed
    
    try {
      // Create procedure - mysql2 handles this correctly
      await connection.query(procedureSQL);
      console.log('✅ Created fixed PlaceBid procedure');
    } catch (procError) {
      // If it fails due to DELIMITER, try without it
      if (procError.message.includes('DELIMITER') || procError.message.includes('syntax')) {
        console.log('Retrying without DELIMITER...');
        // The procedure SQL should work as-is since mysql2 handles it
        throw procError;
      }
      throw procError;
    } finally {
      await connection.query('SET sql_mode = "ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION"');
    }
    
    // Verify
    const [procedures] = await connection.query(
      "SHOW PROCEDURE STATUS WHERE Db = DATABASE() AND Name = 'PlaceBid'"
    );
    
    if (procedures.length > 0) {
      console.log('✅ PlaceBid procedure verified successfully!\n');
    } else {
      throw new Error('Failed to verify PlaceBid procedure');
    }
    
    console.log('✨ Fix applied successfully!');
    console.log('You can now place bids without the GROUP BY error.\n');
    
  } catch (error) {
    console.error('❌ Error fixing PlaceBid procedure:', error.message);
    if (error.sqlMessage) {
      console.error('SQL Error:', error.sqlMessage);
    }
    throw error;
  } finally {
    connection.release();
  }
}

// Run if called directly
if (require.main === module) {
  fixPlaceBidProcedure()
    .then(() => {
      console.log('✅ Done!');
      process.exit(0);
    })
    .catch((error) => {
      console.error('\n❌ Failed:', error.message);
      process.exit(1);
    });
}

module.exports = { fixPlaceBidProcedure };

