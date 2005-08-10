CREATE INDEX tr_virus_rollup_evt_idx ON tr_virus_evt (time_stamp);
CREATE INDEX tr_virus_rollup_http_idx ON tr_virus_evt_http (time_stamp);
CREATE INDEX tr_virus_rollup_smtp_idx ON tr_virus_evt_smtp (time_stamp);
CREATE INDEX tr_virus_rollup_mail_idx ON tr_virus_evt_mail (time_stamp);

DELETE FROM tr_virus_evt WHERE time_stamp < (:cutoff)::timestamp;
DELETE FROM tr_virus_evt_http WHERE time_stamp < (:cutoff)::timestamp;
DELETE FROM tr_virus_evt_smtp WHERE time_stamp < (:cutoff)::timestamp;
DELETE FROM tr_virus_evt_mail WHERE time_stamp < (:cutoff)::timestamp;

DROP INDEX tr_virus_rollup_evt_idx;
DROP INDEX tr_virus_rollup_http_idx;
DROP INDEX tr_virus_rollup_smtp_idx;
DROP INDEX tr_virus_rollup_mail_idx;
