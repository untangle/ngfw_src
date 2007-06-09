-- convert script for release 1.4a

-- drop foreign key constraints for logging

ALTER TABLE tr_http_evt_resp DROP CONSTRAINT FKC9BB12A21F20A4EB;
ALTER TABLE tr_http_evt_req DROP CONSTRAINT FK40505B6C1F20A4EB;
