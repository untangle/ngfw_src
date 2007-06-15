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

CREATE TABLE day_name_date (
        day_name text NOT NULL,
        day_begin date NOT NULL);

DROP TABLE :outtable;

CREATE TABLE :outtable (
        day_name text NOT NULL,
        day_begin date NOT NULL);

-- Note that loop iteration step is -1.
-- args: start-days, end-days, base-date
CREATE OR REPLACE FUNCTION make_dates(INTEGER, INTEGER, DATE)
  RETURNS SETOF day_name_date
  LANGUAGE 'plpgsql'
  AS '
    DECLARE
      rec day_name_date%ROWTYPE;
    BEGIN
      FOR index IN REVERSE $1..$2 LOOP
        SELECT \'\', $3 + index INTO rec;
        RETURN NEXT rec;
      END LOOP;
      RETURN;
    END
  ';

-- Yucky, but these three together get the right output.
INSERT INTO :outtable
   SELECT * FROM make_dates(-1, -:daystosave, (:midnight)::date);
DELETE FROM :outtable 
  WHERE EXISTS
      (SELECT 1 from :intable intable WHERE :outtable.day_begin = intable.day_begin);
UPDATE :outtable
  SET day_name = overlay(overlay(day_begin::text placing '_' from 5) placing '_' from 8);
