
-- rename table, this table stores all events not just blocks, rename to appropriate
ALTER TABLE events.n_webfilter_evt_blk RENAME TO n_webfilter_evt;

-- add two booleans for each event, blocked (was it blocked?) and flagged (was it a violation)
ALTER TABLE events.n_webfilter_evt ADD COLUMN flagged bool;
ALTER TABLE events.n_webfilter_evt ADD COLUMN blocked bool;

-- set blocked of past events based on old columns
UPDATE events.n_webfilter_evt SET blocked = false WHERE action = 'P';
UPDATE events.n_webfilter_evt SET blocked = true  WHERE action = 'B';

-- set flagged of past events based on old columns
UPDATE events.n_webfilter_evt SET flagged = false;
UPDATE events.n_webfilter_evt SET flagged = true WHERE action = 'P';

-- remove obsolete column
ALTER TABLE events.n_webfilter_evt DROP COLUMN action;


