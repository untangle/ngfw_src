-- convert script for release 2.5

CREATE INDEX tr_spyware_cookie_rid_idx on tr_spyware_evt_cookie (request_id);
CREATE INDEX tr_spyware_ax_rid_idx on tr_spyware_evt_activex (request_id);
CREATE INDEX tr_spyware_acc_sid_idx ON tr_spyware_evt_access (session_id);
