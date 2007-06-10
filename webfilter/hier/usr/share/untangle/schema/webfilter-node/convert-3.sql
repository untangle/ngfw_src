-- convert script for release 1.4b
-- bad postinst in previous releases, cumululative conversion

-- drop foreign key constraints for logging

ALTER TABLE tr_httpblk_evt_blk DROP CONSTRAINT FKD760FA7E1F20A4EB;
