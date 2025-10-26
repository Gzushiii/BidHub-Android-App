/**
 * Flexible Item Resolver
 * Handles multiple ID formats and provides comprehensive item lookup
 */

/**
 * Resolve item by flexible ID (UUID, numeric, or canonical)
 * @param {Object} connection - Database connection
 * @param {string|number} idOrUuid - Item ID in any format
 * @returns {Object|null} Item record or null if not found
 */
async function resolveItemByFlexibleId(connection, idOrUuid) {
  const correlationId = `resolve_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  
  console.log('Resolving item with flexible ID:', {
    correlationId,
    idOrUuid,
    idType: typeof idOrUuid,
    isUuid: isUuidFormat(idOrUuid),
    isNumeric: isNumericFormat(idOrUuid)
  });

  let row = null;
  let queryUsed = null;
  let paramsUsed = null;

  try {
    if (isUuidFormat(idOrUuid)) {
      // Try UUID-based lookup first
      queryUsed = `SELECT id, uuid_id, title, status, seller_id, current_bid, buy_now_price, created_at, updated_at FROM items WHERE uuid_id = ? LIMIT 1`;
      paramsUsed = [idOrUuid];
      
      const [rows] = await connection.query(queryUsed, paramsUsed);
      row = rows[0] || null;
      
      console.log('UUID lookup result:', {
        correlationId,
        queryUsed,
        paramsUsed,
        found: !!row,
        row: row ? { id: row.id, uuid_id: row.uuid_id } : null
      });
      
    } else if (isNumericFormat(idOrUuid)) {
      // Try numeric ID lookup
      queryUsed = `SELECT id, uuid_id, title, status, seller_id, current_bid, buy_now_price, created_at, updated_at FROM items WHERE id = ? LIMIT 1`;
      paramsUsed = [Number(idOrUuid)];
      
      const [rows] = await connection.query(queryUsed, paramsUsed);
      row = rows[0] || null;
      
      console.log('Numeric lookup result:', {
        correlationId,
        queryUsed,
        paramsUsed,
        found: !!row,
        row: row ? { id: row.id, uuid_id: row.uuid_id } : null
      });
      
    } else {
      // Last resort: try title lookup (for debugging)
      queryUsed = `SELECT id, uuid_id, title, status, seller_id, current_bid, buy_now_price, created_at, updated_at FROM items WHERE title LIKE ? LIMIT 1`;
      paramsUsed = [`%${idOrUuid}%`];
      
      const [rows] = await connection.query(queryUsed, paramsUsed);
      row = rows[0] || null;
      
      console.log('Title lookup result:', {
        correlationId,
        queryUsed,
        paramsUsed,
        found: !!row,
        row: row ? { id: row.id, uuid_id: row.uuid_id } : null
      });
    }

    if (!row) {
      console.log('Item not found with any lookup method:', {
        correlationId,
        idOrUuid,
        queryUsed,
        paramsUsed,
        allAttempts: {
          uuid: isUuidFormat(idOrUuid),
          numeric: isNumericFormat(idOrUuid),
          title: true
        }
      });
    }

    return row;

  } catch (error) {
    console.error('Error in resolveItemByFlexibleId:', {
      correlationId,
      idOrUuid,
      queryUsed,
      paramsUsed,
      error: error.message,
      stack: error.stack
    });
    throw error;
  }
}

/**
 * Check if string is UUID format
 * @param {string} str - String to check
 * @returns {boolean} True if UUID format
 */
function isUuidFormat(str) {
  if (typeof str !== 'string') return false;
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(str);
}

/**
 * Check if string is numeric format
 * @param {string|number} str - String or number to check
 * @returns {boolean} True if numeric format
 */
function isNumericFormat(str) {
  if (typeof str === 'number') return true;
  if (typeof str !== 'string') return false;
  return /^\d+$/.test(str);
}

/**
 * Get item with comprehensive error information
 * @param {Object} connection - Database connection
 * @param {string|number} idOrUuid - Item ID in any format
 * @returns {Object} Result with item, found status, and error details
 */
async function getItemWithErrorInfo(connection, idOrUuid) {
  const correlationId = `getItem_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  
  try {
    const item = await resolveItemByFlexibleId(connection, idOrUuid);
    
    if (item) {
      return {
        found: true,
        item: item,
        correlationId,
        error: null
      };
    } else {
      return {
        found: false,
        item: null,
        correlationId,
        error: {
          type: 'NOT_FOUND',
          message: 'Item not found with any lookup method',
          requestedId: idOrUuid,
          lookupMethods: {
            uuid: isUuidFormat(idOrUuid),
            numeric: isNumericFormat(idOrUuid),
            title: true
          }
        }
      };
    }
  } catch (error) {
    return {
      found: false,
      item: null,
      correlationId,
      error: {
        type: 'DATABASE_ERROR',
        message: error.message,
        requestedId: idOrUuid,
        stack: error.stack
      }
    };
  }
}

module.exports = {
  resolveItemByFlexibleId,
  getItemWithErrorInfo,
  isUuidFormat,
  isNumericFormat
};
