-- events conversion for release-5.0

ALTER TABLE events.tr_nat_evt_dhcp RENAME TO n_router_evt_dhcp;
ALTER TABLE events.dhcp_abs_lease RENAME TO n_router_dhcp_abs_lease;
ALTER TABLE events.tr_nat_evt_dhcp_abs RENAME TO n_router_evt_dhcp_abs;
ALTER TABLE events.tr_nat_evt_dhcp_abs_leases RENAME TO n_router_evt_dhcp_abs_leases;
ALTER TABLE events.tr_nat_redirect_evt RENAME TO n_router_redirect_evt;
ALTER TABLE events.tr_nat_statistic_evt RENAME TO n_router_statistic_evt;
