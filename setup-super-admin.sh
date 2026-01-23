#!/bin/bash

# ====================================================================
# Super Admin Setup Script for Techno ERP System (Linux/Mac)
# This script creates a super admin user with all necessary reference data
# ====================================================================

set -e  # Exit on error

echo "🚀 Starting Super Admin Setup..."
echo ""

# Check if Docker is running
if ! docker ps > /dev/null 2>&1; then
    echo "❌ Error: Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if PostgreSQL container is running
if ! docker ps | grep -q techno-postgres; then
    echo "❌ Error: techno-postgres container is not running."
    echo "   Please start the database using: docker-compose up -d postgres"
    exit 1
fi

echo "✅ Docker and PostgreSQL container are running"
echo ""

# Wait for PostgreSQL to be ready
echo "⏳ Waiting for PostgreSQL to be ready..."
sleep 3

# Check database connection
if ! docker exec techno-postgres pg_isready -U techno_admin -d techno_erp > /dev/null 2>&1; then
    echo "❌ Error: Cannot connect to PostgreSQL database."
    echo "   Please ensure the database is running and accessible."
    exit 1
fi

echo "✅ Database connection successful"
echo ""

# Execute SQL script
echo "📝 Executing SQL script to create super admin..."
docker exec -i techno-postgres psql -U techno_admin -d techno_erp < create-super-admin.sql

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Super Admin setup completed successfully!"
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  Login Credentials:"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  Username: admin"
    echo "  Password: admin123"
    echo "  National ID: 1111111111"
    echo "  Role: ADMIN (Super Admin)"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "You can now login at: http://localhost:3000/login"
    echo ""
else
    echo ""
    echo "❌ Error: Failed to execute SQL script"
    exit 1
fi
