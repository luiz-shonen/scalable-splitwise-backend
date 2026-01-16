#!/bin/bash

# Configuration
API_URL="http://localhost:8080/api"
TIMESTAMP=$(date +%s)

echo "Waiting for API to be ready..."
until $(curl --output /dev/null --silent --head --fail http://localhost:8080/actuator/health); do
    printf '.'
    sleep 1
done
echo -e "\nAPI is UP!"

echo "1. Creating Users for Full System Test..."
# Alice, Bob, Charlie
USER1_JSON=$(curl -s -X POST "$API_URL/users" -H "Content-Type: application/json" -d "{\"name\": \"Alice\", \"email\": \"alice$TIMESTAMP@test.com\"}")
USER1_ID=$(echo $USER1_JSON | jq '.id')

USER2_JSON=$(curl -s -X POST "$API_URL/users" -H "Content-Type: application/json" -d "{\"name\": \"Bob\", \"email\": \"bob$TIMESTAMP@test.com\"}")
USER2_ID=$(echo $USER2_JSON | jq '.id')

USER3_JSON=$(curl -s -X POST "$API_URL/users" -H "Content-Type: application/json" -d "{\"name\": \"Charlie\", \"email\": \"charlie$TIMESTAMP@test.com\"}")
USER3_ID=$(echo $USER3_JSON | jq '.id')

echo "Users Created: $USER1_ID (Alice), $USER2_ID (Bob), $USER3_ID (Charlie)"

echo -e "\n1b. Testing User Validation (Negative - Duplicate Email)..."
DUPLICATE_USER=$(curl -s -X POST "$API_URL/users" -H "Content-Type: application/json" -d "{\"name\": \"Alice 2\", \"email\": \"alice$TIMESTAMP@test.com\"}")
echo "Expected Validation Error (Duplicate Email):"
echo $DUPLICATE_USER | jq '.'

echo -e "\n2. Creating Group 'Housemates'..."
GROUP_JSON=$(curl -s -X POST "$API_URL/groups" -H "Content-Type: application/json" -d "{
    \"name\": \"Housemates $TIMESTAMP\",
    \"description\": \"Group for household expenses\",
    \"createdById\": $USER1_ID,
    \"memberIds\": [$USER2_ID, $USER3_ID]
}")
GROUP_ID=$(echo $GROUP_JSON | jq '.id')
echo "Group Created: $GROUP_ID ($(echo $GROUP_JSON | jq -r '.name'))"

echo -e "\n3. Testing Group Validation (Positive)..."
# Alice pays 90.00 for electricity within the group
EXPENSE_JSON=$(curl -s -X POST "$API_URL/expenses" -H "Content-Type: application/json" -d "{
    \"paidById\": $USER1_ID,
    \"groupId\": $GROUP_ID,
    \"description\": \"Electricity Bill\",
    \"amount\": 90.00,
    \"splitType\": \"EQUAL\",
    \"participantIds\": [$USER1_ID, $USER2_ID, $USER3_ID]
}")
echo "Group Expense Created:"
echo $EXPENSE_JSON | jq '.'

echo -e "\n4. Testing Group Validation (Negative - Payer not in group)..."
# Create a 4th user not in group
USER4_JSON=$(curl -s -X POST "$API_URL/users" -H "Content-Type: application/json" -d "{\"name\": \"Stranger\", \"email\": \"stranger$TIMESTAMP@test.com\"}")
USER4_ID=$(echo $USER4_JSON | jq '.id')

BAD_EXPENSE=$(curl -s -X POST "$API_URL/expenses" -H "Content-Type: application/json" -d "{
    \"paidById\": $USER4_ID,
    \"groupId\": $GROUP_ID,
    \"description\": \"Fraudulent Expense\",
    \"amount\": 10.00,
    \"splitType\": \"EQUAL\",
    \"participantIds\": [$USER1_ID]
}")
echo "Expected Validation Error (Payer not in group):"
echo $BAD_EXPENSE | jq '.'

echo -e "\n5. Testing Group Validation (Negative - Participant not in group)..."
BAD_EXPENSE2=$(curl -s -X POST "$API_URL/expenses" -H "Content-Type: application/json" -d "{
    \"paidById\": $USER1_ID,
    \"groupId\": $GROUP_ID,
    \"description\": \"Invalid Split\",
    \"amount\": 10.00,
    \"splitType\": \"EQUAL\",
    \"participantIds\": [$USER4_ID]
}")
echo "Expected Validation Error (Participant not in group):"
echo $BAD_EXPENSE2 | jq '.'

echo -e "\n6. Checking Group Details and Balances..."
echo "Group Details:"
curl -s -X GET "$API_URL/groups/$GROUP_ID" | jq '.'

echo -e "\nBalances for Alice (Payer):"
curl -s -X GET "$API_URL/balances/user/$USER1_ID" | jq '.'

echo -e "\nBalances for Bob (Member - should owe Alice 30.00):"
curl -s -X GET "$API_URL/balances/user/$USER2_ID" | jq '.'

echo -e "\n7. Testing Non-Group Expense (Standard Flow)..."
# Bob pays 20.00 for Charlie (lunch)
NON_GROUP_EXPENSE=$(curl -s -X POST "$API_URL/expenses" -H "Content-Type: application/json" -d "{
    \"paidById\": $USER2_ID,
    \"description\": \"Lunch\",
    \"amount\": 20.00,
    \"splitType\": \"EQUAL\",
    \"participantIds\": [$USER2_ID, $USER3_ID]
}")
echo "Individual Expense Created (Bob paid for Charlie):"
echo $NON_GROUP_EXPENSE | jq '.'

echo -e "\n8. Final Balance Check for Bob..."
# Should see Alice (owed 30.0) and Charlie (owes 10.0)
curl -s -X GET "$API_URL/balances/user/$USER2_ID" | jq '.'
