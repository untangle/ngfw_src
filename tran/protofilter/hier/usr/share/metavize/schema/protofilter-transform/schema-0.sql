CREATE TABLE tr_protofilter_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    buffersize int4,
    bytelimit int4,
    chunklimit int4,
    unknownstring varchar(255),
    stripzeros bool,
    PRIMARY KEY (settings_id));

CREATE TABLE tr_protofilter_pattern (
    rule_id int8 NOT NULL,
    protocol varchar(255),
    description varchar(255),
    category varchar(255),
    definition varchar(4096),
    quality varchar(255),
    blocked bool,
    alert bool,
    log bool,
    settings_id int8,
    position int4,
    PRIMARY KEY (rule_id));

CREATE TABLE tr_protofilter_evt (
    event_id int8 NOT NULL,
    session_id int4,
    protocol varchar(255),
    blocked bool,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

ALTER TABLE tr_protofilter_settings ADD CONSTRAINT FK55F095631446F FOREIGN KEY (tid) REFERENCES tid;

ALTER TABLE tr_protofilter_pattern ADD CONSTRAINT FKE929349B79192AB7 FOREIGN KEY (settings_id) REFERENCES tr_protofilter_settings;
