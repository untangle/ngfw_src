CREATE TABLE tr_reporting_settings (id int8 NOT NULL, tid int8 NOT NULL UNIQUE, network_directory int8 NOT NULL, PRIMARY KEY (id));

ALTER TABLE tr_reporting_settings ADD CONSTRAINT FK85B769562C0555C FOREIGN KEY (network_directory) REFERENCES ipmaddr_dir;

ALTER TABLE tr_reporting_settings ADD CONSTRAINT FK85B76951446F FOREIGN KEY (tid) REFERENCES tid;
