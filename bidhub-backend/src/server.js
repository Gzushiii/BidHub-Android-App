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
const auctionsRoutes = require('./routes/auctions');

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
      upload: '/api/upload',
      auctions: '/api/auctions'
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
app.use('/api/auctions', auctionsRoutes);

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
    
    // Auto-fix stored procedures on startup
    try {
      const { fixStoredProcedures } = require('./utils/fixStoredProcedures');
      await fixStoredProcedures(pool);
    } catch (fixError) {
      console.error('⚠️  Warning: Failed to auto-fix stored procedures:', fixError.message);
      console.error('   Server will continue, but bid placement may fail.');
      console.error('   If you see SQL errors, manually apply the fix from sql/fix_placebid_groupby_error_simple.sql');
    }
    
    // Auto-process ended auctions on startup and set up periodic processing
    try {
      const AuctionEndService = require('./services/auctionEndService');
      console.log('🔍 Checking for ended auctions...');
      await AuctionEndService.processEndedAuctions();
      
      // Set up periodic processing every 5 minutes
      setInterval(async () => {
        try {
          await AuctionEndService.processEndedAuctions();
        } catch (error) {
          console.error('Error in periodic auction processing:', error);
        }
      }, 5 * 60 * 1000); // 5 minutes
      
      console.log('✅ Auction end processing scheduled (every 5 minutes)');
    } catch (auctionError) {
      console.error('⚠️  Warning: Failed to set up auction end processing:', auctionError.message);
      console.error('   Auctions may not be automatically closed when they end.');
    }
    
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
