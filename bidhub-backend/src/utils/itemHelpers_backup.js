/**
 * Unified Item Lookup Utilities
 *
 * These helpers provide a consistent way to load items for read and write
 * flows so that bidding/buy-now operations never drift from what the UI shows.
 */

/**
 * Convert a 16-byte Buffer into a UUID v4 string.
 * Returns null when conversion is not possible.
 *
 * @param {Buffer} buffer
 * @returns {string|null}
 */
function bufferToUuid(buffer) {
  if (!Buffer.isBuffer(buffer) || buffer.length !== 16) {
    return null;
  }

  const hex = buffer.toString('hex');

  return [
    hex.substring(0, 8),
    hex.substring(8, 12),
    hex.substring(12, 16),
    hex.substring(16, 20),
    hex.substring(20)
  ].join('-');
}

/**
 * Normalizes any supported date representation to a Date object.
 *
 * @param {Date|string|null} value
 * @returns {Date|null}
 */
function normalizeDate(value) {
  if (!value) {
    return null;
  }

  if (value instanceof Date) {
    return value;
  }

  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

/**
 * Ensure the record has a consistent canonical ID and clean string fields.
 *
 * @param {object} record
 * @param {string} fallbackId
 * @returns {object|null}
 */
function sanitizeItemRecord(record, fallbackId) {
  if (!record) {
    return null;
  }

  const sanitized = { ...record };

  if (Buffer.isBuffer(sanitized.id)) {
    const converted = bufferToUuid(sanitized.id);
    if (converted) {
      sanitized.id = converted;
    }
  }

  if (Buffer.isBuffer(sanitized.uuid_id)) {
    const converted = bufferToUuid(sanitized.uuid_id);
    if (converted) {
      sanitized.uuid_id = converted;
    }
  }

  const uuidCandidate =
    typeof sanitized.uuid_id === 'string' ? sanitized.uuid_id.trim() : '';
  const idCandidate =
    typeof sanitized.id === 'string' ? sanitized.id.trim() : '';

  sanitized.canonical_id = uuidCandidate || idCandidate || fallbackId;

  return sanitized;
}

/**
 * Attempt to locate an item record in the items table using multiple ID formats.
 *
 * Supports VARCHAR(36) IDs, BINARY(16) UUID storage, and transitional schemas
 * that expose uuid_id or legacy integer IDs.
 *
 * @param {object} connection
 * @param {string} normalizedId
 * @returns {Promise<object|null>}
 */
async function queryItemRecord(connection, normalizedId) {
  const attempts = [
    {
      sql: 'SELECT * FROM items WHERE id = ? OR uuid_id = ? LIMIT 1',
      params: [normalizedId, normalizedId],
      toleratedErrors: new Set(['ER_BAD_FIELD_ERROR', 'ER_NO_SUCH_TABLE'])
    },
    {
      sql: 'SELECT * FROM items WHERE id = ? LIMIT 1',
      params: [normalizedId],
      toleratedErrors: new Set(['ER_NO_SUCH_TABLE'])
    },
    {
      sql: 'SELECT * FROM items WHERE uuid_id = ? LIMIT 1',
      params: [normalizedId],
      toleratedErrors: new Set(['ER_BAD_FIELD_ERROR', 'ER_NO_SUCH_TABLE'])
    },
    {
      sql: 'SELECT * FROM items WHERE id = UUID_TO_BIN(?) LIMIT 1',
      params: [normalizedId],
      toleratedErrors: new Set([
        'ER_BAD_FIELD_ERROR',
        'ER_TRUNCATED_WRONG_VALUE',
        'ER_NO_SUCH_TABLE'
      ])
    },
    {
      sql: 'SELECT * FROM items WHERE uuid_id = UUID_TO_BIN(?) LIMIT 1',
      params: [normalizedId],
      toleratedErrors: new Set([
        'ER_BAD_FIELD_ERROR',
        'ER_TRUNCATED_WRONG_VALUE',
        'ER_NO_SUCH_TABLE'
      ])
    }
  ];

  for (const attempt of attempts) {
    try {
      const [rows] = await connection.query(attempt.sql, attempt.params);
      if (rows.length > 0) {
        return sanitizeItemRecord(rows[0], normalizedId);
      }
    } catch (error) {
      if (!attempt.toleratedErrors || !attempt.toleratedErrors.has(error.code)) {
        throw error;
      }
    }
  }

  return null;
}

/**
 * Explain why an item cannot be transacted upon.
 *
 * @param {object} item
 * @returns {string}
 */
function determineInactiveReason(item) {
  if (!item) {
    return 'unknown';
  }

  if (item.deleted_at) {
    return 'deleted';
  }

  if (item.is_draft !== undefined && item.is_draft !== null) {
    const draftFlag =
      typeof item.is_draft === 'boolean'
        ? item.is_draft
        : Number(item.is_draft) === 1;

    if (draftFlag) {
      return 'not_published';
    }
  }

  if (item.state && typeof item.state === 'string') {
    const state = item.state.trim().toLowerCase();
    if (state && state !== 'active') {
      return `state_${state}`;
    }
  }

  if (item.status && typeof item.status === 'string') {
    const status = item.status.trim().toLowerCase();
    if (status && !['active', 'draft'].includes(status)) {
      if (['ended', 'sold', 'closed', 'completed'].includes(status)) {
        return 'auction_ended';
      }
      return `status_${status}`;
    }
  }

  const now = new Date();
  const startTime = normalizeDate(
    item.start_time || item.start_date || item.starts_at
  );

  if (startTime && startTime > now) {
    return 'not_started';
  }

  const endTime = normalizeDate(
    item.end_time || item.end_date || item.ends_at
  );

  if (endTime && endTime <= now) {
    return 'auction_ended';
  }

  return 'inactive';
}

/**
 * Fetch an item with detailed error information that can be returned to clients.
 *
 * @param {object} connection
 * @param {string} itemId
 * @returns {Promise<{item: object|null, error: object|null}>}
 */
async function fetchItemWithErrorInfo(connection, itemId) {
  const normalizedId = String(itemId ?? '').trim();

  if (!normalizedId) {
    return {
      item: null,
      error: {
        code: 'INVALID_ID',
        message: 'Item ID is required',
        http_status: 400,
        json: {
          error: 'invalid_request',
          details: 'missing_item_id',
          message: 'Item ID is required'
        }
      }
    };
  }

  let viewItem = null;

  try {
    const [viewRows] = await connection.query(
      'SELECT * FROM v_active_items WHERE uuid_id = ? LIMIT 1',
      [normalizedId]
    );

    if (viewRows.length > 0) {
      viewItem = sanitizeItemRecord(viewRows[0], normalizedId);
    }
  } catch (error) {
    console.warn(
      'fetchItemWithErrorInfo: v_active_items lookup failed',
      error.code
    );
  }

  const rawItem = await queryItemRecord(connection, normalizedId);

  if (viewItem && rawItem) {
    return {
      item: {
        ...rawItem,
        canonical_id: rawItem.canonical_id,
        view_record: viewItem
      },
      error: null
    };
  }

  if (viewItem && !rawItem) {
    // Rare case: item present in view but not resolvable directly.
    return { item: viewItem, error: null };
  }

  if (!rawItem) {
    return {
      item: null,
      error: {
        code: 'NOT_FOUND',
        message: 'Item not found',
        http_status: 404,
        json: {
          error: 'item_not_found',
          details: 'item_does_not_exist',
          message: 'Item not found',
          item_id: normalizedId
        }
      }
    };
  }

  const reason = determineInactiveReason(rawItem);

  return {
    item: null,
    error: {
      code: 'NOT_ACTIVE',
      message: `Item is not available for transactions (${reason})`,
      http_status: 400,
      json: {
        error: reason === 'auction_ended' ? 'auction_ended' : 'not_active',
        details: reason,
        message: 'Item is not available for transactions',
        item_id: normalizedId,
        ...(rawItem.status && { item_status: rawItem.status }),
        ...(rawItem.state && { item_state: rawItem.state }),
        ...(rawItem.is_draft !== undefined && {
          item_is_draft: rawItem.is_draft
        }),
        ...(rawItem.deleted_at && { item_deleted_at: rawItem.deleted_at })
      }
    }
  };
}

/**
 * Fetch the raw item record without applying availability rules.
 *
 * @param {object} connection
 * @param {string} itemId
 * @returns {Promise<object|null>}
 */
async function fetchItemRecord(connection, itemId) {
  const normalizedId = String(itemId ?? '').trim();
  if (!normalizedId) {
    return null;
  }
  return queryItemRecord(connection, normalizedId);
}

/**
 * Fetch an active item that's available for transactions (bid/buy-now).
 *
 * @param {object} connection
 * @param {string} itemId
 * @returns {Promise<object|null>}
 */
async function fetchActiveItem(connection, itemId) {
  const result = await fetchItemWithErrorInfo(connection, itemId);
  return result.item;
}

/**
 * Validate that an item is available for bidding.
 *
 * @param {object} item
 * @param {number|string} bidderId
 * @returns {{valid: boolean, error: object|null}}
 */
function validateItemForBidding(item, bidderId) {
  if (!item) {
    return {
      valid: false,
      error: {
        http_status: 404,
        json: {
          error: 'item_not_found',
          message: 'Item not found'
        }
      }
    };
  }

  if (item.seller_id !== undefined && item.seller_id === bidderId) {
    return {
      valid: false,
      error: {
        http_status: 400,
        json: {
          error: 'conflict',
          details: 'buyer_is_owner',
          message: 'Cannot bid on your own item'
        }
      }
    };
  }

  const endDate = normalizeDate(item.end_time || item.end_date || item.ends_at);
  if (endDate && endDate <= new Date()) {
    return {
      valid: false,
      error: {
        http_status: 400,
        json: {
          error: 'auction_ended',
          details: 'auction_ended',
          message: 'Auction has ended',
          end_date: endDate
        }
      }
    };
  }

  if (item.buy_now_only === 1 || item.buy_now_only === true) {
    return {
      valid: false,
      error: {
        http_status: 400,
        json: {
          error: 'not_available',
          details: 'buy_now_only',
          message: 'This item is only available for Buy Now, not for bidding'
        }
      }
    };
  }

  return { valid: true, error: null };
}

/**
 * Validate that an item is available for Buy Now.
 *
 * @param {object} item
 * @param {number|string} buyerId
 * @returns {{valid: boolean, error: object|null}}
 */
function validateItemForBuyNow(item, buyerId) {
  if (!item) {
    return {
      valid: false,
      error: {
        http_status: 404,
        json: {
          error: 'item_not_found',
          message: 'Item not found'
        }
      }
    };
  }

  if (item.seller_id !== undefined && item.seller_id === buyerId) {
    return {
      valid: false,
      error: {
        http_status: 400,
        json: {
          error: 'conflict',
          details: 'buyer_is_owner',
          message: 'Cannot buy your own item'
        }
      }
    };
  }

  const buyNowPrice = Number(
    item.buy_now_price ?? item.buy_now_amount ?? item.buy_now
  );

  if (!Number.isFinite(buyNowPrice) || buyNowPrice <= 0) {
    return {
      valid: false,
      error: {
        http_status: 400,
        json: {
          error: 'not_available',
          details: 'no_buy_now_price',
          message: 'Item does not have a Buy Now price'
        }
      }
    };
  }

  const endDate = normalizeDate(item.end_time || item.end_date || item.ends_at);
  if (endDate && endDate <= new Date()) {
    return {
      valid: false,
      error: {
        http_status: 400,
        json: {
          error: 'sale_ended',
          details: 'sale_ended',
          message: 'Sale period has ended',
          end_date: endDate
        }
      }
    };
  }

  return { valid: true, error: null };
}

module.exports = {
  fetchActiveItem,
  fetchItemRecord,
  fetchItemWithErrorInfo,
  validateItemForBidding,
  validateItemForBuyNow
};
