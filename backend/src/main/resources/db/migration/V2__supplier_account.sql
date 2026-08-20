ALTER TABLE supplier ADD COLUMN account_username VARCHAR(60);
CREATE UNIQUE INDEX idx_supplier_account ON supplier(account_username);
