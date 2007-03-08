-- settings schema for release 4.2

CREATE TABLE settings.tr_clamphish_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    smtp_inbound int8 NOT NULL,
    smtp_outbound int8 NOT NULL,
    pop_inbound int8 NOT NULL,
    pop_outbound int8 NOT NULL,
    imap_inbound int8 NOT NULL,
    imap_outbound int8 NOT NULL,
    enable_google_sb bool NOT NULL,
    PRIMARY KEY (settings_id));

-- foreign key constraints

ALTER TABLE settings.tr_clamphish_settings
    ADD CONSTRAINT fk_settings_to_tid
    FOREIGN KEY (tid)
    REFERENCES settings.tid;

ALTER TABLE settings.tr_clamphish_settings
    ADD CONSTRAINT fk_in_ss_smtp_cfg
    FOREIGN KEY (smtp_inbound)
    REFERENCES settings.tr_spam_smtp_config;

ALTER TABLE settings.tr_clamphish_settings
    ADD CONSTRAINT fk_out_ss_smtp_cfg
    FOREIGN KEY (smtp_outbound)
    REFERENCES settings.tr_spam_smtp_config;

ALTER TABLE settings.tr_clamphish_settings
    ADD CONSTRAINT fk_in_ss_pop_cfg
    FOREIGN KEY (pop_inbound)
    REFERENCES settings.tr_spam_pop_config;

ALTER TABLE settings.tr_clamphish_settings
    ADD CONSTRAINT fk_out_ss_pop_cfg
    FOREIGN KEY (pop_outbound)
    REFERENCES settings.tr_spam_pop_config;

ALTER TABLE settings.tr_clamphish_settings
    ADD CONSTRAINT fk_in_ss_imap_cfg
    FOREIGN KEY (imap_inbound)
    REFERENCES settings.tr_spam_imap_config;

ALTER TABLE settings.tr_clamphish_settings
    ADD CONSTRAINT fk_out_ss_imap_cfg
    FOREIGN KEY (imap_outbound)
    REFERENCES settings.tr_spam_imap_config;

