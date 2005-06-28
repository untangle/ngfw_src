-- convert script for release 1.5

-- add ACTION column to TR_HTTPBLK_EVT_BLK

ALTER TABLE tr_httpblk_evt_blk ADD COLUMN action char(1);
UPDATE tr_httpblk_evt_blk SET action = 'B';
