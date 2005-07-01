-- Create the table for firewall events.
create table TR_FIREWALL_EVT (
        EVENT_ID int8 not null,
        SESSION_ID int4,
        RULE_ID int8,
        RULE_INDEX int4,
        TIME_STAMP timestamp,
        primary key (EVENT_ID));

-- Remove a column that was never used
alter table FIREWALL_RULE drop column is_dst_redirect;
