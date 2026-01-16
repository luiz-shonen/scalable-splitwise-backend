#!/bin/bash

# Configuration
API_URL="http://localhost:8080/api"
TIMESTAMP=$(date +%s)

echo "1. Creating Users for Group Test..."
# Create 3 users
USER1_JSON=$(curl -s -X POST "$API_URL/users" -H "Content-Type: application/json" -d "{\"name\": \"Group Alice\", \"email\": \"galice$TIMESTAMP@test.com\"}")
USER1_ID=$(echo $USER1_JSON | jq '.id')

USER2_JSON=$(curl -s -X POST "$API_URL/users" -H "Content-Type: application/json" -d "{\"name\": \"Group Bob\", \"email\": \"gbob$TIMESTAMP@test.com\"}")
USER2_ID=$(echo $USER2_JSON | jq '.id')

USER3_JSON=$(curl -s -X POST "$API_URL/users" -H "Content-Type: application/json" -d "{\"name\": \"Group Charlie\", \"email\": \"gcharlie$TIMESTAMP@test.com\"}")
USER3_ID=$(echo $USER3_JSON | jq '.id')

echo "Users Created: $USER1_ID, $USER2_ID, $USER3_ID"

echo -e "\n2. Creating Group 'Ski Trip'..."
GROUP_JSON=$(curl -s -X POST "$API_URL/groups" -H "Content-Type: application/json" -d "{
    \"name\": \"Ski Trip $TIMESTAMP\",
    \"description\": \"Expenses for the annual ski trip\",
    \"createdById\": $USER1_ID,
    \"memberIds\": [$USER1_ID, $USER2_ID, $USER3_ID]
}")
GROUP_ID=$(echo $GROUP_JSON | jq '.id')
echo "Group Created: $GROUP_ID"

echo -e "\n3. Creating Group Expense (Equal Split)..."
# Alice pays 150.00 for the cabin
EXPENSE_JSON=$(curl -s -X POST "$API_URL/expenses" -H "Content-Type: application/json" -d "{
    \"paidById\": $USER1_ID,
    \"groupId\": $GROUP_ID,
    \"description\": \"Cabin Rental\",
    \"amount\": 150.00,
    \"splitType\": \"EQUAL\",
    \"participantIds\": [$USER1_ID, $USER2_ID, $USER3_ID]
}")
echo "Expense Created:"
echo $EXPENSE_JSON | jq '.'

echo -e "\n4. Verifying Group Details..."
GROUP_DETAILS=$(curl -s -X GET "$API_URL/groups/$GROUP_ID")
echo "Group Members Count: $(echo $GROUP_DETAILS | jq '.members | length')"
echo "Group Expenses Count: $(echo $GROUP_DETAILS | jq '.expenses | length')"

echo -e "\n5. Checking Net Balances for Group Members..."
echo "Balances for Group Alice (Payer):"
curl -s -X GET "$API_URL/balances/user/$USER1_ID" | jq '.'

echo -e "\nBalances for Group Bob (Member):"
curl -s -X GET "$API_URL/balances/user/$USER2_ID" | jq '.'

echo -e "\nBalances for Group Charlie (Member):"
curl -s -X GET "$API_URL/balances/user/$USER3_ID" | jq '.'
