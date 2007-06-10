-- events conversion for release-5.0

ALTER TABLE events.tr_openvpn_statistic_evt RENAME TO n_openvpn_statistic_evt;
ALTER TABLE events.tr_openvpn_distr_evt RENAME TO n_openvpn_distr_evt;
ALTER TABLE events.tr_openvpn_connect_evt RENAME TO n_openvpn_connect_evt;
