-- convert script for release 2.5

-----------------------------------
-- move old tables to new schemas |
-----------------------------------

-- com.untangle.tran.reporting.ReportingSettings
CREATE TABLE settings.tr_reporting_settings
    AS SELECT * FROM public.tr_reporting_settings;

ALTER TABLE settings.tr_reporting_settings
    ADD CONSTRAINT tr_reporting_settings_pkey PRIMARY KEY (id);
ALTER TABLE settings.tr_reporting_settings
    ADD CONSTRAINT tr_reporting_settings_uk UNIQUE (tid);
ALTER TABLE settings.tr_reporting_settings
    ALTER COLUMN id SET NOT NULL;
ALTER TABLE settings.tr_reporting_settings
    ALTER COLUMN tid SET NOT NULL;
ALTER TABLE settings.tr_reporting_settings
    ALTER COLUMN network_directory SET NOT NULL;

-------------------------
-- recreate constraints |
-------------------------

-- foreign key constraints

ALTER TABLE settings.tr_reporting_settings
    ADD CONSTRAINT fk_tr_reporting_settings
    FOREIGN KEY (network_directory) REFERENCES settings.ipmaddr_dir;

ALTER TABLE tr_reporting_settings
    ADD CONSTRAINT fk_tr_reporting_settings
    FOREIGN KEY (tid) REFERENCES tid;

-------------------------
-- drop old constraints |
-------------------------

-- foreign key constraints

ALTER TABLE tr_reporting_settings DROP CONSTRAINT fk85b769562c0555c;
ALTER TABLE tr_reporting_settings DROP CONSTRAINT fk85b76951446f;

--------------------
-- drop old tables |
--------------------

DROP TABLE public.tr_reporting_settings;
