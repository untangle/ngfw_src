-- schema for release-2.5

-------------
-- settings |
-------------

-- com.untangle.tran.airgap.AirgapSettings
CREATE TABLE settings.tr_airgap_settings (
    id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    PRIMARY KEY (id));

----------------
-- constraints |
----------------

-- foreign key constraints

ALTER TABLE settings.tr_airgap_settings
    ADD CONSTRAINT fk_tr_airgap_settings FOREIGN KEY (tid) REFERENCES tid;
