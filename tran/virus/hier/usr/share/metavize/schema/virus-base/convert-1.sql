-- convert script for release 1.4a

-- drop foreign key constraints for logging

ALTER TABLE tr_virus_evt_http DROP CONSTRAINT FK6E88EC87D98C8FC4;
