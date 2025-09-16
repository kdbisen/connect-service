// MongoDB initialization script
db = db.getSiblingDB('connect-service');

// Create collections
db.createCollection('processing_requests');
db.createCollection('processing_audit_logs');

// Create indexes for better performance
db.processing_requests.createIndex({ "requestId": 1 }, { unique: true });
db.processing_requests.createIndex({ "clientId": 1 });
db.processing_requests.createIndex({ "status": 1 });
db.processing_requests.createIndex({ "requestType": 1 });
db.processing_requests.createIndex({ "createdAt": 1 });
db.processing_requests.createIndex({ "status": 1, "createdAt": 1 });

db.processing_audit_logs.createIndex({ "processingRequestId": 1 });
db.processing_audit_logs.createIndex({ "stepName": 1 });
db.processing_audit_logs.createIndex({ "status": 1 });
db.processing_audit_logs.createIndex({ "createdAt": 1 });

print('MongoDB initialization completed');
