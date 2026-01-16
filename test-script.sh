#!/bin/bash
set -e

# Check for dependencies
if ! command -v jq &> /dev/null; then
    echo "Error: 'jq' is not installed. Please install it to run this script."
    exit 1
fi

# Base URL
URL="http://localhost:8080/api"
TS=$(date +%s)

echo "1. Creating Users..."
RESPONSE1=$(curl -s -X POST "$URL/users" -H "Content-Type: application/json" -d "{\"name\": \"Alice\", \"email\": \"alice$TS@test.com\"}")
USER1_ID=$(echo "$RESPONSE1" | jq -r '.id')

RESPONSE2=$(curl -s -X POST "$URL/users" -H "Content-Type: application/json" -d "{\"name\": \"Bob\", \"email\": \"bob$TS@test.com\"}")
USER2_ID=$(echo "$RESPONSE2" | jq -r '.id')

RESPONSE3=$(curl -s -X POST "$URL/users" -H "Content-Type: application/json" -d "{\"name\": \"Charlie\", \"email\": \"charlie$TS@test.com\"}")
USER3_ID=$(echo "$RESPONSE3" | jq -r '.id')

if [ "$USER1_ID" == "null" ] || [ "$USER2_ID" == "null" ] || [ "$USER3_ID" == "null" ]; then
    echo "Error: Failed to create users."
    echo "Alice: $RESPONSE1"
    echo "Bob: $RESPONSE2"
    echo "Charlie: $RESPONSE3"
    exit 1
fi

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
curl -s "$URL/balances/user/$USER1_ID" | jq

echo "Balances for User 2 (Bob):"
curl -s "$URL/balances/user/$USER2_ID" | jq
