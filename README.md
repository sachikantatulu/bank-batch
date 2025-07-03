Complete Testing Steps
Prerequisites
Java 17+, Maven 3.8+

Postman/curl for API testing

Sample txt file (dataSource.txt):
txt
accountNumber,trxAmount,description,trxDate,trxTime,customerId
123456,150.75,Payment,2023-01-15,09:30:00,CUST001
789012,99.99,Refund,2023-01-16,14:45:00,CUST002
Step 1: Setup & Run
bash
# Clone project
git clone https://github.com/your-repo/bank-batch.git
cd bank-batch

# Add txt to resources
cp /path/to/dataSource.txt src/main/resources/

# Build and run
mvn spring-boot:run
Step 2: Initialize Users
bash
# Insert users (already in import.sql)
INSERT INTO app_user (username, password, role) VALUES 
  ('admin', '$2a$10$XptfskLsT1l/bRTLRiiCgejHqOpgXFreUnNUa35gJdCr2v2QbVFzu', 'ADMIN'),
  ('user', '$2a$10$XptfskLsT1l/bRTLRiiCgejHqOpgXFreUnNUa35gJdCr2v2QbVFzu', 'USER');
# Password: "password" (BCrypt encoded)
Step 3: Test Batch Job
bash
curl -X POST http://localhost:8080/api/transactions/batch/start \
  -u admin:password \
  -H "Content-Type: application/json"
Expected Response:
200 OK: "Batch job started successfully"
Check logs for: BATCH JOB COMPLETED SUCCESSFULLY

Step 4: Test Search API
bash
curl -G http://localhost:8080/api/transactions \
  -u user:password \
  --data-urlencode "customerId=CUST001" \
  --data-urlencode "page=0" \
  --data-urlencode "size=10"
Expected Response:
Paginated JSON with matching transactions

Step 5: Test Update with Optimistic Locking
bash
# First get current version (e.g., version=0)
curl http://localhost:8080/api/transactions/1 -u user:password

# Valid update
curl -X PUT http://localhost:8080/api/transactions/1 \
  -u user:password \
  -H "Content-Type: application/json" \
  -d '{"newDescription":"Updated", "version":0}'

# Concurrent update (use same version twice)
curl ... -d '{"newDescription":"Conflict", "version":0}'
Expected Responses:

First: 200 OK with updated transaction (version=1)

Second: 409 Conflict: "Version mismatch"

Step 6: Test Security
bash
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
