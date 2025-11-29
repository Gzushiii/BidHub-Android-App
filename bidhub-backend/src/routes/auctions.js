const express = require('express');
const { authenticateToken } = require('../middleware/auth');
const { pool } = require('../config/database');
const AuctionEndService = require('../services/auctionEndService');

const router = express.Router();

/**
 * POST /api/auctions/process-ended
 * Process ended auctions and notify winners
 * This should be called periodically (e.g., via cron job)
 */
router.post('/process-ended', async (req, res) => {
  try {
    await AuctionEndService.processEndedAuctions();
    res.json({ 
      success: true, 
      message: 'Ended auctions processed successfully' 
    });
  } catch (error) {
    console.error('Error processing ended auctions:', error);
    res.status(500).json({ 
      error: 'Failed to process ended auctions',
      details: process.env.NODE_ENV === 'development' ? error.message : undefined
    });
  }
});

/**
 * GET /api/auctions/:itemId/winner
 * Get winner information for an ended auction
 */
router.get('/:itemId/winner', authenticateToken, async (req, res) => {
  try {
    const itemId = req.params.itemId;
    const winner = await AuctionEndService.getAuctionWinner(itemId);
    
    if (!winner) {
      return res.status(404).json({ 
        error: 'Auction winner not found',
        details: 'Auction may not have ended or has no winner'
      });
    }
    
    res.json({
      success: true,
      winner: {
        user_id: winner.winner_id,
        email: winner.email,
        alias: winner.alias,
        winning_bid: parseFloat(winner.winning_bid)
      }
    });
    
  } catch (error) {
    console.error('Error getting auction winner:', error);
    res.status(500).json({ 
      error: 'Failed to get auction winner',
      details: process.env.NODE_ENV === 'development' ? error.message : undefined
    });
  }
});

module.exports = router;

