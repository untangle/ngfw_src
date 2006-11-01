-- convert script for release 2.5

-----------------------------------
-- move old tables to new schemas |
-----------------------------------

-- com.untangle.tran.test.TestSettings
CREATE TABLE settings.tr_test_settings
    AS SELECT * FROM public.tr_test_settings;

ALTER TABLE settings.tr_test_settings
    ADD CONSTRAINT tr_test_settings_pkey PRIMARY KEY (id);
ALTER TABLE settings.tr_test_settings
    ADD CONSTRAINT tr_test_settings_uk PRIMARY KEY (tid);
ALTER TABLE settings.tr_test_settings
    ALTER COLUMN id SET NOT NULL;
ALTER TABLE settings.tr_test_settings
    ALTER COLUMN tid SET NOT NULL;

-------------------------
-- recreate constraints |
-------------------------

-- foreign key constraints

ALTER TABLE settings.tr_test_settings
    ADD CONSTRAINT fk_tr_test_settings
    FOREIGN KEY (tid) REFERENCES settings.tid;

-------------------------
-- drop old constraints |
-------------------------

-- foreign key constraints

ALTER TABLE public.tr_test_settings DROP CONSTRAINT fkeb1f4d2f1446f;

--------------------
-- drop old tables |
--------------------

DROP TABLE public.tr_test_settings;
