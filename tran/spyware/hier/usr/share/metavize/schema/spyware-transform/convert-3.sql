-- convert script for release 2.5

-----------------------------------
-- move old tables to new schemas |
-----------------------------------

-- com.untangle.tran.spyware.SpywareSettings
-- (adding url_blacklist_enabled and url_blacklist_details)
CREATE TABLE settings.tr_spyware_settings (
    settings_id,
    tid,
    activex_enabled,
    cookie_enabled,
    spyware_enabled,
    block_all_activex,
    url_blacklist_enabled,
    activex_details,
    cookie_details,
    spyware_details,
    block_all_activex_details,
    url_blacklist_details)
AS SELECT settings_id, tid, activex_enabled, cookie_enabled, spyware_enabled,
         block_all_activex, true, activex_details, cookie_details,
         spyware_details, block_all_activex_details,
         'no details'::varchar(255)
   FROM public.tr_spyware_settings;

ALTER TABLE settings.tr_spyware_settings
    ADD CONSTRAINT tr_spyware_settings_pkey PRIMARY KEY (settings_id);
ALTER TABLE settings.tr_spyware_settings
    ADD CONSTRAINT tr_spyware_settings_uk UNIQUE (tid);
ALTER TABLE settings.tr_spyware_settings
    ALTER COLUMN settings_id SET NOT NULL;
ALTER TABLE settings.tr_spyware_settings
    ALTER COLUMN tid SET NOT NULL;

-- com.untangle.tran.spyware.SpywareSettings.cookieRules
CREATE TABLE settings.tr_spyware_cr AS SELECT * FROM public.tr_spyware_cr;

ALTER TABLE settings.tr_spyware_cr
    ADD CONSTRAINT tr_spyware_cr_pkey PRIMARY KEY (setting_id, position);
ALTER TABLE settings.tr_spyware_cr
    ALTER COLUMN setting_id SET NOT NULL;
ALTER TABLE settings.tr_spyware_cr
    ALTER COLUMN rule_id SET NOT NULL;
ALTER TABLE settings.tr_spyware_cr
    ALTER COLUMN position SET NOT NULL;

-- com.untangle.tran.spyware.SpywareSettings.activeXRules
CREATE TABLE settings.tr_spyware_ar AS SELECT * FROM public.tr_spyware_ar;

ALTER TABLE settings.tr_spyware_ar
    ADD CONSTRAINT tr_spyware_ar_pkey
    PRIMARY KEY (setting_id, position);
ALTER TABLE settings.tr_spyware_ar
    ALTER COLUMN setting_id SET NOT NULL;
ALTER TABLE settings.tr_spyware_ar
    ALTER COLUMN rule_id SET NOT NULL;
ALTER TABLE settings.tr_spyware_ar
    ALTER COLUMN position SET NOT NULL;

-- com.untangle.tran.spyware.SpywareSettings.subnetRules
CREATE TABLE settings.tr_spyware_sr AS SELECT * FROM public.tr_spyware_sr;

ALTER TABLE settings.tr_spyware_sr
    ADD CONSTRAINT tr_spyware_sr_pkey
    PRIMARY KEY (settings_id, position);
ALTER TABLE settings.tr_spyware_sr
    ALTER COLUMN settings_id SET NOT NULL;
ALTER TABLE settings.tr_spyware_sr
    ALTER COLUMN rule_id SET NOT NULL;
ALTER TABLE settings.tr_spyware_sr
    ALTER COLUMN position SET NOT NULL;

-----------
-- events |
-----------

-- com.untangle.tran.spyware.SpywareAccessEvent
CREATE TABLE events.tr_spyware_evt_access
    AS SELECT * FROM public.tr_spyware_evt_access;

ALTER TABLE events.tr_spyware_evt_access
    ADD CONSTRAINT tr_spyware_evt_access_pkey PRIMARY KEY (event_id);
ALTER TABLE events.tr_spyware_evt_access
    ALTER COLUMN event_id SET NOT NULL;

-- com.untangle.tran.spyware.SpywareActiveXEvent
CREATE TABLE events.tr_spyware_evt_activex
    AS SELECT * FROM public.tr_spyware_evt_activex;

ALTER TABLE events.tr_spyware_evt_activex
    ADD CONSTRAINT tr_spyware_evt_activex_pkey PRIMARY KEY (event_id);
ALTER TABLE events.tr_spyware_evt_activex
    ALTER COLUMN event_id SET NOT NULL;


-- com.untangle.tran.spyware.SpywareCookieEvent
CREATE TABLE events.tr_spyware_evt_cookie
    AS SELECT * FROM public.tr_spyware_evt_cookie;

ALTER TABLE events.tr_spyware_evt_cookie
    ADD CONSTRAINT tr_spyware_evt_cookie_pkey PRIMARY KEY (event_id);
ALTER TABLE events.tr_spyware_evt_cookie
    ALTER COLUMN event_id SET NOT NULL;

-------------------------
-- recreate constraints |
-------------------------

-- foreign key constraints

ALTER TABLE settings.tr_spyware_ar
    ADD CONSTRAINT fk_tr_spyware_ar
    FOREIGN KEY (setting_id) REFERENCES settings.tr_spyware_settings;

ALTER TABLE settings.tr_spyware_ar
    ADD CONSTRAINT fk_tr_spyware_ar_rule
    FOREIGN KEY (rule_id) REFERENCES settings.string_rule;

ALTER TABLE settings.tr_spyware_settings
    ADD CONSTRAINT fk_tr_spyware_settings
    FOREIGN KEY (tid) REFERENCES settings.tid;

ALTER TABLE settings.tr_spyware_cr
    ADD CONSTRAINT fk_tr_spyware_cr
    FOREIGN KEY (setting_id) REFERENCES settings.tr_spyware_settings;

ALTER TABLE settings.tr_spyware_cr
    ADD CONSTRAINT fk_tr_spyware_cr_rule
    FOREIGN KEY (rule_id) REFERENCES settings.string_rule;

ALTER TABLE settings.tr_spyware_sr
    ADD CONSTRAINT fk_tr_spyware_sr
    FOREIGN KEY (settings_id) REFERENCES settings.tr_spyware_settings;

ALTER TABLE settings.tr_spyware_sr
    ADD CONSTRAINT fk_tr_spyware_sr_rule
    FOREIGN KEY (rule_id) REFERENCES settings.ipmaddr_rule;

-------------------------
-- drop old constraints |
-------------------------

-- foreign key constraints

ALTER TABLE tr_spyware_ar DROP CONSTRAINT fkf0bdc78871aad3e;
ALTER TABLE tr_spyware_ar DROP CONSTRAINT fkf0bdc781cae658a;
ALTER TABLE tr_spyware_settings DROP CONSTRAINT fk33dfef2a1446f;
ALTER TABLE tr_spyware_cr DROP CONSTRAINT fkf0bdcb61cae658a;
ALTER TABLE tr_spyware_cr DROP CONSTRAINT fkf0bdcb6871aad3e;
ALTER TABLE tr_spyware_sr DROP CONSTRAINT fkf0bdea6871aad3e;
ALTER TABLE tr_spyware_sr DROP CONSTRAINT fkf0bdea679192ab7;

--------------------
-- drop old tables |
--------------------

DROP TABLE public.tr_spyware_evt_activex;
DROP TABLE public.tr_spyware_settings;
DROP TABLE public.tr_spyware_cr;
DROP TABLE public.tr_spyware_evt_access;
DROP TABLE public.tr_spyware_ar;
DROP TABLE public.tr_spyware_sr;
DROP TABLE public.tr_spyware_evt_cookie;

---------------
-- new tables |
---------------

-- com.untangle.tran.spyware.SpywareBlacklistEvent
CREATE TABLE events.tr_spyware_evt_blacklist (
    event_id int8 NOT NULL,
    session_id int4,
    request_id int8,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- indeces for reporting

CREATE INDEX tr_spyware_cookie_rid_idx
    ON events.tr_spyware_evt_cookie (request_id);
CREATE INDEX tr_spyware_bl_rid_idx
    ON events.tr_spyware_evt_blacklist (request_id);
CREATE INDEX tr_spyware_ax_rid_idx
    ON events.tr_spyware_evt_activex (request_id);
CREATE INDEX tr_spyware_acc_sid_idx
    ON events.tr_spyware_evt_access (session_id);

------------
-- analyze |
------------

ANALYZE events.tr_spyware_evt_access;
ANALYZE events.tr_spyware_evt_activex;
ANALYZE events.tr_spyware_evt_cookie;
