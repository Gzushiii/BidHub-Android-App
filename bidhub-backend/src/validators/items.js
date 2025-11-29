const Joi = require('joi');

// Item creation validation schema
const createItemSchema = Joi.object({
  title: Joi.string()
    .min(3)
    .max(255)
    .required()
    .messages({
      'string.min': 'Title must be at least 3 characters long',
      'string.max': 'Title cannot exceed 255 characters',
      'any.required': 'Title is required'
    }),
  
  description: Joi.string()
    .optional()
    .allow('', null)
    .custom((value, helpers) => {
      // If value is empty or null, allow it (bypass length validation)
      if (!value || value.trim() === '') {
        return value;
      }
      // If value exists, validate length
      if (value.length < 10) {
        return helpers.error('string.min');
      }
      if (value.length > 2000) {
        return helpers.error('string.max');
      }
      return value;
    })
    .messages({
      'string.min': 'Description must be at least 10 characters long if provided',
      'string.max': 'Description cannot exceed 2000 characters'
    }),
  
  category_id: Joi.number()
    .integer()
    .positive()
    .required()
    .messages({
      'number.base': 'Category ID must be a number',
      'number.integer': 'Category ID must be an integer',
      'number.positive': 'Category ID must be positive',
      'any.required': 'Category ID is required'
    }),
  
  starting_price: Joi.number()
    .precision(2)
    .min(0.01)
    .max(999999.99)
    .required()
    .messages({
      'number.base': 'Starting price must be a number',
      'number.min': 'Starting price must be at least $0.01',
      'number.max': 'Starting price cannot exceed $999,999.99',
      'any.required': 'Starting price is required'
    }),
  
  reserve_price: Joi.number()
    .precision(2)
    .min(0.01)
    .max(999999.99)
    .optional()
    .messages({
      'number.base': 'Reserve price must be a number',
      'number.min': 'Reserve price must be at least $0.01',
      'number.max': 'Reserve price cannot exceed $999,999.99'
    }),
  
  duration_days: Joi.number()
    .integer()
    .min(1)
    .max(30)
    .required()
    .messages({
      'number.base': 'Duration must be a number',
      'number.integer': 'Duration must be an integer',
      'number.min': 'Duration must be at least 1 day',
      'number.max': 'Duration cannot exceed 30 days',
      'any.required': 'Duration is required'
    }),
  
  status: Joi.string()
    .valid('active', 'draft')
    .optional()
    .default('active')
    .messages({
      'any.only': 'Status must be either active or draft'
    }),
  
  images: Joi.array()
    .items(
      Joi.string()
        .uri()
        .max(500) // URLs should be reasonable length
        .messages({
          'string.uri': 'Image must be a valid URL',
          'string.max': 'Image URL cannot exceed 500 characters'
        })
    )
    .max(10)
    .optional()
    .messages({
      'array.max': 'Cannot have more than 10 images'
    })
});

// Item update validation schema (all fields optional)
const updateItemSchema = Joi.object({
  title: Joi.string()
    .min(3)
    .max(255)
    .optional()
    .messages({
      'string.min': 'Title must be at least 3 characters long',
      'string.max': 'Title cannot exceed 255 characters'
    }),
  
  description: Joi.string()
    .optional()
    .allow('', null)
    .custom((value, helpers) => {
      // If value is empty or null, allow it (bypass length validation)
      if (!value || value.trim() === '') {
        return value;
      }
      // If value exists, validate length
      if (value.length < 10) {
        return helpers.error('string.min');
      }
      if (value.length > 2000) {
        return helpers.error('string.max');
      }
      return value;
    })
    .messages({
      'string.min': 'Description must be at least 10 characters long if provided',
      'string.max': 'Description cannot exceed 2000 characters'
    }),
  
  category_id: Joi.number()
    .integer()
    .positive()
    .optional()
    .messages({
      'number.base': 'Category ID must be a number',
      'number.integer': 'Category ID must be an integer',
      'number.positive': 'Category ID must be positive'
    }),
  
  images: Joi.array()
    .items(
      Joi.string()
        .uri()
        .max(500) // URLs should be reasonable length
        .messages({
          'string.uri': 'Image must be a valid URL',
          'string.max': 'Image URL cannot exceed 500 characters'
        })
    )
    .max(10)
    .optional()
    .messages({
      'array.max': 'Cannot have more than 10 images'
    })
});

// Bid placement validation schema
const placeBidSchema = Joi.object({
  item_id: Joi.number()
    .integer()
    .positive()
    .required()
    .messages({
      'number.base': 'Item ID must be a number',
      'number.integer': 'Item ID must be an integer',
      'number.positive': 'Item ID must be positive',
      'any.required': 'Item ID is required'
    }),
  
  amount: Joi.number()
    .precision(2)
    .min(0.01)
    .max(999999.99)
    .required()
    .messages({
      'number.base': 'Bid amount must be a number',
      'number.min': 'Bid amount must be at least $0.01',
      'number.max': 'Bid amount cannot exceed $999,999.99',
      'any.required': 'Bid amount is required'
    })
});

// Credit purchase validation schema
const purchaseCreditsSchema = Joi.object({
  amount: Joi.number()
    .precision(2)
    .min(1.00)
    .max(10000.00)
    .required()
    .messages({
      'number.base': 'Amount must be a number',
      'number.min': 'Minimum purchase amount is $1.00',
      'number.max': 'Maximum purchase amount is $10,000.00',
      'any.required': 'Amount is required'
    }),
  
  payment_method: Joi.string()
    .valid('stripe', 'card', 'test', 'redemption_code')
    .required()
    .messages({
      'any.only': 'Payment method must be one of: stripe, card, test, redemption_code',
      'any.required': 'Payment method is required'
    }),
  
  transaction_id: Joi.string()
    .min(1)
    .max(255)
    .required()
    .messages({
      'string.min': 'Transaction ID is required',
      'string.max': 'Transaction ID cannot exceed 255 characters',
      'any.required': 'Transaction ID is required'
    })
});

// Pagination validation schema
const paginationSchema = Joi.object({
  limit: Joi.number()
    .integer()
    .min(1)
    .max(100)
    .default(20)
    .messages({
      'number.base': 'Limit must be a number',
      'number.integer': 'Limit must be an integer',
      'number.min': 'Limit must be at least 1',
      'number.max': 'Limit cannot exceed 100'
    }),
  
  offset: Joi.number()
    .integer()
    .min(0)
    .default(0)
    .messages({
      'number.base': 'Offset must be a number',
      'number.integer': 'Offset must be an integer',
      'number.min': 'Offset cannot be negative'
    })
});

// Credit transaction filter schema
const transactionFilterSchema = Joi.object({
  type: Joi.string()
    .valid('purchase', 'redemption', 'bid', 'refund', 'transfer', 'bonus')
    .optional()
    .messages({
      'any.only': 'Type must be one of: purchase, redemption, bid, refund, transfer, bonus'
    }),
  
  status: Joi.string()
    .valid('pending', 'completed', 'failed', 'cancelled')
    .optional()
    .messages({
      'any.only': 'Status must be one of: pending, completed, failed, cancelled'
    })
}).concat(paginationSchema);

module.exports = {
  createItemSchema,
  updateItemSchema,
  placeBidSchema,
  purchaseCreditsSchema,
  paginationSchema,
  transactionFilterSchema
};
