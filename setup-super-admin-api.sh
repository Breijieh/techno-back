#!/bin/bash

# ====================================================================
# Super Admin Setup Script using Backend API (Linux/Mac)
# This script creates a super admin user via the backend API
# ====================================================================

set -e  # Exit on error

echo "🚀 Starting Super Admin Setup via API..."
echo ""

# Configuration
BACKEND_URL="http://localhost:8080"
ADMIN_USERNAME="admin"
ADMIN_PASSWORD="admin123"
ADMIN_NATIONAL_ID="1111111111"

# Check if backend is running
echo "⏳ Checking if backend is running..."
if ! curl -s -f "${BACKEND_URL}/api/public/health" > /dev/null 2>&1; then
    echo "❌ Error: Backend is not running at ${BACKEND_URL}"
    echo "   Please start the backend using: ./mvnw spring-boot:run"
    exit 1
fi

echo "✅ Backend is running"
echo ""

# Create super admin user via API
echo "📝 Creating super admin user..."

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${BACKEND_URL}/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"${ADMIN_USERNAME}\",
    \"password\": \"${ADMIN_PASSWORD}\",
    \"nationalId\": \"${ADMIN_NATIONAL_ID}\",
    \"userType\": \"ADMIN\"
  }")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 201 ]; then
    echo "✅ Super Admin user created successfully!"
elif [ "$HTTP_CODE" -eq 409 ]; then
    echo "⚠️  User already exists. Updating password..."
    # Note: Password update would require a separate endpoint
    echo "   If you need to reset the password, use the reset password feature in the UI."
elif [ "$HTTP_CODE" -eq 403 ]; then
    echo "❌ Error: Access denied. Registration endpoint might require authentication."
    echo "   Please check backend security configuration."
    exit 1
else
    echo "❌ Error: Failed to create user (HTTP $HTTP_CODE)"
    echo "   Response: $BODY"
    exit 1
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Login Credentials:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Username: ${ADMIN_USERNAME}"
echo "  Password: ${ADMIN_PASSWORD}"
echo "  National ID: ${ADMIN_NATIONAL_ID}"
echo "  Role: ADMIN (Super Admin)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "You can now login at: http://localhost:3000/login"
echo ""
