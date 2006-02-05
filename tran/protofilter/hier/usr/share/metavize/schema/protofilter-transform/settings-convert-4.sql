-- settings convert for release 3.2

DROP TABLE settings.tr_protofilter_pattern_tmp;

CREATE TABLE settings.tr_protofilter_pattern_tmp AS
    SELECT rule_id, -27 as metavize_id, protocol::text, description::text, category::text,
           definition::text, quality::text, blocked, alert, log, settings_id, position
    FROM settings.tr_protofilter_pattern;

DROP TABLE settings.tr_protofilter_pattern;

ALTER TABLE settings.tr_protofilter_pattern_tmp RENAME TO tr_protofilter_pattern;
ALTER TABLE settings.tr_protofilter_pattern ADD PRIMARY KEY (rule_id);
ALTER TABLE settings.tr_protofilter_pattern ALTER COLUMN rule_id SET NOT NULL;

ALTER TABLE settings.tr_protofilter_pattern
    ADD CONSTRAINT fk_tr_protofilter_pattern
        FOREIGN KEY (settings_id) REFERENCES settings.tr_protofilter_settings;
