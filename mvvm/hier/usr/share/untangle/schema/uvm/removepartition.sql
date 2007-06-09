-- XXX
SET search_path TO events,settings,public;

begin;

INSERT INTO :basetablename
  SELECT * FROM ONLY :tablename;

DROP TABLE :tablename;

commit;
