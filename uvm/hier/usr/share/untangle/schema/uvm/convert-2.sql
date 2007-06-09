-- convert script for release 1.4a

-- drop foreign key constraints for logging

ALTER TABLE mvvm_evt_pipeline DROP CONSTRAINT FK9CF995D62F5A0D7;
