-- events conversion for release-9.2

ALTER TABLE events.n_spam_smtp_rbl_evt RENAME TO n_spam_smtp_tarpit_evt;
ALTER INDEX events.n_spam_smtp_rbl_evt_ts_idx RENAME TO n_spam_smtp_tarpit_evt_ts_idx;

ALTER TABLE events.n_spam_smtp_tarpit_evt ADD COLUMN vendor_name varchar(255);
ALTER TABLE events.n_spam_smtp_tarpit_evt DROP COLUMN skipped;
ALTER TABLE events.n_spam_smtp_tarpit_evt RENAME COLUMN pl_endp_id to session_id;

