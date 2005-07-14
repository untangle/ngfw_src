-- convert script for release 2.5

ALTER TABLE tr_spyware_settings ADD COLUMN url_blacklist_enabled bool;
ALTER TABLE tr_spyware_settings ADD COLUMN url_blacklist_details varchar(255);

UPDATE tr_spyware_settings SET url_blacklist_enabled = true
    WHERE url_blacklist_enabled IS NULL;
UPDATE tr_spyware_settings SET url_blacklist_details = 'no details'
    WHERE url_blacklist_details IS NULL;

CREATE TABLE tr_spyware_evt_blacklist (
    event_id int8 NOT NULL,
    session_id int4,
    request_id int8,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE INDEX tr_spyware_cookie_rid_idx on tr_spyware_evt_cookie (request_id);
CREATE INDEX tr_spyware_bl_rid_idx on tr_spyware_evt_blacklist (request_id);
CREATE INDEX tr_spyware_ax_rid_idx on tr_spyware_evt_activex (request_id);
CREATE INDEX tr_spyware_acc_sid_idx ON tr_spyware_evt_access (session_id);
