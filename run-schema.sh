#!/bin/bash

# Database connection details - use environment variables for security
DB_HOST="${DB_HOST:-bidhub-bidhub.b.aivencloud.com}"
DB_PORT="${DB_PORT:-27575}"
DB_USER="${DB_USER:-avnadmin}"
DB_PASSWORD="${DB_PASSWORD}"
DB_NAME="${DB_NAME:-defaultdb}"

# Check if password is provided
if [ -z "$DB_PASSWORD" ]; then
    echo "❌ Error: DB_PASSWORD environment variable is required"
    echo "Usage: DB_PASSWORD=your_password ./run-schema.sh"
    exit 1
fi

echo "Running BidHub database schema..."

# Run the schema file
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" --ssl-mode=REQUIRED "$DB_NAME" < bidhub_database_schema.sql

if [ $? -eq 0 ]; then
    echo "✅ Database schema created successfully!"
else
    echo "❌ Failed to create database schema"
fi
