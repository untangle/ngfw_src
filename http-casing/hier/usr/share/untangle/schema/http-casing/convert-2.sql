-- convert script for release 1.4b
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

-- bad postinst in previous releases, cumululative conversion

-- drop foreign key constraints for logging

ALTER TABLE tr_http_evt_resp DROP CONSTRAINT FKC9BB12A21F20A4EB;
ALTER TABLE tr_http_evt_req DROP CONSTRAINT FK40505B6C1F20A4EB;
