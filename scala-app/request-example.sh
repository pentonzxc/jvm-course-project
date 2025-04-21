#!/bin/bash

# Check for --quiet or -q argument
QUIET=false
if [[ "$1" == "-q" ]]; then
    QUIET=true
fi

# Send a POST request with JSON data
post_response=$(curl -s -X POST "http://localhost:8080/order/create" \
     -H "Content-Type: application/json" \
     -d '{
           "id": "550e8400-e29b-41d4-a716-446655440000",
           "items": [
             {
               "id": "c0a80101-0000-0000-0000-000000000001",
               "item": {
                 "name": "Laptop"
               },
               "cost": 1200,
               "amount": 2
             },
             {
               "id": "c0a80101-0000-0000-0000-000000000002",
               "item": {
                 "name": "Mouse"
               },
               "cost": 50,
               "amount": 1
             }
           ],
           "customer": {
             "id": "123e4567-e89b-12d3-a456-426614174000",
             "name": "John Doe",
             "city": "New York"
           }
         }')

# Print only if not quiet
if [ "$QUIET" = false ]; then
    printf "POST Response:\n%s\n\n" "$post_response"
fi

# Send a GET request and print the response
get_response=$(curl -s "http://localhost:8080/order?id=550e8400-e29b-41d4-a716-446655440000")

if [ "$QUIET" = false ]; then
    printf "GET Response:\n%s\n" "$get_response"
fi
