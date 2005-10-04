-- convert script for release 3.0

---------------
-- new tables |
---------------

CREATE TABLE settings.tr_spyware_wl (
    settings_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (settings_id, position));

ALTER TABLE settings.tr_spyware_wl
    ADD CONSTRAINT fk_tr_spyware_wl
    FOREIGN KEY (rule_id) REFERENCES settings.string_rule;

---------------------
-- normalize naming |
---------------------

ALTER TABLE tr_spyware_cr RENAME column setting_id to settings_id;
ALTER TABLE tr_spyware_ar RENAME column setting_id to settings_id;
