-- schema for release-2.5
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

-------------
-- settings |
-------------

-- com.untangle.tran.test.TestSettings
CREATE TABLE settings.n_test_settings (
    id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    mode varchar(255),
    buffered bool,
    normal bool,
    release bool,
    quiet bool,
    min_random_buffer_size int4,
    max_random_buffer_size int4,
    PRIMARY KEY (ID));

----------------
-- constraints |
----------------

-- foreign key constraints

ALTER TABLE settings.n_test_settings
    ADD CONSTRAINT fk_tr_test_settings
    FOREIGN KEY (tid) REFERENCES settings.u_tid;

