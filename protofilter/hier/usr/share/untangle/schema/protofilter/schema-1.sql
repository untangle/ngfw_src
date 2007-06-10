-- schema for release 2.5

-------------
-- settings |
-------------

-- com.untangle.tran.protofilter.ProtoFilterSettings
CREATE TABLE settings.tr_protofilter_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    buffersize int4,
    bytelimit int4,
    chunklimit int4,
    unknownstring varchar(255),
    stripzeros bool,
    PRIMARY KEY (settings_id));

-- com.untangle.tran.protofilter.ProtoFilterPattern
CREATE TABLE settings.tr_protofilter_pattern (
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

-----------
-- events |
-----------

-- com.untangle.tran.protofilter.ProtoFilterLogEvent
CREATE TABLE events.tr_protofilter_evt (
    event_id int8 NOT NULL,
    session_id int4,
    protocol varchar(255),
    blocked bool,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

----------------
-- constraints |
----------------

-- indeces for reporting

CREATE INDEX tr_protofilter_sid_idx ON tr_protofilter_evt (session_id);

-- foreign key constraints

ALTER TABLE settings.tr_protofilter_settings
    ADD CONSTRAINT fk_tr_protofilter_settings
        FOREIGN KEY (tid) REFERENCES settings.tid;

ALTER TABLE settings.tr_protofilter_pattern
    ADD CONSTRAINT fk_tr_protofilter_pattern
        FOREIGN KEY (settings_id) REFERENCES settings.tr_protofilter_settings;

