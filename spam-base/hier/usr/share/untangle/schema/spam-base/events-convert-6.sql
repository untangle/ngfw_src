-- events conversion for release-5.0

ALTER TABLE events.tr_spam_evt_smtp RENAME TO n_spam_evt_smtp;
ALTER TABLE events.tr_spam_evt RENAME TO n_spam_evt;
ALTER TABLE events.tr_spam_smtp_rbl_evt RENAME TO n_spam_smtp_rbl_evt;
