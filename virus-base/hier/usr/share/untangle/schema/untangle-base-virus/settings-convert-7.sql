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

ALTER TABLE settings.tr_virus_settings RENAME TO n_virus_settings;
ALTER TABLE settings.tr_virus_vs_ext RENAME TO n_virus_vs_ext;
ALTER TABLE settings.tr_virus_config RENAME TO n_virus_config;
ALTER TABLE settings.tr_virus_smtp_config RENAME TO n_virus_smtp_config;
ALTER TABLE settings.tr_virus_pop_config RENAME TO n_virus_pop_config;
ALTER TABLE settings.tr_virus_imap_config RENAME TO n_virus_imap_config;
ALTER TABLE settings.tr_virus_vs_mt RENAME TO n_virus_vs_mt;
