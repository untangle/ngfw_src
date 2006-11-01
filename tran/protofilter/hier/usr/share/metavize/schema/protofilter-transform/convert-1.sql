-- convert script for release 2.5

-----------------------------------
-- move old tables to new schemas |
-----------------------------------

-- com.untangle.tran.protofilter.ProtoFilterSettings
CREATE TABLE settings.tr_protofilter_settings
    AS SELECT * FROM public.tr_protofilter_settings;

ALTER TABLE settings.tr_protofilter_settings
    ADD CONSTRAINT tr_protofilter_settings_pkey
        PRIMARY KEY (settings_id);
ALTER TABLE settings.tr_protofilter_settings
    ADD CONSTRAINT tr_protofilter_settings_uk
        UNIQUE (tid);
ALTER TABLE settings.tr_protofilter_settings
    ALTER COLUMN settings_id SET NOT NULL;
ALTER TABLE settings.tr_protofilter_settings
    ALTER COLUMN tid SET NOT NULL;

-- com.untangle.tran.protofilter.ProtoFilterPattern
CREATE TABLE settings.tr_protofilter_pattern
    AS SELECT * FROM public.tr_protofilter_pattern;

ALTER TABLE settings.tr_protofilter_pattern
    ADD CONSTRAINT tr_protofilter_pattern_pkey
        PRIMARY KEY (rule_id);
ALTER TABLE settings.tr_protofilter_pattern
    ALTER COLUMN rule_id SET NOT NULL;

-- com.untangle.tran.protofilter.ProtoFilterLogEvent
CREATE TABLE events.tr_protofilter_evt
    AS SELECT * FROM public.tr_protofilter_evt;

ALTER TABLE events.tr_protofilter_evt
    ADD CONSTRAINT tr_protofilter_evt_pkey
        PRIMARY KEY (event_id);
ALTER TABLE events.tr_protofilter_evt
    ALTER COLUMN event_id SET NOT NULL;


-------------------------
-- recreate constraints |
-------------------------

-- indeces

CREATE INDEX tr_protofilter_sid_idx ON public.tr_protofilter_evt (session_id);

-- foreign key constraints

ALTER TABLE settings.tr_protofilter_settings
    ADD CONSTRAINT fk_tr_protofilter_settings
        FOREIGN KEY (tid) REFERENCES settings.tid;

ALTER TABLE settings.tr_protofilter_pattern
    ADD CONSTRAINT fk_tr_protofilter_pattern
        FOREIGN KEY (settings_id) REFERENCES settings.tr_protofilter_settings;

-------------------------
-- drop old constraints |
-------------------------

-- foreign key constraints

ALTER TABLE tr_protofilter_settings DROP CONSTRAINT fk55f095631446f;
ALTER TABLE tr_protofilter_pattern DROP CONSTRAINT fke929349b79192ab7;

--------------------
-- drop old tables |
--------------------

DROP TABLE public.tr_protofilter_settings;
DROP TABLE public.tr_protofilter_pattern;
DROP TABLE public.tr_protofilter_evt;

------------
-- analyze |
------------

ANALYZE events.tr_protofilter_evt;
