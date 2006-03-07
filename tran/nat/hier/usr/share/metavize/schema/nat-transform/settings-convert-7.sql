-- convert script for release 3.2

-- added column for the setup state
ALTER TABLE settings.tr_nat_settings ADD COLUMN setup_state INT2;

-- Indicate that the current settings are deprecated
UPDATE settings.tr_nat_settings SET setup_state=1;

-- Drop all of the constraints on NAT, these settings aren't needed after an upgrade.
ALTER TABLE settings.tr_dhcp_leases   DROP CONSTRAINT fk_tr_dhcp_leases;
ALTER TABLE settings.tr_dhcp_leases   DROP CONSTRAINT fk_tr_dhcp_leases_rule;

ALTER TABLE settings.tr_nat_dns_hosts DROP CONSTRAINT fk_tr_nat_dns_hosts;
ALTER TABLE settings.tr_nat_dns_hosts DROP CONSTRAINT fk_tr_nat_dns_hosts_rule;

ALTER TABLE settings.tr_nat_redirects DROP CONSTRAINT fk_tr_nat_redirects;
ALTER TABLE settings.tr_nat_redirects DROP CONSTRAINT fk_tr_nat_redirects_rule;

