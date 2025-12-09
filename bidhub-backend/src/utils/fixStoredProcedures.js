/**
 * Automatically fix PlaceBid and BuyNow stored procedures
 * This runs on server startup to ensure procedures are always fixed
 */

const mysql = require('mysql2/promise');

/**
 * Fix PlaceBid and BuyNow stored procedures
 * @param {mysql.Pool} pool - Database connection pool (used to get config)
 */
async function fixStoredProcedures(pool) {
  let connection;
  
  try {
    // Create a direct connection with multiple statements enabled for procedure creation
    // We can't use the pool because it doesn't have multipleStatements enabled
    const poolConfig = pool.config || pool.pool?.config;
    connection = await mysql.createConnection({
      host: process.env.DB_HOST,
      port: Number(process.env.DB_PORT) || 3306,
      user: process.env.DB_USER,
      password: process.env.DB_PASSWORD,
      database: process.env.DB_NAME,
      ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: false } : false,
      multipleStatements: true // Required for procedure creation
    });
    
    console.log('🔧 Checking and fixing stored procedures...');
    
    // Drop existing procedures
    await connection.query('DROP PROCEDURE IF EXISTS PlaceBid');
    await connection.query('DROP PROCEDURE IF EXISTS BuyNow');
    console.log('✅ Dropped old procedures');
    
    // Create fixed PlaceBid procedure
    const createPlaceBid = `
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
    
    await connection.query(createPlaceBid);
    console.log('✅ Created fixed PlaceBid procedure');
    
    // Create fixed BuyNow procedure
    const createBuyNow = `
CREATE PROCEDURE BuyNow(
    IN p_item_id INT UNSIGNED,
    IN p_buyer_id INT UNSIGNED,
    IN p_buyer_alias VARCHAR(50)
)
BEGIN
    DECLARE v_buy_now_price DECIMAL(10,2) DEFAULT 0.00;
    DECLARE v_user_credits DECIMAL(10,2) DEFAULT 0.00;
    DECLARE v_item_exists INT DEFAULT 0;
    DECLARE v_user_exists INT DEFAULT 0;
    DECLARE v_seller_id INT UNSIGNED DEFAULT NULL;
    DECLARE v_seller_credits DECIMAL(10,2) DEFAULT 0.00;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    SELECT credits INTO v_user_credits
    FROM users
    WHERE id = p_buyer_id
    FOR UPDATE;

    SELECT COUNT(*) INTO v_item_exists
    FROM items
    WHERE id = p_item_id AND status = 'active';
    
    IF v_item_exists > 0 THEN
        SELECT seller_id, buy_now_price
        INTO v_seller_id, v_buy_now_price
        FROM items
        WHERE id = p_item_id AND status = 'active'
        FOR UPDATE;
    END IF;

    IF v_item_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Item not found or not active';
    END IF;

    IF v_buy_now_price IS NULL OR v_buy_now_price <= 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Item does not have buy now option';
    END IF;

    IF p_buyer_id = v_seller_id THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot buy your own item';
    END IF;

    SELECT COUNT(*) INTO v_user_exists
    FROM users
    WHERE id = p_buyer_id;

    IF v_user_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'User not found';
    END IF;

    IF v_user_credits < v_buy_now_price THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient credits';
    END IF;

    SELECT credits INTO v_seller_credits
    FROM users
    WHERE id = v_seller_id
    FOR UPDATE;

    UPDATE users 
    SET credits = credits - v_buy_now_price
    WHERE id = p_buyer_id;

    UPDATE users 
    SET credits = credits + v_buy_now_price
    WHERE id = v_seller_id;

    INSERT INTO transactions (user_id, transaction_type, amount, description, item_id)
    VALUES (p_buyer_id, 'purchase', v_buy_now_price, 
            CONCAT('Buy now purchase of item ', p_item_id), p_item_id);

    INSERT INTO transactions (user_id, transaction_type, amount, description, item_id)
    VALUES (v_seller_id, 'sale', v_buy_now_price, 
            CONCAT('Sale of item ', p_item_id), p_item_id);

    UPDATE items 
    SET status = 'sold',
        current_bid = v_buy_now_price,
        current_bidder_id = p_buyer_id
    WHERE id = p_item_id;

    COMMIT;
    
    SELECT 'Item purchased successfully' AS message, v_buy_now_price AS purchase_amount;
END
    `;
    
    await connection.query(createBuyNow);
    console.log('✅ Created fixed BuyNow procedure');
    
    // Verify procedures exist
    const [procedures] = await connection.query(
      "SHOW PROCEDURE STATUS WHERE Db = DATABASE() AND Name IN ('PlaceBid', 'BuyNow')"
    );
    
    if (procedures.length >= 2) {
      console.log('✅ Stored procedures verified successfully');
      return true;
    } else {
      console.warn('⚠️  Warning: Not all procedures were created. Found:', procedures.length);
      return false;
    }
    
  } catch (error) {
    console.error('❌ Error fixing stored procedures:', error.message);
    if (error.sqlMessage) {
      console.error('   SQL Error:', error.sqlMessage);
    }
    // Don't throw - allow server to start even if fix fails
    // The procedures might already be fixed
    return false;
  } finally {
    if (connection) {
      await connection.end();
    }
  }
}

module.exports = { fixStoredProcedures };

