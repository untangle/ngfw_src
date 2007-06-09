-- settings conversion for release-5.0

ALTER TABLE settings.tr_dhcp_leases RENAME TO n_router_dhcp_leases;
ALTER TABLE settings.tr_nat_settings RENAME TO n_router_settings;
ALTER TABLE settings.tr_nat_redirects RENAME TO n_router_redirects;
ALTER TABLE settings.tr_nat_dns_hosts RENAME TO n_router_dns_hosts;
