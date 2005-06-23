-- convert script for release 1.4a

-- drop foreign key constraints for logging

ALTER TABLE tr_nat_evt_dhcp_abs_leases DROP CONSTRAINT FK852599793F3A2900;
ALTER TABLE tr_nat_evt_dhcp_abs_leases DROP CONSTRAINT FK852599798C84B540;

-- fix syntax error in release 1.4
UPDATE tr_nat_settings SET dns_local_domain = '' WHERE dns_local_domain ISNULL;