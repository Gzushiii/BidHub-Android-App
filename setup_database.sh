#!/bin/bash

# BidHub Database Setup Script
# This script sets up the MySQL database for BidHub

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== BidHub Database Setup ===${NC}"

# Check if .env file exists
if [ ! -f .env ]; then
    echo -e "${RED}Error: .env file not found!${NC}"
    echo "Please create a .env file with your database credentials:"
    echo "DB_HOST=your_host"
    echo "DB_PORT=3306"
    echo "DB_USER=your_username"
    echo "DB_PASSWORD=your_password"
    echo "DB_NAME=bidhub_db"
    exit 1
fi

# Load environment variables
source .env

# Check if required environment variables are set
if [ -z "$DB_HOST" ] || [ -z "$DB_USER" ] || [ -z "$DB_PASSWORD" ] || [ -z "$DB_NAME" ]; then
    echo -e "${RED}Error: Missing required environment variables!${NC}"
    echo "Please ensure all required variables are set in .env file:"
    echo "DB_HOST, DB_USER, DB_PASSWORD, DB_NAME"
    exit 1
fi

echo -e "${YELLOW}Setting up database: $DB_NAME on $DB_HOST${NC}"

# Test database connection
echo -e "${YELLOW}Testing database connection...${NC}"
mysql -h "$DB_HOST" -P "${DB_PORT:-3306}" -u "$DB_USER" -p"$DB_PASSWORD" -e "SELECT 1;" 2>/dev/null

if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Cannot connect to MySQL database!${NC}"
    echo "Please check your database credentials in .env file"
    exit 1
fi

echo -e "${GREEN}Database connection successful!${NC}"

# Drop and recreate database
echo -e "${YELLOW}Dropping existing database (if exists)...${NC}"
mysql -h "$DB_HOST" -P "${DB_PORT:-3306}" -u "$DB_USER" -p"$DB_PASSWORD" -e "DROP DATABASE IF EXISTS $DB_NAME;" 2>/dev/null

# Create database
echo -e "${YELLOW}Creating database...${NC}"
mysql -h "$DB_HOST" -P "${DB_PORT:-3306}" -u "$DB_USER" -p"$DB_PASSWORD" -e "CREATE DATABASE $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null

if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Failed to create database!${NC}"
    exit 1
fi

echo -e "${GREEN}Database created successfully!${NC}"

# Run the schema script
echo -e "${YELLOW}Running database schema...${NC}"
mysql -h "$DB_HOST" -P "${DB_PORT:-3306}" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < bidhub_database_schema_complete.sql

if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Failed to run database schema!${NC}"
    exit 1
fi

echo -e "${GREEN}Database schema applied successfully!${NC}"

# Verify the setup
echo -e "${YELLOW}Verifying database setup...${NC}"
mysql -h "$DB_HOST" -P "${DB_PORT:-3306}" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < check_items_database_fixed.sql

if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Database verification failed!${NC}"
    exit 1
fi

echo -e "${GREEN}=== Database setup completed successfully! ===${NC}"
echo -e "${YELLOW}You can now run the Android app and test item posting.${NC}"
