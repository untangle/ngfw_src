DROP TABLE :outtable;

CREATE TABLE :outtable (
        day_name text NOT NULL,
        day_begin date NOT NULL);

-- Yucky, but these three together get the right output.
INSERT INTO :outtable
    SELECT '' as day_name, (:midnight)::date + s.a as day_begin
      FROM generate_series(-1, -:daystosave, -1) AS s(a);
DELETE FROM :outtable 
  WHERE EXISTS
      (SELECT 1 from :intable intable WHERE :outtable.day_begin = intable.day_begin);
UPDATE :outtable
  SET day_name = overlay(overlay(day_begin::text placing '_' from 5) placing '_' from 8);
