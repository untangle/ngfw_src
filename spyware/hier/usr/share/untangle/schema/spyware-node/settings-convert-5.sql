-- convert script for release 3.1
-- $HeadURL$
-- Copyright (c) 2003-2007 Untangle, Inc. 
--
-- This program is free software; you can redistribute it and/or modify
-- it under the terms of the GNU General Public License, version 2,
-- as published by the Free Software Foundation.
--
-- This program is distributed in the hope that it will be useful, but
-- AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
-- NONINFRINGEMENT.  See the GNU General Public License for more details.
--
-- You should have received a copy of the GNU General Public License
-- along with this program; if not, write to the Free Software
-- Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
--

-------------------------
-- disable subnet rules |
-------------------------

UPDATE ipmaddr_rule SET live = false WHERE EXISTS (SELECT rule_id FROM tr_spyware_sr WHERE ipmaddr_rule.rule_id = rule_id);

---------------------
-- No more varchars |
---------------------

-- tr_spyware_settings

DROP TABLE settings.tr_spyware_new;

CREATE TABLE settings.tr_spyware_new AS
    SELECT settings_id, tid, activex_enabled, cookie_enabled, spyware_enabled,
           block_all_activex, url_blacklist_enabled, activex_details::text,
           cookie_details::text, spyware_details::text,
           block_all_activex_details::text, url_blacklist_details::text,
           0 AS subnet_version, 0 AS activex_version, 0 AS cookie_version
    FROM settings.tr_spyware_settings;

DROP TABLE settings.tr_spyware_settings CASCADE;
ALTER TABLE settings.tr_spyware_new RENAME TO tr_spyware_settings;
ALTER TABLE settings.tr_spyware_settings ADD PRIMARY KEY (settings_id);
ALTER TABLE settings.tr_spyware_settings ALTER COLUMN settings_id SET NOT NULL;
ALTER TABLE settings.tr_spyware_settings ALTER COLUMN tid SET NOT NULL;
ALTER TABLE settings.tr_spyware_settings ALTER COLUMN subnet_version SET NOT NULL;
ALTER TABLE settings.tr_spyware_settings ALTER COLUMN activex_version SET NOT NULL;
ALTER TABLE settings.tr_spyware_settings ALTER COLUMN cookie_version SET NOT NULL;
ALTER TABLE settings.tr_spyware_settings
    ADD CONSTRAINT tr_spyware_settings_uk UNIQUE (tid);

-----------------------------
-- Link directly to pl_endp |
-----------------------------

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

ALTER TABLE tr_spyware_settings DROP CONSTRAINT tr_spyware_settings_uk;
ALTER TABLE tr_spyware_settings ADD CONSTRAINT tr_spyware_settings_tid_key UNIQUE (tid);
