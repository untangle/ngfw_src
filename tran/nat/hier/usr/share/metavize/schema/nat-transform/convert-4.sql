-- convert script for release 1.5

-- Insert an item to track whether or not logging of DMZ is enabled.
ALTER TABLE tr_nat_settings ADD COLUMN dmz_logging_enabled bool;

-- Insert the events for redirects
create table TR_REDIRECT_EVT (
        EVENT_ID int8 not null,
        SESSION_ID int4,
        RULE_ID int8,
        RULE_INDEX int4,
        IS_DMZ bool,
        TIME_STAMP timestamp,
        primary key (EVENT_ID));


-- Set the new rows to false
UPDATE tr_nat_settings SET dmz_logging_enabled = false;


