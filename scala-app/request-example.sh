#!/bin/bash

# Send a POST request with JSON data
response=$(curl -X POST "http://localhost:8080/order/create" \
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
         }' -v)
printf "%s\n" "$response"

# # Send a POST request using data from a file
# curl -X POST "http://localhost:8080/order/create" \
#      --data "@data-example.json" \
#      -H "Content-Type: application/json"

# Send a GET request and print the response
response=$(curl -s "http://localhost:8080/order?id=550e8400-e29b-41d4-a716-446655440000")
printf "%s\n" "$response"
