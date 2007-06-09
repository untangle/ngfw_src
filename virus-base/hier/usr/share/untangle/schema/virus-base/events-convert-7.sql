-- events conversion for release-5.0

ALTER TABLE events.tr_virus_evt RENAME TO n_virus_evt;
ALTER TABLE events.tr_virus_evt_http RENAME TO n_virus_evt_http;
ALTER TABLE events.tr_virus_evt_smtp RENAME TO n_virus_evt_smtp;
ALTER TABLE events.tr_virus_evt_mail RENAME TO n_virus_evt_mail;
