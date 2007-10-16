-- settings conversion for release-5.0
-- $HeadURL$
-- Copyright (c) 2003-2007 Untangle, Inc.
--
-- This program is free software; you can redistribute it and/or modify
-- it under the terms of the GNU General Public License, version 2,
-- as published by the Free Software Foundation.
--
-- This program is distributed in the hope that it will be useful, but
-- AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
-- NONINFRINGEMENT.  See the GNU General Public License for more details.
--
-- You should have received a copy of the GNU General Public License
-- along with this program; if not, write to the Free Software
-- Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
--

ALTER TABLE settings.admin_settings RENAME TO u_admin_settings;
ALTER TABLE settings.mvvm_user RENAME TO u_user;
ALTER TABLE settings.upgrade_settings RENAME TO u_upgrade_settings;
ALTER TABLE settings.mail_settings RENAME TO u_mail_settings;
ALTER TABLE settings.logging_settings RENAME TO u_logging_settings;
ALTER TABLE settings.policy RENAME TO u_policy;
ALTER TABLE settings.mvvm_user_policy_rules RENAME TO u_user_policy_rules;
ALTER TABLE settings.user_policy_rule RENAME TO u_user_policy_rule;
ALTER TABLE settings.system_policy_rule RENAME TO u_system_policy_rule;
ALTER TABLE settings.transform_args RENAME TO u_node_args;
ALTER TABLE settings.mackage_state RENAME TO u_mackage_state;
ALTER TABLE settings.transform_manager_state RENAME TO u_node_manager_state;
ALTER TABLE settings.period RENAME TO u_period;
ALTER TABLE settings.transform_preferences RENAME TO u_node_preferences;
ALTER TABLE settings.string_rule RENAME TO u_string_rule;
ALTER TABLE settings.tid RENAME TO u_tid;
ALTER TABLE settings.transform_persistent_state RENAME TO u_node_persistent_state;
ALTER TABLE settings.ipmaddr_dir RENAME TO u_ipmaddr_dir;
ALTER TABLE settings.mimetype_rule RENAME TO u_mimetype_rule;
ALTER TABLE settings.ipmaddr_dir_entries RENAME TO u_ipmaddr_dir_entries;
ALTER TABLE settings.ipmaddr_rule RENAME TO u_ipmaddr_rule;
ALTER TABLE settings.snmp_settings RENAME TO u_snmp_settings;
ALTER TABLE settings.ab_repository_settings RENAME TO u_ab_repository_settings;
ALTER TABLE settings.ab_settings RENAME TO u_ab_settings;
ALTER TABLE settings.mvvm_ddns_settings RENAME TO u_ddns_settings;
ALTER TABLE settings.mvvm_dhcp_lease_rule RENAME TO u_dhcp_lease_rule;
ALTER TABLE settings.mvvm_dns_static_host_rule RENAME TO u_dns_static_host_rule;
ALTER TABLE settings.mvvm_network_intf RENAME TO u_network_intf;
ALTER TABLE settings.mvvm_ip_network RENAME TO u_ip_network;
ALTER TABLE settings.mvvm_network_route RENAME TO u_network_route;
ALTER TABLE settings.mvvm_network_space RENAME TO u_network_space;
ALTER TABLE settings.mvvm_redirect_rule RENAME TO u_redirect_rule;
ALTER TABLE settings.mvvm_redirects RENAME TO u_redirects;
ALTER TABLE settings.mvvm_network_settings RENAME TO u_network_settings;
ALTER TABLE settings.mvvm_access_settings RENAME TO u_access_settings;
ALTER TABLE settings.mvvm_misc_settings RENAME TO u_misc_settings;
ALTER TABLE settings.mvvm_address_settings RENAME TO u_address_settings;
ALTER TABLE settings.mvvm_network_services RENAME TO u_network_services;
ALTER TABLE settings.mvvm_dhcp_lease_list RENAME TO u_dhcp_lease_list;
ALTER TABLE settings.mvvm_dns_host_list RENAME TO u_dns_host_list;
ALTER TABLE settings.portal_bookmark RENAME TO n_portal_bookmark;
ALTER TABLE settings.portal_user RENAME TO n_portal_user;
ALTER TABLE settings.portal_user_bm_mt RENAME TO n_portal_user_bm_mt;
ALTER TABLE settings.portal_group RENAME TO n_portal_group;
ALTER TABLE settings.portal_group_bm_mt RENAME TO n_portal_group_bm_mt;
ALTER TABLE settings.portal_global RENAME TO n_portal_global;
ALTER TABLE settings.portal_global_bm_mt RENAME TO n_portal_global_bm_mt;
ALTER TABLE settings.portal_home_settings RENAME TO n_portal_home_settings;
ALTER TABLE settings.portal_settings RENAME TO n_portal_settings;
ALTER TABLE settings.mvvm_pppoe_connection RENAME TO u_pppoe_connection;
ALTER TABLE settings.mvvm_pppoe RENAME TO u_pppoe;
ALTER TABLE settings.mvvm_wmi_settings RENAME TO u_wmi_settings;
ALTER TABLE settings.mvvm_branding_settings RENAME TO uvm_branding_settings;

ALTER TABLE settings.idx_string_rule RENAME TO  u_idx_string_rule;

UPDATE settings.u_node_persistent_state SET name = 'untangle-node-webfilter' WHERE name = 'httpblocker-transform';
UPDATE settings.u_node_persistent_state SET name = 'untangle-node-openvpn' WHERE name = 'openvpn-transform';
UPDATE settings.u_node_persistent_state SET name = 'untangle-node-ips' WHERE name = 'ids-transform';
UPDATE settings.u_node_persistent_state SET name = 'untangle-node-sigma' WHERE name = 'sigma-transform';
UPDATE settings.u_node_persistent_state SET name = 'untangle-node-spamassassin' WHERE name = 'spamassassin-transform';
UPDATE settings.u_node_persistent_state SET name = 'untangle-node-reporting' WHERE name = 'reporting-transform';
UPDATE settings.u_node_persistent_state SET name = 'untangle-node-protofilter' WHERE name = 'protofilter-transform';
UPDATE settings.u_node_persistent_state SET name = 'untangle-node-clam' WHERE name = 'clam-transform';
UPDATE settings.u_node_persistent_state SET name = 'untangle-node-test' WHERE name = 'test-transform';
UPDATE settings.u_node_persistent_state SET name = 'untangle-node-router' WHERE name = 'nat-transform';
UPDATE settings.u_node_persistent_state SET name = 'untangle-node-spyware' WHERE name = 'spyware-transform';
UPDATE settings.u_node_persistent_state SET name = 'untangle-node-firewall' WHERE name = 'firewall-transform';
UPDATE settings.u_node_persistent_state SET name = 'untangle-node-shield' WHERE name = 'airgap-transform';
UPDATE settings.u_node_persistent_state SET name = 'untangle-node-phish' WHERE name = 'clamphish-transform';
UPDATE settings.u_node_persistent_state SET name = 'untangle-node-kav' WHERE name = 'kav-transform';
UPDATE settings.u_node_persistent_state SET name = 'untangle-node-portal' WHERE name = 'portal-transform';
UPDATE settings.u_node_persistent_state SET name = 'untangle-node-hauri' WHERE name = 'hauri-transform';
UPDATE settings.u_node_persistent_state SET name = 'untangle-node-boxbackup' WHERE name = 'boxbackup-transform';
UPDATE settings.u_node_persistent_state SET name = 'untangle-casing-http' WHERE name = 'http-casing';
UPDATE settings.u_node_persistent_state SET name = 'untangle-casing-ftp' WHERE name = 'ftp-casing';
UPDATE settings.u_node_persistent_state SET name = 'untangle-casing-mail' WHERE name = 'mail-casing';
