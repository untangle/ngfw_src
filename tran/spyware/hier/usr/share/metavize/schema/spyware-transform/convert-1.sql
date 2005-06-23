-- convert script for release 1.4a

-- drop foreign key constraints for logging

ALTER TABLE tr_spyware_evt_activex DROP CONSTRAINT FK826FDDAF1F20A4EB;
ALTER TABLE tr_spyware_evt_access DROP CONSTRAINT FK77DE31AF1F20A4EB;
ALTER TABLE tr_spyware_evt_cookie DROP CONSTRAINT FK5DC406AF1F20A4EB;
