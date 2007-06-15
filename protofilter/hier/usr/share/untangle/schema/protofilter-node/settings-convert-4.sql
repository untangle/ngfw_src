-- settings convert for release 3.2
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

DROP TABLE settings.tr_protofilter_pattern_tmp;

CREATE TABLE settings.tr_protofilter_pattern_tmp AS
    SELECT rule_id, -27 as metavize_id, protocol::text, description::text, category::text,
           definition::text, quality::text, blocked, alert, log, settings_id, position
    FROM settings.tr_protofilter_pattern;

DROP TABLE settings.tr_protofilter_pattern;

ALTER TABLE settings.tr_protofilter_pattern_tmp RENAME TO tr_protofilter_pattern;
ALTER TABLE settings.tr_protofilter_pattern ADD PRIMARY KEY (rule_id);
ALTER TABLE settings.tr_protofilter_pattern ALTER COLUMN rule_id SET NOT NULL;

ALTER TABLE settings.tr_protofilter_pattern
    ADD CONSTRAINT fk_tr_protofilter_pattern
        FOREIGN KEY (settings_id) REFERENCES settings.tr_protofilter_settings;
