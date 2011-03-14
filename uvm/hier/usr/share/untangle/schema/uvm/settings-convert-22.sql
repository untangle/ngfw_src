-- settings conversion for release-8.1

-- interface enumeration changes
UPDATE settings.u_user_policy_rule SET client_intf_matcher = '1' WHERE client_intf_matcher = '0';
UPDATE settings.u_user_policy_rule SET client_intf_matcher = '2' WHERE client_intf_matcher = '1';
UPDATE settings.u_user_policy_rule SET client_intf_matcher = '3' WHERE client_intf_matcher = '2';
UPDATE settings.u_user_policy_rule SET client_intf_matcher = '4' WHERE client_intf_matcher = '3';
UPDATE settings.u_user_policy_rule SET client_intf_matcher = '5' WHERE client_intf_matcher = '4';
UPDATE settings.u_user_policy_rule SET client_intf_matcher = '6' WHERE client_intf_matcher = '5';
UPDATE settings.u_user_policy_rule SET client_intf_matcher = '7' WHERE client_intf_matcher = '6';
UPDATE settings.u_user_policy_rule SET client_intf_matcher = '250' WHERE client_intf_matcher = '7';

UPDATE settings.u_user_policy_rule SET server_intf_matcher = '1' WHERE server_intf_matcher = '0';
UPDATE settings.u_user_policy_rule SET server_intf_matcher = '2' WHERE server_intf_matcher = '1';
UPDATE settings.u_user_policy_rule SET server_intf_matcher = '3' WHERE server_intf_matcher = '2';
UPDATE settings.u_user_policy_rule SET server_intf_matcher = '4' WHERE server_intf_matcher = '3';
UPDATE settings.u_user_policy_rule SET server_intf_matcher = '5' WHERE server_intf_matcher = '4';
UPDATE settings.u_user_policy_rule SET server_intf_matcher = '6' WHERE server_intf_matcher = '5';
UPDATE settings.u_user_policy_rule SET server_intf_matcher = '7' WHERE server_intf_matcher = '6';
UPDATE settings.u_user_policy_rule SET server_intf_matcher = '250' WHERE server_intf_matcher = '7';

UPDATE settings.u_user_policy_rule SET client_intf_matcher = 'any' WHERE client_intf_matcher = 'more_trusted';
UPDATE settings.u_user_policy_rule SET client_intf_matcher = 'any' WHERE client_intf_matcher = 'less_trusted';

UPDATE settings.u_user_policy_rule SET server_intf_matcher = 'any' WHERE server_intf_matcher = 'more_trusted';
UPDATE settings.u_user_policy_rule SET server_intf_matcher = 'any' WHERE server_intf_matcher = 'less_trusted';

-- drop obsolete (unused) tables
DROP TABLE settings.u_ddnsq_settings;
DROP TABLE settings.u_dhcp_lease_rule;
DROP TABLE settings.u_dns_static_host_rule;
DROP TABLE settings.u_network_intf;
DROP TABLE settings.u_ip_network;
DROP TABLE settings.u_network_route;
DROP TABLE settings.u_network_space;
DROP TABLE settings.u_redirect_rule;
DROP TABLE settings.u_redirects;
DROP TABLE settings.u_network_settings;
DROP TABLE settings.u_network_services;
DROP TABLE settings.u_dhcp_lease_list;
DROP TABLE settings.u_dns_host_list;
DROP TABLE settings.u_pppoe_connection;
DROP TABLE settings.u_pppoe;
DROP TABLE settings.u_wmi_settings;








