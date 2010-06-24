-- Copyright (c) 2003-2008 Untangle, Inc.
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

ALTER TABLE settings.u_user_policy_rule
  ADD COLUMN start_time_string text;

ALTER TABLE settings.u_user_policy_rule
  ADD COLUMN end_time_string text;

update u_user_policy_rule set start_time_string = substring(cast(start_time as text) from '..:..');
update u_user_policy_rule set end_time_string = substring(cast(end_time as text) from '..:..');

ALTER TABLE settings.u_user_policy_rule
  DROP COLUMN start_time;

ALTER TABLE settings.u_user_policy_rule
  DROP COLUMN end_time;
