-- V9: Drop FK constraints that reference app_user from conversation and document,
-- since user identity is managed by Keycloak (JWT sub) not a local app_user table.
-- The user_id / uploaded_by columns keep the UUID value from the JWT subject claim.

ALTER TABLE conversation DROP CONSTRAINT IF EXISTS conversation_user_id_fkey;
ALTER TABLE document     DROP CONSTRAINT IF EXISTS document_uploaded_by_fkey;
