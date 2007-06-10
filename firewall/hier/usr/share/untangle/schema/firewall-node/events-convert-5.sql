-- events conversion for release-5.0

ALTER TABLE events.tr_firewall_evt RENAME TO n_firewall_evt;
ALTER TABLE events.tr_firewall_statistic_evt RENAME TO n_firewall_statistic_evt;
