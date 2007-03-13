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
