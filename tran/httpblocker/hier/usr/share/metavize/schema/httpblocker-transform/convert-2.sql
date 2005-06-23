-- convert script for release 1.4a

-- drop foreign key constraints for logging

ALTER TABLE tr_httpblk_evt_blk DROP CONSTRAINT FKD760FA7E1F20A4EB;
