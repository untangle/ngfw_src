CREATE INDEX tr_virus_evt_ts_idx ON tr_virus_evt (time_stamp);
CREATE INDEX tr_virus_http_ts_idx ON tr_virus_evt_http (time_stamp);

DELETE FROM tr_virus_evt WHERE time_stamp < (:cutoff)::timestamp;
DELETE FROM tr_virus_evt_http WHERE time_stamp < (:cutoff)::timestamp;

DROP INDEX tr_virus_evt_ts_idx;
DROP INDEX tr_virus_http_ts_idx;
