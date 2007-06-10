-- events conversion for release-5.0

ALTER TABLE events.tr_spyware_evt_access RENAME TO n_spyware_evt_access;
ALTER TABLE events.tr_spyware_evt_activex RENAME TO n_spyware_evt_activex;
ALTER TABLE events.tr_spyware_evt_cookie RENAME TO n_spyware_evt_cookie;
ALTER TABLE events.tr_spyware_evt_blacklist RENAME TO n_spyware_evt_blacklist;
ALTER TABLE events.tr_spyware_statistic_evt RENAME TO n_spyware_statistic_evt;
