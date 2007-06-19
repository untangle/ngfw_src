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

-- XXX
SET search_path TO events,settings,public;

DROP TABLE :tablename;
CREATE TABLE :tablename (
    CHECK (time_stamp >= TIMESTAMP :daybegin AND time_stamp < TIMESTAMP :dayend)
) INHERITS (:basetablename);

INSERT INTO :tablename
  SELECT *
    FROM ONLY :basetablename
   WHERE time_stamp >= TIMESTAMP :daybegin AND time_stamp < TIMESTAMP :dayend;

DELETE FROM ONLY :basetablename
   WHERE time_stamp >= TIMESTAMP :daybegin AND time_stamp < TIMESTAMP :dayend;

-- Indices are now created automatically later...
-- CREATE INDEX reqid_idx_:tablename ON :tablename (event_id);
-- CREATE INDEX ts_idx_:tablename ON :tablename (time_stamp);
