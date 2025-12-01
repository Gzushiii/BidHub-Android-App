/**
 * Credit Transfer Validation Utilities
 * Provides validation and safeguards for credit transfers
 */

const { pool } = require('../config/database');

/**
 * Validate that a credit transfer can be safely executed
 * @param {number} userId - User ID
 * @param {number} amount - Amount to transfer
 * @param {string} transactionType - Type of transaction (bid, buy_now, etc.)
 * @param {number} itemId - Item ID (if applicable)
 * @param {object} connection - Database connection (optional)
 * @returns {Promise<{valid: boolean, error?: string, currentBalance?: number}>}
 */
async function validateCreditTransfer(userId, amount, transactionType, itemId = null, connection = null) {
  const db = connection || pool;
  
  try {
    // Validate amount
    if (!amount || amount <= 0 || !Number.isFinite(amount)) {
      return { valid: false, error: 'Invalid amount' };
    }

    // Get user's current balance with lock
    // Handle case where balance_version column might not exist
    const [users] = await db.query(
      'SELECT credits, COALESCE(balance_version, 0) as balance_version FROM users WHERE id = ? FOR UPDATE',
      [userId]
    );

    if (users.length === 0) {
      return { valid: false, error: 'User not found' };
    }

    const currentBalance = Number(users[0].credits || 0);
    const balanceVersion = Number(users[0].balance_version || 0);

    // Check sufficient balance
    if (currentBalance < amount) {
      return {
        valid: false,
        error: 'Insufficient credits',
        currentBalance,
        required: amount,
        shortfall: amount - currentBalance
      };
    }

    // Check for duplicate transaction (if itemId provided)
    if (itemId) {
      const [existingTransactions] = await db.query(
        `SELECT id FROM credit_transactions 
         WHERE user_id = ? AND item_id = ? AND type = ? AND status = 'completed'
         AND created_at > DATE_SUB(NOW(), INTERVAL 5 MINUTE)`,
        [userId, itemId, transactionType]
      );

      if (existingTransactions.length > 0) {
        return {
          valid: false,
          error: 'Duplicate transaction detected',
          existingTransactionId: existingTransactions[0].id
        };
      }
    }

    return {
      valid: true,
      currentBalance,
      balanceVersion
    };

  } catch (error) {
    console.error('Credit validation error:', error);
    return {
      valid: false,
      error: 'Validation error: ' + error.message
    };
  }
}

/**
 * Check if a transaction has already been processed (idempotency check)
 * @param {string} idempotencyKey - Unique idempotency key
 * @param {object} connection - Database connection (optional)
 * @returns {Promise<{exists: boolean, transaction?: object}>}
 */
async function checkIdempotency(idempotencyKey, connection = null) {
  const db = connection || pool;
  
  try {
    const [transactions] = await db.query(
      `SELECT id, user_id, type, amount, status, item_id, created_at
       FROM credit_transactions 
       WHERE idempotency_key = ?`,
      [idempotencyKey]
    );

    if (transactions.length > 0) {
      return {
        exists: true,
        transaction: transactions[0]
      };
    }

    return { exists: false };

  } catch (error) {
    console.error('Idempotency check error:', error);
    return { exists: false, error: error.message };
  }
}

/**
 * Verify balance consistency after a transaction
 * @param {number} userId - User ID
 * @param {number} expectedBalance - Expected balance after transaction
 * @param {object} connection - Database connection (optional)
 * @returns {Promise<{consistent: boolean, actualBalance?: number, difference?: number}>}
 */
async function verifyBalanceConsistency(userId, expectedBalance, connection = null) {
  const db = connection || pool;
  
  try {
    const [users] = await db.query(
      'SELECT credits FROM users WHERE id = ?',
      [userId]
    );

    if (users.length === 0) {
      return { consistent: false, error: 'User not found' };
    }

    const actualBalance = Number(users[0].credits || 0);
    const difference = Math.abs(actualBalance - expectedBalance);

    // Allow small floating point differences (0.01)
    const isConsistent = difference < 0.01;

    return {
      consistent: isConsistent,
      actualBalance,
      expectedBalance,
      difference
    };

  } catch (error) {
    console.error('Balance consistency check error:', error);
    return { consistent: false, error: error.message };
  }
}

/**
 * Get transaction summary for a user
 * @param {number} userId - User ID
 * @param {object} connection - Database connection (optional)
 * @returns {Promise<{totalSpent: number, totalReceived: number, netBalance: number}>}
 */
async function getTransactionSummary(userId, connection = null) {
  const db = connection || pool;
  
  try {
    const [summary] = await db.query(
      `SELECT 
        SUM(CASE WHEN type IN ('bid', 'buy_now') AND status = 'completed' THEN amount ELSE 0 END) as total_spent,
        SUM(CASE WHEN type IN ('bonus', 'outbid_refund', 'purchase') AND status = 'completed' THEN amount ELSE 0 END) as total_received
       FROM credit_transactions 
       WHERE user_id = ?`,
      [userId]
    );

    const totalSpent = Number(summary[0]?.total_spent || 0);
    const totalReceived = Number(summary[0]?.total_received || 0);
    const netBalance = totalReceived - totalSpent;

    return {
      totalSpent,
      totalReceived,
      netBalance
    };

  } catch (error) {
    console.error('Transaction summary error:', error);
    return {
      totalSpent: 0,
      totalReceived: 0,
      netBalance: 0,
      error: error.message
    };
  }
}

module.exports = {
  validateCreditTransfer,
  checkIdempotency,
  verifyBalanceConsistency,
  getTransactionSummary
};

