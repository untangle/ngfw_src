

-- Migrate the settings so that the client no longer uses the primary key.
-- Instead of using the group id, this now just uses the name of the group.
CREATE TABLE settings.n_openvpn_client_2 AS
    SELECT 
         settings.n_openvpn_client.rule_id AS rule_id, 
         settings.n_openvpn_client."name" AS "name", 
         settings.n_openvpn_group."name" AS group_name, 
         settings.n_openvpn_client.address AS address, 
         settings.n_openvpn_client.is_edgeguard AS is_edgeguard, 
         settings.n_openvpn_client.category AS category, 
         settings.n_openvpn_client.description AS description, 
         settings.n_openvpn_client.live AS live, 
         settings.n_openvpn_client.alert AS alert, 
         settings.n_openvpn_client."log" AS "log", 
         settings.n_openvpn_client.settings_id AS settings_id, 
         settings.n_openvpn_client."position" AS "position", 
         settings.n_openvpn_client.dist_key AS dist_key, 
         settings.n_openvpn_client.dist_passwd AS dist_passwd
     FROM 
         settings.n_openvpn_client
     LEFT JOIN settings.n_openvpn_group ON settings.n_openvpn_client.group_id = n_openvpn_group.rule_id;

ALTER TABLE settings.n_openvpn_client_2 ADD PRIMARY KEY(rule_id);
ALTER TABLE settings.n_openvpn_client_2 ALTER COLUMN rule_id SET NOT NULL;

ALTER TABLE n_openvpn_client_2 ADD CONSTRAINT n_openvpn_client_to_settings
        FOREIGN KEY (settings_id) REFERENCES n_openvpn_settings;

CREATE TABLE settings.n_openvpn_site_2 AS
    SELECT 
         settings.n_openvpn_site.rule_id AS rule_id, 
         settings.n_openvpn_site."name" AS "name", 
         settings.n_openvpn_group."name" AS group_name, 
         settings.n_openvpn_site.address AS address, 
         settings.n_openvpn_site.is_edgeguard AS is_edgeguard, 
         settings.n_openvpn_site.category AS category, 
         settings.n_openvpn_site.description AS description, 
         settings.n_openvpn_site.live AS live, 
         settings.n_openvpn_site.alert AS alert, 
         settings.n_openvpn_site."log" AS "log", 
         settings.n_openvpn_site.settings_id AS settings_id, 
         settings.n_openvpn_site."position" AS "position", 
         settings.n_openvpn_site.dist_key AS dist_key, 
         settings.n_openvpn_site.dist_passwd AS dist_passwd
     FROM 
         settings.n_openvpn_site
     LEFT JOIN settings.n_openvpn_group ON settings.n_openvpn_site.group_id = n_openvpn_group.rule_id;

ALTER TABLE settings.n_openvpn_site_2 ADD PRIMARY KEY(rule_id);
ALTER TABLE settings.n_openvpn_site_2 ALTER COLUMN rule_id SET NOT NULL;

ALTER TABLE settings.n_openvpn_site_2 ADD CONSTRAINT n_openvpn_site_to_settings
        FOREIGN KEY (settings_id) REFERENCES n_openvpn_settings;

ALTER TABLE settings.n_openvpn_c_site_network DROP CONSTRAINT FKF75374E830D9EF2D;

ALTER TABLE n_openvpn_c_site_network ADD CONSTRAINT n_openvpn_c_site_network_to_site
        FOREIGN KEY (client_id) REFERENCES n_openvpn_site_2;


