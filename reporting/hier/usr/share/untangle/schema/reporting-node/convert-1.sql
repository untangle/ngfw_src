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
