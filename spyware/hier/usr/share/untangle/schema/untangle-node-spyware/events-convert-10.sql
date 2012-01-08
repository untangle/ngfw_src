-- 9.2

ALTER TABLE events.n_spyware_evt_blacklist RENAME n_spyware_evt_url;
ALTER TABLE events.n_spyware_evt_access RENAME COLUMN pl_endp_id to session_id;
