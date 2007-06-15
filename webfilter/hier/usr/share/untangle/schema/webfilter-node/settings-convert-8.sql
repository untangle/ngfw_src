-- settings conversion for release-5.0
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

ALTER TABLE settings.tr_httpblk_template RENAME TO n_webfilter_template;
ALTER TABLE settings.tr_httpblk_passed_urls RENAME TO n_webfilter_passed_urls;
ALTER TABLE settings.tr_httpblk_settings RENAME TO n_webfilter_settings;
ALTER TABLE settings.tr_httpblk_extensions RENAME TO n_webfilter_extensions;
ALTER TABLE settings.tr_httpblk_mime_types RENAME TO n_webfilter_mime_types;
ALTER TABLE settings.tr_httpblk_passed_clients RENAME TO n_webfilter_passed_clients;
ALTER TABLE settings.tr_httpblk_blocked_urls RENAME TO n_webfilter_blocked_urls;
ALTER TABLE settings.tr_httpblk_blcat RENAME TO n_webfilter_blcat;
