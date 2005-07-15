-- convert script for release 2.5

-- Create the table for firewall events.
create table TR_FIREWALL_EVT (
        EVENT_ID int8 not null,
        SESSION_ID int4,
        RULE_ID int8,
        RULE_INDEX int4,
        TIME_STAMP timestamp,
        primary key (EVENT_ID));

-- Create the table for tracking firewall statistics
create table TR_FIREWALL_STATISTIC_EVT (
        EVENT_ID int8 not null,
        TCP_BLOCK_DEFAULT int4,
        TCP_BLOCK_RULE int4,
        TCP_PASS_DEFAULT int4,
        TCP_PASS_RULE int4,
        UDP_BLOCK_DEFAULT int4,
        UDP_BLOCK_RULE int4,
        UDP_PASS_DEFAULT int4,
        UDP_PASS_RULE int4,
        ICMP_BLOCK_DEFAULT int4,
        ICMP_BLOCK_RULE int4,
        ICMP_PASS_DEFAULT int4,
        ICMP_PASS_RULE int4,
        TIME_STAMP timestamp,
        primary key (EVENT_ID));

-- create index for log reports

CREATE INDEX tr_firewall_evt_sid_idx ON tr_firewall_evt (session_id);

-- Remove a column that is never used
alter table FIREWALL_RULE drop column is_dst_redirect;
