-- settings conversion for release-9.1

-- interface enumeration changes

-- convert old values
update n_firewall_rule set src_port_matcher = 'any' where src_port_matcher = 'n/a';
update n_firewall_rule set dst_port_matcher = 'any' where dst_port_matcher = 'n/a';

-- delete old rules
delete from n_firewall_rule where protocol_matcher = 'ping';
delete from n_firewall_rule where protocol_matcher = 'PING';



