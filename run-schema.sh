#!/bin/bash

# Load environment variables from .env file if it exists
if [ -f .env ]; then
    echo "📁 Loading environment variables from .env file..."
    export $(cat .env | grep -v '^#' | grep -v '^$' | xargs)
fi

# Database connection details - use environment variables for security
DB_HOST="${DB_HOST:-bidhub-bidhub.b.aivencloud.com}"
DB_PORT="${DB_PORT:-27575}"
DB_USER="${DB_USER:-avnadmin}"
DB_PASSWORD="${DB_PASSWORD}"
DB_NAME="${DB_NAME:-defaultdb}"

# Check if password is provided
if [ -z "$DB_PASSWORD" ]; then
    echo "❌ Error: DB_PASSWORD environment variable is required"
    echo "💡 Create a .env file with your database credentials or set DB_PASSWORD manually"
    echo "Usage:"
    echo "  Option 1: Create .env file with DB_PASSWORD=your_password"
    echo "  Option 2: DB_PASSWORD=your_password ./run-schema.sh"
    exit 1
fi

echo "Running BidHub database schema..."

# Run the schema file
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" --ssl "$DB_NAME" < bidhub_complete_schema_defaultdb.sql

if [ $? -eq 0 ]; then
    echo "✅ Database schema created successfully!"
else
    echo "❌ Failed to create database schema"
fi
