// Load environment variables
if (process.env.NODE_ENV !== 'production') {
  require('dotenv').config({ path: '../.env' });
} else {
  // In production, environment variables are provided by Render
  require('dotenv').config();
}
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const path = require('path');

// Import database configuration
const { initializeDatabase, pool } = require('./config/database');

// Import services
const keepAliveService = require('./services/keepAlive');

// Import API routes
const authRoutes = require('./routes/auth');
const itemsRoutes = require('./routes/items');
const bidsRoutes = require('./routes/bids');
const creditsRoutes = require('./routes/credits');
const categoriesRoutes = require('./routes/categories');
const topupsRoutes = require('./routes/topups');
const uploadRoutes = require('./routes/upload');

const app = express();
const PORT = process.env.PORT || 3000;

// Behind Render/Proxies, trust X-Forwarded-* so rate-limit works
app.set('trust proxy', 1);

// Security middleware
app.use(helmet());
app.use(cors({
  origin: process.env.CORS_ORIGIN || '*',
  credentials: true
}));

// Rate limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // limit each IP to 100 requests per windowMs
  message: 'Too many requests from this IP, please try again later.'
});
app.use('/api/', limiter);

// Body parsing
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));

// Serve uploaded images statically
app.use('/uploads', express.static(path.join(__dirname, '../uploads')));

// Root endpoint
app.get('/', (req, res) => {
  res.json({ 
    message: 'Welcome to BidHub API!',
    version: '1.0.0',
    endpoints: {
      health: '/api/health',
      auth: '/api/auth',
      items: '/api/items',
      bids: '/api/bids',
      credits: '/api/credits',
      categories: '/api/categories',
      topups: '/api/topups',
      upload: '/api/upload'
    },
    documentation: 'Visit /api/health for server status'
  });
});

// Health check endpoint
app.get('/api/health', async (req, res) => {
  // Test database connection
  let dbStatus = 'OK';
  try {
    await pool.query('SELECT 1');
  } catch (err) {
    dbStatus = 'ERROR';
  }

  res.json({ 
    status: 'OK', 
    timestamp: new Date().toISOString(),
    environment: process.env.NODE_ENV || 'production',
    message: 'BidHub API is running successfully on Render!',
    version: '2025-11-03-v1',
    debugMode: 'active',
    database: dbStatus,
    keepAlive: keepAliveService.getStatus()
  });
});

// API routes
app.use('/api/auth', authRoutes);
app.use('/api/items', itemsRoutes);
app.use('/api/bids', bidsRoutes);
app.use('/api/credits', creditsRoutes);
app.use('/api/categories', categoriesRoutes);
app.use('/api/topups', topupsRoutes);
app.use('/api/upload', uploadRoutes);

// 404 handler
app.use('*', (req, res) => {
  res.status(404).json({ error: 'Endpoint not found' });
});

// Error handler
app.use((err, req, res, next) => {
  console.error('API Error:', err);
  res.status(500).json({ 
    error: 'Internal server error',
    ...(process.env.NODE_ENV === 'development' && { details: err.message })
  });
});

// Start server
const startServer = async () => {
  try {
    // Initialize database connection
    await initializeDatabase();
    
    // Start the server
    app.listen(PORT, () => {
      console.log(`BidHub API server running on port ${PORT}`);
      console.log(`Environment: ${process.env.NODE_ENV || 'production'}`);
      console.log(`Database: ${process.env.DB_HOST}:${process.env.DB_PORT}`);
      
      // Start keep-alive service
      keepAliveService.start();
    });
  } catch (error) {
    console.error('Failed to start server:', error);
    process.exit(1);
  }
};

// Graceful shutdown
process.on('SIGTERM', () => {
  console.log('SIGTERM received, shutting down gracefully');
  keepAliveService.stop();
  process.exit(0);
});

process.on('SIGINT', () => {
  console.log('SIGINT received, shutting down gracefully');
  keepAliveService.stop();
  process.exit(0);
});

startServer();

module.exports = app;
