-- duplicate of 12 (to fix an earlier problem with 12 we must call it 13 so existing servers will run)

-- rename table, this table stores all events not just blocks, rename to appropriate
ALTER TABLE events.n_webfilter_evt_blk RENAME TO n_webfilter_evt;
ALTER INDEX events.n_webfilter_evt_blk_ts_idx RENAME TO n_webfilter_evt_ts_idx;
ALTER INDEX events.n_webfilter_evt_blk_pkey RENAME TO n_webfilter_evt_pkey;

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


