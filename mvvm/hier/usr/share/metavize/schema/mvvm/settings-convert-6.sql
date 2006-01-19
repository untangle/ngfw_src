-- settings conversion for release-3.2

-- Added for AddressBook    
-- com.metavize.mvvm.addrbook.RepositorySettings
CREATE TABLE settings.ab_repository_settings (
    settings_id int8 NOT NULL,
    superuser_dn text,
    superuser_pass text,
    search_base text,
    ldap_host text,
    port int4,
    PRIMARY KEY (settings_id));


-- Added for AddressBook
-- com.metavize.mvvm.addrbook.AddressBookSettings
CREATE TABLE settings.ab_settings (
    settings_id int8 NOT NULL,
    ad_repo_settings int8 NOT NULL,
    ab_configuration char(1) NOT NULL,
    PRIMARY KEY (settings_id));

UPDATE tid SET policy_id = NULL WHERE id IN
    (SELECT tid.id FROM tid
     LEFT JOIN transform_persistent_state ON tid.id = tid
     WHERE target_state IS NULL AND NOT policy_id IS NULL);

DELETE FROM user_policy_rule WHERE set_id IS NULL;