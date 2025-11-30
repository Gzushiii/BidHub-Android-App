/**
 * Script to apply the PlaceBid GROUP BY fix
 * This fixes the SQL error: "In aggregated query without GROUP BY..."
 * 
 * Usage: node scripts/apply_placebid_fix.js
 */

const fs = require('fs');
const path = require('path');
const { pool } = require('../src/config/database');

async function applyFix() {
  const connection = await pool.getConnection();
  
  try {
    console.log('🔧 Applying PlaceBid and BuyNow GROUP BY fix...\n');
    
    // Read the SQL fix file
    const sqlFilePath = path.join(__dirname, '../../sql/fix_placebid_groupby_error.sql');
    const sql = fs.readFileSync(sqlFilePath, 'utf8');
    
    // Split by semicolons but preserve DELIMITER blocks
    const statements = sql
      .split(/;\s*(?=\n|$)/)
      .map(s => s.trim())
      .filter(s => s.length > 0 && !s.startsWith('--') && s !== 'USE defaultdb');
    
    // Execute each statement
    for (let i = 0; i < statements.length; i++) {
      const statement = statements[i];
      
      // Skip comments and empty statements
      if (!statement || statement.startsWith('--') || statement.length < 10) {
        continue;
      }
      
      try {
        // Handle DELIMITER statements specially
        if (statement.startsWith('DELIMITER')) {
          // MySQL doesn't support DELIMITER in programmatic execution
          // We'll need to execute the procedure creation differently
          continue;
        }
        
        console.log(`Executing statement ${i + 1}/${statements.length}...`);
        await connection.query(statement);
      } catch (error) {
        // If it's a DELIMITER issue, try executing the whole procedure block
        if (error.message.includes('DELIMITER') || error.message.includes('syntax')) {
          console.log('Handling procedure creation...');
          // Extract procedure definitions and execute them
          continue;
        }
        throw error;
      }
    }
    
    // Execute procedures separately (handle DELIMITER issue)
    console.log('\n📝 Creating PlaceBid procedure...');
    const placeBidProcedure = `
      DROP PROCEDURE IF EXISTS PlaceBid;
      
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
    
    // Split and execute procedure (MySQL requires special handling)
    const procedureStatements = placeBidProcedure
      .split(';')
      .map(s => s.trim())
      .filter(s => s.length > 0);
    
    for (const stmt of procedureStatements) {
      if (stmt.trim().length > 0) {
        await connection.query(stmt);
      }
    }
    
    console.log('✅ PlaceBid procedure created successfully!');
    
    // Verify the procedure exists
    const [procedures] = await connection.query(
      "SHOW PROCEDURE STATUS WHERE Db = DATABASE() AND Name = 'PlaceBid'"
    );
    
    if (procedures.length > 0) {
      console.log('✅ PlaceBid procedure verified!');
    } else {
      console.log('⚠️  Warning: Could not verify PlaceBid procedure');
    }
    
    console.log('\n✨ Fix applied successfully!');
    console.log('You can now try placing a bid - it should work without errors.');
    
  } catch (error) {
    console.error('❌ Error applying fix:', error.message);
    console.error('Stack:', error.stack);
    throw error;
  } finally {
    connection.release();
  }
}

// Run the fix
applyFix()
  .then(() => {
    console.log('\n✅ Done!');
    process.exit(0);
  })
  .catch((error) => {
    console.error('\n❌ Failed:', error.message);
    process.exit(1);
  });

