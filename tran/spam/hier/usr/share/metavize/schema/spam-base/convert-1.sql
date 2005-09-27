-- add missing indices
CREATE INDEX tr_spam_evt_mid_idx
    ON events.tr_spam_evt (msg_id);
CREATE INDEX tr_spam_evt_smtp_mid_idx
    ON events.tr_spam_evt_smtp (msg_id);
