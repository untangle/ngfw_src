-- settings convert for release-4.2


-- Drop the constraints
ALTER TABLE tr_openvpn_s_site_network DROP CONSTRAINT FKF75374E89E4538C5;

ALTER TABLE tr_openvpn_client DROP CONSTRAINT FKF4113219E4538C5;

ALTER TABLE tr_openvpn_site DROP CONSTRAINT tr_openvpn_site_to_settings;

ALTER TABLE tr_openvpn_group DROP CONSTRAINT FKB66694699E4538C5;

ALTER TABLE tr_openvpn_settings DROP CONSTRAINT FK62A65DF939CBD260;

-- com.untangle.tran.openvpn.VpnSettings
DROP TABLE settings.tr_openvpn_settings_tmp;

CREATE TABLE settings.tr_openvpn_settings_tmp AS
    SELECT id, tid, server_address::text, domain, key_size,
           country, province, locality, org, org_unit,
           email, max_clients, is_edgeguard_client, is_ca_on_usb,
           is_bridge, expose_clients, keep_alive, public_port,
           is_dns_override, dns_1, dns_2
    FROM settings.tr_openvpn_settings;

DROP TABLE settings.tr_openvpn_settings CASCADE;
ALTER TABLE settings.tr_openvpn_settings_tmp RENAME TO tr_openvpn_settings;

ALTER TABLE settings.tr_openvpn_settings ALTER COLUMN id SET NOT NULL;
ALTER TABLE settings.tr_openvpn_settings ALTER COLUMN tid SET NOT NULL;
ALTER TABLE settings.tr_openvpn_settings ADD PRIMARY KEY (id);

ALTER TABLE tr_openvpn_s_site_network ADD CONSTRAINT FKF75374E89E4538C5
        FOREIGN KEY (settings_id) REFERENCES tr_openvpn_settings;

ALTER TABLE tr_openvpn_client ADD CONSTRAINT FKF4113219E4538C5
        FOREIGN KEY (settings_id) REFERENCES tr_openvpn_settings;

ALTER TABLE tr_openvpn_site ADD CONSTRAINT tr_openvpn_site_to_settings
        FOREIGN KEY (settings_id) REFERENCES tr_openvpn_settings;

ALTER TABLE tr_openvpn_group ADD CONSTRAINT FKB66694699E4538C5
        FOREIGN KEY (settings_id) REFERENCES tr_openvpn_settings;

ALTER TABLE tr_openvpn_settings ADD CONSTRAINT FK62A65DF939CBD260
        FOREIGN KEY (tid) REFERENCES tid;

-- strip the /32 off the end of the server address string.
UPDATE settings.tr_openvpn_settings SET server_address = REPLACE( server_address, '/32', '' );

-- transfer the server address from the email field into the server address field.

UPDATE settings.tr_openvpn_settings SET server_address = email WHERE
       is_edgeguard_client = 't' AND
       email != '' AND
       server_address != email;

-- fix the previous hack of saving the address inside of the email address.
UPDATE settings.tr_openvpn_settings SET email = '' WHERE
       is_edgeguard_client = 't';

