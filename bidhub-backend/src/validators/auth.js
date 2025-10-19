const Joi = require('joi');

const registerValidator = Joi.object({
  username: Joi.string().alphanum().min(3).max(30).required(),
  email: Joi.string().email().required(),
  phone_number: Joi.string().pattern(/^[0-9+\-\s()]+$/).min(10).max(20).required(),
  password: Joi.string().min(8).max(128).required(),
  first_name: Joi.string().min(1).max(50).required(),
  last_name: Joi.string().min(1).max(50).required(),
  alias: Joi.string().alphanum().min(3).max(30).required()
});

const loginValidator = Joi.object({
  email: Joi.string().email().required(),
  password: Joi.string().required()
});

const updateProfileValidator = Joi.object({
  first_name: Joi.string().min(1).max(50).optional(),
  last_name: Joi.string().min(1).max(50).optional(),
  phone_number: Joi.string().pattern(/^[0-9+\-\s()]+$/).min(10).max(20).optional(),
  alias: Joi.string().alphanum().min(3).max(30).optional()
});

module.exports = {
  registerValidator,
  loginValidator,
  updateProfileValidator
};
