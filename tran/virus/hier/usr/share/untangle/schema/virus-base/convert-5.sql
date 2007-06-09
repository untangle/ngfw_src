-- add missing indices

CREATE INDEX tr_virus_evt_smtp_ts_idx
    ON events.tr_virus_evt_smtp (time_stamp);
CREATE INDEX tr_virus_evt_mail_ts_idx
    ON events.tr_virus_evt_mail (time_stamp);
CREATE INDEX tr_virus_evt_smtp_mid_idx
    ON events.tr_virus_evt_smtp (msg_id);
