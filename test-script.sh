#!/bin/bash
set -e

# Check for dependencies
if ! command -v jq &> /dev/null; then
    echo "Error: 'jq' is not installed. Please install it to run this script."
    exit 1
fi

# Base URL
URL="http://localhost:8080/api"

echo "1. Creating Users..."
USER1_ID=$(curl -s -X POST "$URL/users" -H "Content-Type: application/json" -d '{"name": "Alice", "email": "alice@test.com"}' | jq -r '.id')
USER2_ID=$(curl -s -X POST "$URL/users" -H "Content-Type: application/json" -d '{"name": "Bob", "email": "bob@test.com"}' | jq -r '.id')
USER3_ID=$(curl -s -X POST "$URL/users" -H "Content-Type: application/json" -d '{"name": "Charlie", "email": "charlie@test.com"}' | jq -r '.id')

echo "Users Created: $USER1_ID, $USER2_ID, $USER3_ID"

echo -e "\n2. Creating Expense (Equal Split)..."
curl -s -X POST "$URL/expenses" \
     -H "Content-Type: application/json" \
     -d "{
           \"description\": \"Dinner\",
           \"amount\": 100.00,
           \"paidById\": $USER1_ID,
           \"splitType\": \"EQUAL\",
           \"participantIds\": [$USER1_ID, $USER2_ID, $USER3_ID]
         }" | jq

echo -e "\n3. Checking Balances..."
echo "Balances for User 1 (Alice - Paid):"
curl -s "$URL/balances/$USER1_ID" | jq

echo "Balances for User 2 (Bob):"
curl -s "$URL/balances/$USER2_ID" | jq
