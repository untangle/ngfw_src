-- convert script for release 2.5

-----------------------------------
-- move old tables to new schemas |
-----------------------------------

-- com.untangle.tran.airgap.AirgapSettings
CREATE TABLE settings.tr_airgap_settings
    AS SELECT * FROM public.tr_airgap_settings;

ALTER TABLE settings.tr_airgap_settings
    ADD CONSTRAINT tr_airgap_settings_pkey PRIMARY KEY (id);
ALTER TABLE settings.tr_airgap_settings
    ADD CONSTRAINT tr_airgap_settings_uk UNIQUE (tid);
ALTER TABLE settings.tr_airgap_settings
    ALTER COLUMN id SET NOT NULL;
ALTER TABLE settings.tr_airgap_settings
    ALTER COLUMN tid SET NOT NULL;

-------------------------
-- recreate constraints |
-------------------------

-- foreign key constraints

ALTER TABLE settings.tr_airgap_settings
    ADD CONSTRAINT fk_tr_airgap_settings FOREIGN KEY (tid) REFERENCES tid;

-------------------------
-- drop old constraints |
-------------------------

-- foreign key constraints

ALTER TABLE tr_airgap_settings DROP CONSTRAINT fk7b2ca9f51446f;

--------------------
-- drop old tables |
--------------------

DROP TABLE public.tr_airgap_settings;