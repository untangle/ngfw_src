-- events schema for release 3.1

-----------
-- events |
-----------

-- com.untangle.tran.firewall.FirewallEvent
CREATE TABLE events.tr_firewall_evt (
    event_id int8 NOT NULL,
    pl_endp_id int8,
    was_blocked bool,
    rule_id int8,
    rule_index int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- com.untangle.tran.firewall.FirewallStatisticEvent
CREATE TABLE events.tr_firewall_statistic_evt (
    event_id int8 NOT NULL,
    tcp_block_default int4,
    tcp_block_rule int4,
    tcp_pass_default int4,
    tcp_pass_rule int4,
    udp_block_default int4,
    udp_block_rule int4,
    udp_pass_default int4,
    udp_pass_rule int4,
    icmp_block_default int4,
    icmp_block_rule int4,
    icmp_pass_default int4,
    icmp_pass_rule int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

----------------
-- constraints |
----------------

-- indices for reporting

CREATE INDEX tr_firewall_evt_plepid_idx ON events.tr_firewall_evt (pl_endp_id);

