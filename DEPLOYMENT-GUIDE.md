# Render Deployment Guide for RentalPH Backend

This guide will walk you through deploying your RentalPH backend to Render and connecting it to your Aiven MySQL database.

## Prerequisites

- ✅ Aiven MySQL database set up and running
- ✅ MySQL Workbench connected to Aiven
- ✅ GitHub repository with your code
- ✅ Render account (free tier available)

## Step 1: Prepare Your Aiven Database

### 1.1 Get Aiven Connection Details

1. Log into your Aiven dashboard
2. Select your MySQL service
3. Go to the "Overview" tab
4. Note down the following details:
   - **Host** (e.g., `mysql-12345-abc123.aivencloud.com`)
   - **Port** (usually `3306`)
   - **Database name** (e.g., `defaultdb`)
   - **Username** (e.g., `avnadmin`)
   - **Password** (click "Show" to reveal)

### 1.2 Create Database and Tables

1. Open MySQL Workbench
2. Connect to your Aiven database using the details above
3. Run the `mysql_schema.sql` file from your project root
4. Verify the tables were created successfully

## Step 2: Deploy to Render

### 2.1 Connect GitHub Repository

1. Go to [Render Dashboard](https://dashboard.render.com)
2. Click "New" → "Web Service"
3. Connect your GitHub account
4. Select your repository: `Grp4_IT3B_CC106T`
5. Choose the `backend` folder as the root directory

### 2.2 Configure Build Settings

Set the following in Render:

- **Name**: `rentalph-backend` (or your preferred name)
- **Environment**: `Node`
- **Build Command**: `npm install`
- **Start Command**: `npm start`
- **Node Version**: `18` (or latest)

### 2.3 Set Environment Variables

In the Render dashboard, go to "Environment" tab and add:

```
NODE_ENV=production
PORT=3000
DB_HOST=your-aiven-mysql-host
DB_PORT=3306
DB_NAME=rentalph_db
DB_USER=your-aiven-username
DB_PASSWORD=your-aiven-password
DB_SSL=true
JWT_SECRET=your-super-secret-jwt-key-here
JWT_EXPIRES_IN=24h
CORS_ORIGIN=*
```

**Important**: 
- Replace the Aiven values with your actual credentials
- Generate a strong JWT_SECRET (you can use an online generator)
- Keep these values secure and never commit them to Git

### 2.4 Deploy

1. Click "Create Web Service"
2. Render will automatically build and deploy your app
3. Wait for the deployment to complete (usually 2-3 minutes)
4. Note your app URL (e.g., `https://rentalph-backend.onrender.com`)

## Step 3: Test Your Deployment

### 3.1 Health Check

Visit your app URL + `/health`:
```
https://your-app-name.onrender.com/health
```

You should see:
```json
{
  "success": true,
  "message": "RentalPH Backend API is running",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "environment": "production"
}
```

### 3.2 Test API Endpoints

#### Register a Test User
```bash
curl -X POST https://your-app-name.onrender.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test123!",
    "role": "tenant"
  }'
```

#### Login
```bash
curl -X POST https://your-app-name.onrender.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test123!"
  }'
```

## Step 4: Update Your Android App

### 4.1 Update DatabaseConfig.java

In your Android project, update the API base URL:

```java
public class DatabaseConfig {
    // Change this to your Render app URL
    public static final String API_BASE_URL = "https://your-app-name.onrender.com/api/";
    
    // Keep other configurations as they are
    public static final String DB_HOST = "your-aiven-mysql-host";
    public static final String DB_USERNAME = "your-aiven-username";
    public static final String DB_PASSWORD = "your-aiven-password";
    public static final String DB_NAME = "rentalph_db";
}
```

### 4.2 Test Android Connection

1. Build and run your Android app
2. Try to register a new user
3. Check if the user appears in your Aiven database via MySQL Workbench

## Step 5: Monitor and Maintain

### 5.1 Render Dashboard

- Monitor your app's health and performance
- Check logs for any errors
- Monitor resource usage (free tier has limits)

### 5.2 Aiven Monitoring

- Monitor database performance
- Check connection limits
- Monitor storage usage

### 5.3 Common Issues and Solutions

#### Issue: "Database connection failed"
**Solution**: 
- Verify Aiven credentials are correct
- Check if Aiven service is running
- Ensure SSL is enabled

#### Issue: "CORS errors in Android app"
**Solution**:
- Update `CORS_ORIGIN` in Render environment variables
- Add your Android app's domain if needed

#### Issue: "App goes to sleep on free tier"
**Solution**:
- Free tier apps sleep after 15 minutes of inactivity
- First request after sleep takes longer to respond
- Consider upgrading to paid plan for production

## Step 6: Production Considerations

### 6.1 Security

- Use strong, unique passwords
- Rotate JWT secrets regularly
- Enable HTTPS (included with Render)
- Consider IP whitelisting for database access

### 6.2 Performance

- Monitor response times
- Consider database indexing
- Implement caching if needed
- Upgrade to paid Render plan for better performance

### 6.3 Backup

- Regular database backups via Aiven
- Code backups via GitHub
- Environment variable backups

## Troubleshooting

### Check Render Logs

1. Go to your Render dashboard
2. Click on your service
3. Go to "Logs" tab
4. Look for error messages

### Test Database Connection

Use MySQL Workbench to verify:
1. Database is accessible
2. Tables exist
3. Data can be inserted/retrieved

### Verify Environment Variables

In Render dashboard, ensure all environment variables are set correctly and match your Aiven credentials.

## Next Steps

1. **Set up monitoring**: Consider adding application monitoring
2. **Implement CI/CD**: Automate deployments from GitHub
3. **Add testing**: Implement automated tests
4. **Scale up**: Consider upgrading to paid plans as your app grows
5. **Add features**: Implement additional API endpoints as needed

## Support

If you encounter issues:

1. Check Render logs first
2. Verify Aiven database connectivity
3. Test API endpoints individually
4. Check environment variables
5. Review this guide for common solutions

For additional help:
- Render Documentation: https://render.com/docs
- Aiven Documentation: https://docs.aiven.io/
- Node.js/Express Documentation: https://expressjs.com/
