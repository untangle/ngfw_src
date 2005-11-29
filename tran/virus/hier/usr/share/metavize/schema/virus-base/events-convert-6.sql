-- events convert for release 3.1

--------------------
-- link to pl_endp |
--------------------

CREATE TABLE events.tr_virus_tmp AS
    SELECT evt.event_id, endp.event_id AS pl_endp_id, clean, virus_name::text,
           virus_cleaned, vendor_name::text, evt.time_stamp
    FROM events.tr_virus_evt evt JOIN pl_endp endp USING (session_id);

DROP TABLE events.tr_virus_evt;
ALTER TABLE events.tr_virus_tmp RENAME TO tr_virus_evt;
ALTER TABLE events.tr_virus_evt ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE events.tr_virus_evt ADD PRIMARY KEY (event_id);

--------------------
-- remove varchars |
--------------------

-- events.tr_virus_evt_http

ALTER TABLE events.tr_virus_evt_http ADD COLUMN tmp text;
UPDATE events.tr_virus_evt_http SET tmp = virus_name;
ALTER TABLE events.tr_virus_evt_http DROP COLUMN virus_name;
ALTER TABLE events.tr_virus_evt_http RENAME COLUMN tmp TO virus_name;

ALTER TABLE events.tr_virus_evt_http ADD COLUMN tmp text;
UPDATE events.tr_virus_evt_http SET tmp = vendor_name;
ALTER TABLE events.tr_virus_evt_http DROP COLUMN vendor_name;
ALTER TABLE events.tr_virus_evt_http RENAME COLUMN tmp TO vendor_name;

-- events.tr_virus_evt_smtp

ALTER TABLE events.tr_virus_evt_smtp ADD COLUMN tmp text;
UPDATE events.tr_virus_evt_smtp SET tmp = virus_name;
ALTER TABLE events.tr_virus_evt_smtp DROP COLUMN virus_name;
ALTER TABLE events.tr_virus_evt_smtp RENAME COLUMN tmp TO virus_name;

ALTER TABLE events.tr_virus_evt_smtp ADD COLUMN tmp text;
UPDATE events.tr_virus_evt_smtp SET tmp = vendor_name;
ALTER TABLE events.tr_virus_evt_smtp DROP COLUMN vendor_name;
ALTER TABLE events.tr_virus_evt_smtp RENAME COLUMN tmp TO vendor_name;

-- events.tr_virus_evt_mail

ALTER TABLE events.tr_virus_evt_mail ADD COLUMN tmp text;
UPDATE events.tr_virus_evt_mail SET tmp = virus_name;
ALTER TABLE events.tr_virus_evt_mail DROP COLUMN virus_name;
ALTER TABLE events.tr_virus_evt_mail RENAME COLUMN tmp TO virus_name;

ALTER TABLE events.tr_virus_evt_mail ADD COLUMN tmp text;
UPDATE events.tr_virus_evt_mail SET tmp = vendor_name;
ALTER TABLE events.tr_virus_evt_mail DROP COLUMN vendor_name;
ALTER TABLE events.tr_virus_evt_mail RENAME COLUMN tmp TO vendor_name;
