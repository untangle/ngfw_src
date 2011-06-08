-- settings conversion for release-9.0

-- interface enumeration changes
UPDATE settings.n_firewall_rule SET src_intf_matcher = '250' WHERE src_intf_matcher = '7';
UPDATE settings.n_firewall_rule SET src_intf_matcher = '7' WHERE src_intf_matcher = '6';
UPDATE settings.n_firewall_rule SET src_intf_matcher = '6' WHERE src_intf_matcher = '5';
UPDATE settings.n_firewall_rule SET src_intf_matcher = '5' WHERE src_intf_matcher = '4';
UPDATE settings.n_firewall_rule SET src_intf_matcher = '4' WHERE src_intf_matcher = '3';
UPDATE settings.n_firewall_rule SET src_intf_matcher = '3' WHERE src_intf_matcher = '2';
UPDATE settings.n_firewall_rule SET src_intf_matcher = '2' WHERE src_intf_matcher = '1';
UPDATE settings.n_firewall_rule SET src_intf_matcher = '1' WHERE src_intf_matcher = '0';

UPDATE settings.u_firewall_rule SET src_intf_matcher = '1' WHERE src_intf_matcher = 'O';
UPDATE settings.u_firewall_rule SET src_intf_matcher = '2' WHERE src_intf_matcher = 'I';
UPDATE settings.u_firewall_rule SET src_intf_matcher = '3' WHERE src_intf_matcher = 'D';
UPDATE settings.u_firewall_rule SET src_intf_matcher = '250' WHERE src_intf_matcher = 'V';

UPDATE settings.n_firewall_rule SET dst_intf_matcher = '250' WHERE dst_intf_matcher = '7';
UPDATE settings.n_firewall_rule SET dst_intf_matcher = '7' WHERE dst_intf_matcher = '6';
UPDATE settings.n_firewall_rule SET dst_intf_matcher = '6' WHERE dst_intf_matcher = '5';
UPDATE settings.n_firewall_rule SET dst_intf_matcher = '5' WHERE dst_intf_matcher = '4';
UPDATE settings.n_firewall_rule SET dst_intf_matcher = '4' WHERE dst_intf_matcher = '3';
UPDATE settings.n_firewall_rule SET dst_intf_matcher = '3' WHERE dst_intf_matcher = '2';
UPDATE settings.n_firewall_rule SET dst_intf_matcher = '2' WHERE dst_intf_matcher = '1';
UPDATE settings.n_firewall_rule SET dst_intf_matcher = '1' WHERE dst_intf_matcher = '0';

UPDATE settings.u_firewall_rule SET dst_intf_matcher = '1' WHERE dst_intf_matcher = 'O';
UPDATE settings.u_firewall_rule SET dst_intf_matcher = '2' WHERE dst_intf_matcher = 'I';
UPDATE settings.u_firewall_rule SET dst_intf_matcher = '3' WHERE dst_intf_matcher = 'D';
UPDATE settings.u_firewall_rule SET dst_intf_matcher = '250' WHERE dst_intf_matcher = 'V';

UPDATE settings.n_firewall_rule set src_intf_matcher = 'any' WHERE src_intf_matcher = 'more_trusted';
UPDATE settings.n_firewall_rule set src_intf_matcher = 'any' WHERE src_intf_matcher = 'less_trusted';

UPDATE settings.n_firewall_rule set dst_intf_matcher = 'any' WHERE dst_intf_matcher = 'more_trusted';
UPDATE settings.n_firewall_rule set dst_intf_matcher = 'any' WHERE dst_intf_matcher = 'less_trusted';

