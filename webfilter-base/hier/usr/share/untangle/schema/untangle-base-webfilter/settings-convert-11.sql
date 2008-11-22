-- settings conversion for release-5.0
-- $HeadURL: svn://chef/branch/prod/web-ui/work/src/webfilter/hier/usr/share/untangle/schema/untangle-base-webfilter/settings-convert-8.sql $
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

ALTER TABLE settings.n_webfilter_settings ADD COLUMN enable_https bool;
UPDATE settings.n_webfilter_settings SET enable_https = true;
ALTER TABLE settings.n_webfilter_settings ALTER COLUMN enable_https SET NOT NULL;

INSERT INTO settings.n_webfilter_blcat
       (category_id, name, display_name,
        description, block, log, setting_id)
       SELECT nextval('hibernate_sequence'), 'Uncategorized', 'Uncategorized',
       'Uncategorized', true, true, settings_id
       FROM settings.n_webfilter_settings
       WHERE fascist_mode = true;

UPDATE settings.n_webfilter_settings
SET block_all_ip_hosts = true
WHERE fascist_mode = true;

UPDATE settings.n_webfilter_blcat SET block = true, log = true
WHERE setting_id IN (SELECT settings_id
                     FROM settings.n_webfilter_settings
                     WHERE fascist_mode = true);

ALTER TABLE settings.n_webfilter_settings DROP COLUMN fascist_mode;
