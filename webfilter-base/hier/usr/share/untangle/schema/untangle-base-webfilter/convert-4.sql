-- convert script for release 1.5
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

-----------------------------------
-- move old tables to new schemas |
-----------------------------------

-- com.untangle.tran.httpblocker.BlockTemplate
CREATE TABLE settings.tr_httpblk_template
    AS SELECT * FROM public.tr_httpblk_template;

ALTER TABLE settings.tr_httpblk_template
    ADD CONSTRAINT tr_httpblk_template_pkey PRIMARY KEY (message_id);
ALTER TABLE settings.tr_httpblk_template
    ALTER COLUMN message_id SET NOT NULL;

-- com.untangle.tran.httpblocker.HttpBlockerSettings.blockedUrls
CREATE TABLE settings.tr_httpblk_passed_urls
    AS SELECT * from public.tr_httpblk_passed_urls;

ALTER TABLE settings.tr_httpblk_passed_urls
    ADD CONSTRAINT tr_httpblk_passed_urls_pkey
        PRIMARY KEY (setting_id, position);
ALTER TABLE settings.tr_httpblk_passed_urls
    ALTER COLUMN setting_id SET NOT NULL;
ALTER TABLE settings.tr_httpblk_passed_urls
    ALTER COLUMN rule_id SET NOT NULL;
ALTER TABLE settings.tr_httpblk_passed_urls
    ALTER COLUMN position SET NOT NULL;

-- com.untangle.tran.httpblocker.HttpBlockerSettings
CREATE TABLE settings.tr_httpblk_settings
    AS SELECT * FROM public.tr_httpblk_settings;

ALTER TABLE settings.tr_httpblk_settings
    ADD CONSTRAINT tr_httpblk_settings_pkey PRIMARY KEY (settings_id);
ALTER TABLE settings.tr_httpblk_settings
    ADD CONSTRAINT tr_httpblk_settings_uk UNIQUE (tid);
ALTER TABLE settings.tr_httpblk_settings
    ALTER COLUMN settings_id SET NOT NULL;
ALTER TABLE settings.tr_httpblk_settings
    ALTER COLUMN tid SET NOT NULL;
ALTER TABLE settings.tr_httpblk_settings
    ALTER COLUMN template SET NOT NULL;
ALTER TABLE settings.tr_httpblk_settings
    ALTER COLUMN block_all_ip_hosts SET NOT NULL;

-- com.untangle.tran.httpblocker.HttpBlockerSettings.blockedExtensions
CREATE TABLE settings.tr_httpblk_extensions
    AS SELECT * FROM public.tr_httpblk_extensions;

ALTER TABLE settings.tr_httpblk_extensions
    ADD CONSTRAINT tr_httpblk_extensions_pkey
        PRIMARY KEY (setting_id, position);
ALTER TABLE settings.tr_httpblk_extensions
    ALTER COLUMN setting_id SET NOT NULL;
ALTER TABLE settings.tr_httpblk_extensions
    ALTER COLUMN rule_id SET NOT NULL;
ALTER TABLE settings.tr_httpblk_extensions
    ALTER COLUMN position SET NOT NULL;

-- com.untangle.tran.httpblocker.HttpBlockerSettings.blockedMimeTypes
CREATE TABLE settings.tr_httpblk_mime_types
    AS SELECT * FROM public.tr_httpblk_mime_types;

ALTER TABLE settings.tr_httpblk_mime_types
    ADD CONSTRAINT tr_httpblk_mime_types_pkey
        PRIMARY KEY (setting_id, position);
ALTER TABLE settings.tr_httpblk_mime_types
    ALTER COLUMN setting_id SET NOT NULL;
ALTER TABLE settings.tr_httpblk_mime_types
    ALTER COLUMN rule_id SET NOT NULL;
ALTER TABLE settings.tr_httpblk_mime_types
    ALTER COLUMN position SET NOT NULL;

-- com.untangle.tran.httpblocker.HttpBlockerSettings.passedClients
CREATE TABLE settings.tr_httpblk_passed_clients
    AS SELECT * FROM public.tr_httpblk_passed_clients;

ALTER TABLE settings.tr_httpblk_passed_clients
    ADD CONSTRAINT tr_httpblk_passed_clients_pkey
        PRIMARY KEY (setting_id, position);
ALTER TABLE settings.tr_httpblk_passed_clients
    ALTER COLUMN setting_id SET NOT NULL;
ALTER TABLE settings.tr_httpblk_passed_clients
    ALTER COLUMN rule_id SET NOT NULL;
ALTER TABLE settings.tr_httpblk_passed_clients
    ALTER COLUMN position SET NOT NULL;

-- com.untangle.tran.httpblocker.HttpBlockerSettings.blockedUrls
CREATE TABLE settings.tr_httpblk_blocked_urls
    AS SELECT * FROM public.tr_httpblk_blocked_urls;

ALTER TABLE settings.tr_httpblk_blocked_urls
    ADD CONSTRAINT tr_httpblk_blocked_urls_pkey
        PRIMARY KEY (setting_id, position);
ALTER TABLE settings.tr_httpblk_blocked_urls
    ALTER COLUMN setting_id SET NOT NULL;
ALTER TABLE settings.tr_httpblk_blocked_urls
    ALTER COLUMN rule_id SET NOT NULL;
ALTER TABLE settings.tr_httpblk_blocked_urls
    ALTER COLUMN position SET NOT NULL;

-- com.untangle.tran.httpblocker.Category
CREATE TABLE settings.tr_httpblk_blcat
    AS SELECT * FROM public.tr_httpblk_blcat;

ALTER TABLE settings.tr_httpblk_blcat
    ADD CONSTRAINT tr_httpblk_blcat_pkey PRIMARY KEY (category_id);
ALTER TABLE settings.tr_httpblk_blcat
    ALTER COLUMN category_id SET NOT NULL;

-- com.untangle.tran.httpblocker.HttpBlockerEvent

CREATE TABLE events.tr_httpblk_evt_blk (
    event_id,
    request_id,
    action,
    reason,
    category,
    time_stamp)
AS SELECT event_id, request_id, 'B'::char(1), reason, category, time_stamp
   FROM public.tr_httpblk_evt_blk;

ALTER TABLE events.tr_httpblk_evt_blk
    ADD CONSTRAINT tr_httpblk_evt_blk_pkey PRIMARY KEY (event_id);
ALTER TABLE events.tr_httpblk_evt_blk
    ALTER COLUMN event_id SET NOT NULL;

-------------------------
-- recreate constraints |
-------------------------

ALTER TABLE settings.tr_httpblk_passed_urls
    ADD CONSTRAINT fk_tr_httpblk_passed_urls
    FOREIGN KEY (setting_id) REFERENCES settings.tr_httpblk_settings;

ALTER TABLE settings.tr_httpblk_passed_urls
    ADD CONSTRAINT fk_tr_httpblk_passed_urls_rule
    FOREIGN KEY (rule_id) REFERENCES settings.string_rule;

ALTER TABLE settings.tr_httpblk_settings
    ADD CONSTRAINT fk_tr_httpblk_settings_template
    FOREIGN KEY (template) REFERENCES settings.tr_httpblk_template;

ALTER TABLE settings.tr_httpblk_settings
    ADD CONSTRAINT fk_tr_httpblk_settings
    FOREIGN KEY (tid) REFERENCES settings.tid;

ALTER TABLE settings.tr_httpblk_extensions
    ADD CONSTRAINT fk_tr_httpblk_extensions
    FOREIGN KEY (setting_id) REFERENCES settings.tr_httpblk_settings;

ALTER TABLE settings.tr_httpblk_extensions
    ADD CONSTRAINT fk_tr_httpblk_extensions_rule
    FOREIGN KEY (rule_id) REFERENCES settings.string_rule;

ALTER TABLE settings.tr_httpblk_mime_types
    ADD CONSTRAINT fk_tr_httpblk_mime_types
    FOREIGN KEY (setting_id) REFERENCES settings.tr_httpblk_settings;

ALTER TABLE settings.tr_httpblk_mime_types
    ADD CONSTRAINT fk_tr_httpblk_mime_types_rule
    FOREIGN KEY (rule_id) REFERENCES settings.mimetype_rule;

ALTER TABLE settings.tr_httpblk_passed_clients
    ADD CONSTRAINT fk_tr_httpblk_passed_clients
    FOREIGN KEY (setting_id) REFERENCES settings.tr_httpblk_settings;

ALTER TABLE settings.tr_httpblk_passed_clients
    ADD CONSTRAINT fk_tr_httpblk_passed_clients_rule
    FOREIGN KEY (rule_id) REFERENCES settings.ipmaddr_rule;

ALTER TABLE settings.tr_httpblk_blocked_urls
    ADD CONSTRAINT fk_tr_httpblk_blocked_urls
    FOREIGN KEY (setting_id) REFERENCES settings.tr_httpblk_settings;

ALTER TABLE settings.tr_httpblk_blocked_urls
    ADD CONSTRAINT fk_tr_httpblk_blocked_urls_rule
    FOREIGN KEY (rule_id) REFERENCES settings.string_rule;

ALTER TABLE settings.tr_httpblk_blcat
    ADD CONSTRAINT fk_tr_httpblk_blcat
    FOREIGN KEY (setting_id) REFERENCES settings.tr_httpblk_settings;

-------------------------
-- drop old constraints |
-------------------------

ALTER TABLE tr_httpblk_passed_urls DROP CONSTRAINT FK6C8C0C8C1CAE658A;
ALTER TABLE tr_httpblk_passed_urls DROP CONSTRAINT FK6C8C0C8C871AAD3E;
ALTER TABLE tr_httpblk_settings DROP CONSTRAINT FK3F2D0D8ADFE0BC7A;
ALTER TABLE tr_httpblk_settings DROP CONSTRAINT FK3F2D0D8A1446F;
ALTER TABLE tr_httpblk_extensions DROP CONSTRAINT FKBC81FBBB871AAD3E;
ALTER TABLE tr_httpblk_extensions DROP CONSTRAINT FKBC81FBBB1CAE658A;
ALTER TABLE tr_httpblk_mime_types DROP CONSTRAINT FKF4BA8C351CAE658A;
ALTER TABLE tr_httpblk_mime_types DROP CONSTRAINT FKF4BA8C35871AAD3E;
ALTER TABLE tr_httpblk_passed_clients DROP CONSTRAINT FKFB0B65401CAE658A;
ALTER TABLE tr_httpblk_passed_clients DROP CONSTRAINT FKFB0B6540871AAD3E;
ALTER TABLE tr_httpblk_blocked_urls DROP CONSTRAINT FK804E415E871AAD3E;
ALTER TABLE tr_httpblk_blocked_urls DROP CONSTRAINT FK804E415E1CAE658A;
ALTER TABLE tr_httpblk_blcat DROP CONSTRAINT FKA0680F251CAE658A;

--------------------
-- drop old tables |
--------------------

DROP TABLE public.tr_httpblk_template;
DROP TABLE public.tr_httpblk_passed_urls;
DROP TABLE public.tr_httpblk_settings;
DROP TABLE public.tr_httpblk_extensions;
DROP TABLE public.tr_httpblk_evt_blk;
DROP TABLE public.tr_httpblk_mime_types;
DROP TABLE public.tr_httpblk_passed_clients;
DROP TABLE public.tr_httpblk_blocked_urls;
DROP TABLE public.tr_httpblk_blcat;

------------
-- analyze |
------------

ANALYZE events.tr_httpblk_evt_blk;
