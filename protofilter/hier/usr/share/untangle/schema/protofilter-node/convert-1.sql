-- convert script for release 2.5
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
