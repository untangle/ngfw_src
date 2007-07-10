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

ALTER TABLE settings.tr_spyware_settings RENAME TO n_spyware_settings;
ALTER TABLE settings.tr_spyware_cr RENAME TO n_spyware_cr;
ALTER TABLE settings.tr_spyware_ar RENAME TO n_spyware_ar;
ALTER TABLE settings.tr_spyware_sr RENAME TO n_spyware_sr;
ALTER TABLE settings.tr_spyware_wl RENAME TO n_spyware_wl;

ALTER TABLE settings.idx_spyware_rule_ar RENAME TO n_spyware_ar_rule_idx;
ALTER TABLE settings.idx_spyware_rule_cr RENAME TO n_spyware_cr_rule_idx;
ALTER TABLE settings.idx_spyware_rule_sr RENAME TO n_spyware_sr_rule_idx;
