CREATE TABLE tr_airgap_settings (
    id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    PRIMARY KEY (id));

ALTER TABLE tr_airgap_settings ADD CONSTRAINT FK7B2CA9F51446F FOREIGN KEY (tid) REFERENCES tid;
