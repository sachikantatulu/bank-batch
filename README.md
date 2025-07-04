Complete Testing Steps:
Prerequisites
Java 17+, Maven 3.8+
Postman/curl for API testing
Sample txt file (dataSource.txt):

txt:
ACCOUNT_NUMBER|TRX_AMOUNT|DESCRIPTION|TRX_DATE|TRX_TIME|CUSTOMER_ID
8872838283|123.00|FUND TRANSFER|2019-09-12|11:11:11|222
8872838283|1123.00|ATM WITHDRWAL|2019-09-11|11:11:11|222

Step 1: Setup & Run
# Clone project
git clone https://github.com/sachikantatulu/bank-batch.git

cd bank-batch

# Add txt to resources
cp /path/to/dataSource.txt src/main/resources/

# Build and run
mvn spring-boot:run
Step 2: Initialize Users
# Insert users (already in import.sql)
INSERT INTO app_user (username, password, role) VALUES
  ('admin', '$2a$10$a.HWcaBHsmUwlGb.UcTWGuk2cZVZl6j7vhgB2RXOREiRig.4Q6tI.', 'ADMIN'),
  ('user', '$2a$10$a.HWcaBHsmUwlGb.UcTWGuk2cZVZl6j7vhgB2RXOREiRig.4Q6tI.', 'USER');
# Password: "password" (BCrypt encoded)
we can test through postman easily imprort postman_collection.json to test all endpoints instantly:

Step 3: Test Batch Job
curl -X POST http://localhost:8080/api/transactions/batch/start \
  -u admin:password \
  -H "Content-Type: application/json"
Expected Response:
200 OK: "Batch job started successfully"
Check logs for: BATCH JOB COMPLETED SUCCESSFULLY

Step 4: Test Search API
curl --location 'http://localhost:8080/api/transactions?customerId=222&page=0&size=5' \
--header 'Authorization: Basic dXNlcjpwYXNzd29yZA=='
Expected Response:
{"content":[{"id":1,"accountNumber":"8872838283","trxAmount":123.0000,"description":"Updated payment","trxDate":"2019-09-12","trxTime":"11:11:11","customerId":"222","version":1},{"id":2,"accountNumber":"8872838283","trxAmount":1123.0000,"description":"ATM WITHDRAWAL","trxDate":"2019-09-11","trxTime":"11:11:11","customerId":"222","version":0},{"id":3,"accountNumber":"8872838283","trxAmount":1223.0000,"description":"FUND TRANSFER","trxDate":"2019-10-11","trxTime":"11:11:11","customerId":"222","version":0},{"id":4,"accountNumber":"8872838283","trxAmount":1233.0000,"description":"3rd Party FUND TRANSFER","trxDate":"2019-11-11","trxTime":"11:11:11","customerId":"222","version":0},{"id":5,"accountNumber":"8872838283","trxAmount":1243.0000,"description":"3rd Party FUND TRANSFER","trxDate":"2019-08-11","trxTime":"11:11:11","customerId":"222","version":0}],"pageable":{"pageNumber":0,"pageSize":5,"sort":{"empty":true,"sorted":false,"unsorted":true},"offset":0,"paged":true,"unpaged":false},"last":false,"totalPages":13,"totalElements":62,"first":true,"size":5,"number":0,"sort":{"empty":true,"sorted":false,"unsorted":true},"numberOfElements":5,"empty":false}

Step 5: Test Update with Optimistic Locking
# First get current version (e.g., version=0)
curl http://localhost:8080/api/transactions/1 -u user:password

# Valid update
curl --location --request PUT 'http://localhost:8080/api/transactions/1' \
--header 'Content-Type: application/json' \
--header 'Authorization: Basic dXNlcjpwYXNzd29yZA==' \
--data '{
  "newDescription": "Updated payment",
  "version": 0
}'

# Concurrent update (use same version twice)
curl ... -d '{"newDescription":"Conflict", "version":0}'
Expected Responses:

First: 200 OK with updated transaction (version=1)
Second: 409 Conflict: "Transaction was updated by another user"

Step 6: Test Security

# Unauthenticated access
curl -X POST http://localhost:8080/api/transactions/batch/start

# User accessing admin endpoint
curl -X POST http://localhost:8080/api/transactions/batch/start -u user:password
Expected Responses:

401 Unauthorized
403 Forbidden

Key Design Patterns Used:
Repository Pattern (JPA):
Abstracts database operations for Transaction and AppUser entities
Why: Simplifies data access and promotes separation of concerns

Builder Pattern (Spring Batch):
Used in JobBuilder and StepBuilder
Why: Provides a clean way to construct complex batch job configurations

Optimistic Locking:
Implemented via @Version in Transaction entity
Why: Handles concurrent updates without database-level locking

MVC Pattern:
Separation of Controller/Service/Repository layers
Why: Standard Spring architecture for maintainability 
