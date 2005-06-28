-- convert script for release 1.5

-- Insert an item to track whether or not logging of DMZ is enabled.
ALTER TABLE tr_nat_settings ADD COLUMN dmz_logging_enabled bool;

-- Set the new rows to false
UPDATE tr_nat_settings SET dmz_logging_enabled = false;


