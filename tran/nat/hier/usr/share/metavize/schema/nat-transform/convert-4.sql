-- convert script for release 1.5

-- Insert an item to track whether or not logging of DMZ is enabled.
ALTER TABLE tr_nat_settings ADD COLUMN dmz_logging_enabled bool;

-- Insert the events for redirects
create table TR_NAT_REDIRECT_EVT (
        EVENT_ID int8 not null,
        SESSION_ID int4,
        RULE_ID int8,
        RULE_INDEX int4,
        IS_DMZ bool,
        TIME_STAMP timestamp,
        primary key (EVENT_ID));

-- Insert the table for nat statistics
create table TR_NAT_STATISTIC_EVT (
        EVENT_ID      int8 not null,
        NAT_SESSIONS  int4,
        DMZ_SESSIONS  int4,
        TCP_INCOMING  int4,
        TCP_OUTGOING  int4,
        UDP_INCOMING  int4,
        UDP_OUTGOING  int4,
        ICMP_INCOMING int4,
        ICMP_OUTGOING int4,
        TIME_STAMP    timestamp,
        primary key (EVENT_ID));

-- Set the new rows to false
UPDATE tr_nat_settings SET dmz_logging_enabled = false;

