-- convert script for release 3.1

---------------------
-- No more varchars |
---------------------

-- tr_spyware_settings

DROP TABLE settings.tr_spyware_new;

CREATE TABLE settings.tr_spyware_new AS
    SELECT settings_id, tid, activex_enabled, cookie_enabled, spyware_enabled,
           block_all_activex, url_blacklist_enabled, activex_details::text,
           cookie_details::text, spyware_details::text,
           block_all_activex_details::text, url_blacklist_details::text
    FROM settings.tr_spyware_settings;

DROP TABLE settings.tr_spyware_settings CASCADE;
ALTER TABLE settings.tr_spyware_new RENAME TO tr_spyware_settings;
ALTER TABLE settings.tr_spyware_settings ADD PRIMARY KEY (settings_id);
ALTER TABLE settings.tr_spyware_settings ALTER COLUMN settings_id SET NOT NULL;
ALTER TABLE settings.tr_spyware_settings ALTER COLUMN tid SET NOT NULL;
ALTER TABLE settings.tr_spyware_settings
    ADD CONSTRAINT tr_spyware_settings_uk UNIQUE (tid);

-----------------------------
-- Link directly to pl_endp |
-----------------------------

-- fix events.tr_spyware_evt_access

DROP TABLE events.tr_spyware_new;

CREATE TABLE events.tr_spyware_new AS
    SELECT evt.event_id, pl_endp.event_id AS pl_endp_id, request_id, ipmaddr,
           ident::text, blocked, evt.time_stamp
    FROM events.tr_spyware_evt_access evt JOIN pl_endp USING (session_id);

DROP TABLE events.tr_spyware_evt_access;
ALTER TABLE events.tr_spyware_new RENAME TO tr_spyware_evt_access;
ALTER TABLE events.tr_spyware_evt_access ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE events.tr_spyware_evt_access ADD PRIMARY KEY (event_id);

-- fix events.tr_spyware_evt_activex

DROP TABLE events.tr_spyware_new;

CREATE TABLE events.tr_spyware_new AS
    SELECT evt.event_id, pl_endp.event_id AS pl_endp_id, request_id,
           ident::text, evt.time_stamp
    FROM events.tr_spyware_evt_activex evt JOIN pl_endp USING (session_id);

DROP TABLE events.tr_spyware_evt_activex;
ALTER TABLE events.tr_spyware_new RENAME TO tr_spyware_evt_activex;
ALTER TABLE events.tr_spyware_evt_activex ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE events.tr_spyware_evt_activex ADD PRIMARY KEY (event_id);

-- fix events.tr_spyware_evt_cookie

DROP TABLE events.tr_spyware_new;

CREATE TABLE events.tr_spyware_new AS
    SELECT evt.event_id, pl_endp.event_id AS pl_endp_id, request_id,
           ident::text, to_server, evt.time_stamp
    FROM events.tr_spyware_evt_cookie evt JOIN pl_endp USING (session_id);

DROP TABLE events.tr_spyware_evt_cookie;
ALTER TABLE events.tr_spyware_new RENAME TO tr_spyware_evt_cookie;
ALTER TABLE events.tr_spyware_evt_cookie ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE events.tr_spyware_evt_cookie ADD PRIMARY KEY (event_id);

-- fix events.tr_spyware_evt_blacklist

DROP TABLE events.tr_spyware_new;

CREATE TABLE events.tr_spyware_new AS
    SELECT evt.event_id, pl_endp.event_id AS pl_endp_id, request_id,
           evt.time_stamp
    FROM events.tr_spyware_evt_blacklist evt JOIN pl_endp USING (session_id);

DROP TABLE events.tr_spyware_evt_blacklist;
ALTER TABLE events.tr_spyware_new RENAME TO tr_spyware_evt_blacklist;
ALTER TABLE events.tr_spyware_evt_blacklist ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE events.tr_spyware_evt_blacklist ADD PRIMARY KEY (event_id);

-- recreate indexes

CREATE INDEX tr_spyware_cookie_rid_idx
    ON events.tr_spyware_evt_cookie (request_id);
CREATE INDEX tr_spyware_bl_rid_idx
    ON events.tr_spyware_evt_blacklist (request_id);
CREATE INDEX tr_spyware_ax_rid_idx
    ON events.tr_spyware_evt_activex (request_id);
CREATE INDEX tr_spyware_acc_sid_idx
    ON events.tr_spyware_evt_access (pl_endp_id);

-- recreate constraints

ALTER TABLE settings.tr_spyware_ar
    ADD CONSTRAINT fk_tr_spyware_ar
    FOREIGN KEY (settings_id) REFERENCES settings.tr_spyware_settings;

ALTER TABLE settings.tr_spyware_settings
    ADD CONSTRAINT fk_tr_spyware_settings
    FOREIGN KEY (tid) REFERENCES settings.tid;

ALTER TABLE settings.tr_spyware_cr
    ADD CONSTRAINT fk_tr_spyware_cr
    FOREIGN KEY (settings_id) REFERENCES settings.tr_spyware_settings;

ALTER TABLE settings.tr_spyware_sr
    ADD CONSTRAINT fk_tr_spyware_sr
    FOREIGN KEY (settings_id) REFERENCES settings.tr_spyware_settings;

-- may be missing due to typo

ALTER TABLE settings.tr_spyware_cr
    ADD CONSTRAINT fk_tr_spyware_cr_rule
    FOREIGN KEY (rule_id) REFERENCES settings.string_rule;

-- indices for reporting

DROP INDEX tr_spyware_acc_sid_idx;
CREATE INDEX tr_spyware_acc_plepid_idx
    ON events.tr_spyware_evt_access (pl_endp_id);
