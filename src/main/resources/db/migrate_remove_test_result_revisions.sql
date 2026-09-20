-- Simplified CareS laboratory scope: one result and one PDF per test request.
-- Run after taking a backup when upgrading a database created with the former
-- revision workflow. Stored attachment files are not deleted by this migration.
DROP TABLE IF EXISTS test_result_attachment;
DROP TABLE IF EXISTS test_result_revision;
