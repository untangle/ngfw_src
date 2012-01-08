-- events conversion for release-9.2

ALTER TABLE events.n_protofilter_evt RENAME COLUMN pl_endp_id to session_id;


