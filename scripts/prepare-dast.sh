#!/bin/bash
set -e

# Configuration
API_URL="http://localhost:8088"
ZAP_REPORTS_DIR="zap-reports"
BACKEND_DEV_DIR="backend/dev"

# Ensure reports directory exists
mkdir -p "$ZAP_REPORTS_DIR"
chmod 777 "$ZAP_REPORTS_DIR"

# Function to get token
get_token() {
    local email=$1
    local password=$2
    local output_var=$3

    echo "Logging in as $email..."
    response=$(curl -s -X POST "$API_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"$email\",\"password\":\"$password\"}")

    token=$(echo "$response" | jq -r .token)

    if [ "$token" == "null" ] || [ -z "$token" ]; then
        echo "Failed to get token for $email!"
        # Print response for debugging (be careful with secrets in production logs, but this is a test user)
        echo "Response: $response"
        exit 1
    fi

    echo "Token acquired."
    # Masking the token in GitHub Actions logs
    echo "::add-mask::$token"

    # Return the token via the variable name passed as argument
    eval "$output_var='$token'"
}

# 1. Get Tokens
get_token "test@everactive.pl" "password" TOKEN_USER
get_token "test-manager@everactive.pl" "password" TOKEN_MGR

# 2. Configure ZAP Scripts
echo "Configuring ZAP scripts..."

# Copy base scripts
cp "$BACKEND_DEV_DIR/zap.yaml" "$ZAP_REPORTS_DIR/zap.yaml"
cp "$BACKEND_DEV_DIR/add-auth-header.js" "$ZAP_REPORTS_DIR/add-auth-header.js"

# Prepare SetToken scripts based on template
TEMPLATE=$(cat "$BACKEND_DEV_DIR/set-token.js")

# No Auth (Token is empty/null)
echo "$TEMPLATE" | sed "s|TOKEN_PLACEHOLDER||" > "$ZAP_REPORTS_DIR/set-token-noauth.js"

# User Auth
echo "$TEMPLATE" | sed "s|TOKEN_PLACEHOLDER|$TOKEN_USER|" > "$ZAP_REPORTS_DIR/set-token-user.js"

# Manager Auth
echo "$TEMPLATE" | sed "s|TOKEN_PLACEHOLDER|$TOKEN_MGR|" > "$ZAP_REPORTS_DIR/set-token-manager.js"

echo "ZAP environment prepared successfully."
