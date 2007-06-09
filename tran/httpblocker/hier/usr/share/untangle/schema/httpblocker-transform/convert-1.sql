-- convert script for release 1.4

-- We forgot a semi-colon in schema-0

ALTER TABLE tr_httpblk_extensions ADD CONSTRAINT FKBC81FBBB871AAD3E FOREIGN KEY (rule_id) REFERENCES string_rule;

ALTER TABLE tr_httpblk_extensions ADD CONSTRAINT FKBC81FBBB1CAE658A FOREIGN KEY (setting_id) REFERENCES tr_httpblk_settings;
