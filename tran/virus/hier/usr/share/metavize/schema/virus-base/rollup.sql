CREATE INDEX tr_virus_rollup_evt_idx ON tr_virus_evt (time_stamp);
CREATE INDEX tr_virus_rollup_http_idx ON tr_virus_evt_http (time_stamp);

DELETE FROM tr_virus_evt WHERE time_stamp < (:cutoff)::timestamp;
DELETE FROM tr_virus_evt_http WHERE time_stamp < (:cutoff)::timestamp;
DELETE FROM tr_virus_evt_smtp WHERE time_stamp < (:cutoff)::timestamp;
DELETE FROM tr_virus_evt_mail WHERE time_stamp < (:cutoff)::timestamp;

DROP INDEX tr_virus_rollup_evt_idx;
DROP INDEX tr_virus_rollup_http_idx;

-- fix up event tables if message info data is:
-- 1- invalid (events refer to null messages)
DELETE FROM tr_virus_evt_smtp
  WHERE msg_id IS null;
DELETE FROM tr_virus_evt_mail
  WHERE msg_id IS null;
-- 2- missing (events refer to non-existent messages)
DELETE FROM tr_virus_evt_smtp
  WHERE msg_id IN
        (SELECT msg_id
           FROM tr_virus_evt_smtp
         EXCEPT
         SELECT id
           FROM tr_mail_message_info);
DELETE FROM tr_virus_evt_mail
  WHERE msg_id IN
        (SELECT msg_id
           FROM tr_virus_evt_mail
         EXCEPT
         SELECT id
           FROM tr_mail_message_info);
