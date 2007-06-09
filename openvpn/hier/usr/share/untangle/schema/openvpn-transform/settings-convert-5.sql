-- settings conversion for release-5.0

ALTER TABLE settings.tr_openvpn_c_site_network RENAME TO n_openvpn_c_site_network;
ALTER TABLE settings.tr_openvpn_s_site_network RENAME TO n_openvpn_s_site_network;
ALTER TABLE settings.tr_openvpn_client RENAME TO n_openvpn_client;
ALTER TABLE settings.tr_openvpn_site RENAME TO n_openvpn_site;
ALTER TABLE settings.tr_openvpn_group RENAME TO n_openvpn_group;
ALTER TABLE settings.tr_openvpn_settings RENAME TO n_openvpn_settings;
