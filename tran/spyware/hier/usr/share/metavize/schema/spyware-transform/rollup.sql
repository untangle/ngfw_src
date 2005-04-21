CREATE INDEX tr_spyware_ax_ts_idx ON tr_spyware_evt_activex (time_stamp);
CREATE INDEX tr_spyware_ac_ts_idx ON tr_spyware_evt_access (time_stamp);
CREATE INDEX tr_spyware_co_ts_idx ON tr_spyware_evt_cookie (time_stamp);

DELETE FROM tr_spyware_evt_activex WHERE time_stamp < (:cutoff)::timestamp;
DELETE FROM tr_spyware_evt_access WHERE time_stamp < (:cutoff)::timestamp;
DELETE FROM tr_spyware_evt_cookie WHERE time_stamp < (:cutoff)::timestamp;

DROP INDEX tr_spyware_ax_ts_idx;
DROP INDEX tr_spyware_ac_ts_idx;
DROP INDEX tr_spyware_co_ts_idx;

