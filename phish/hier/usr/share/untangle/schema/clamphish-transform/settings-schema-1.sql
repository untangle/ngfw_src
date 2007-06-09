-- settings schema for release-5.0

CREATE TABLE settings.n_phish_settings (
    spam_settings_id int8 NOT NULL,
    enable_google_sb bool NOT NULL,
    PRIMARY KEY (spam_settings_id));

-- foreign key constraints

ALTER TABLE settings.tr_clamphish_settings
    ADD CONSTRAINT fk_clamphish_to_spam_settings
    FOREIGN KEY (spam_settings_id)
    REFERENCES settings.tr_spam_settings;

-- this is a conversion step in case the user has a pre-existing
-- clamphish-transform that was initialized using only the spam-base
-- schema.

INSERT INTO tr_clamphish_settings
    SELECT ss.settings_id AS spam_settings_id, true AS enable_google_sb
    FROM transform_persistent_state
    JOIN tr_spam_settings ss USING (tid)
    WHERE name = 'clamphish-transform';
