-- settings schema for release-5.0

-- com.untangle.tran.httpblocker.BlockTemplate
CREATE TABLE settings.n_webfilter_template (
    message_id int8 NOT NULL,
    HEADER varchar(255),
    CONTACT varchar(255),
    PRIMARY KEY (message_id));

-- com.untangle.tran.httpblocker.HttpBlockerSettings.blockedUrls
CREATE TABLE settings.n_webfilter_passed_urls (
    setting_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

-- com.untangle.tran.httpblocker.HttpBlockerSettings
CREATE TABLE settings.n_webfilter_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    template int8 NOT NULL,
    block_all_ip_hosts bool NOT NULL,
    fascist_mode bool NOT NULL,
    PRIMARY KEY (settings_id));

-- com.untangle.tran.httpblocker.HttpBlockerSettings.blockedExtensions
CREATE TABLE settings.n_webfilter_extensions (
    setting_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

-- com.untangle.tran.httpblocker.HttpBlockerSettings.blockedMimeTypes
CREATE TABLE settings.n_webfilter_mime_types (
    setting_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

-- com.untangle.tran.httpblocker.HttpBlockerSettings.passedClients
CREATE TABLE settings.n_webfilter_passed_clients (
    setting_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

-- com.untangle.tran.httpblocker.HttpBlockerSettings.blockedUrls
CREATE TABLE settings.n_webfilter_blocked_urls (
    setting_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

-- com.untangle.tran.httpblocker.BlacklistCategory
CREATE TABLE settings.n_webfilter_blcat (
    category_id int8 NOT NULL,
    name varchar(255),
    display_name varchar(255),
    description varchar(255),
    block_domains bool NOT NULL,
    block_urls bool NOT NULL,
    block_expressions bool NOT NULL,
    log_only bool NOT NULL,
    setting_id int8,
    position int4,
    PRIMARY KEY (category_id));

----------------
-- constraints |
----------------

-- foreign key constraints

ALTER TABLE settings.n_webfilter_passed_urls
    ADD CONSTRAINT fk_tr_httpblk_passed_urls
    FOREIGN KEY (setting_id) REFERENCES settings.n_webfilter_settings;

ALTER TABLE settings.n_webfilter_passed_urls
    ADD CONSTRAINT fk_tr_httpblk_passed_urls_rule
    FOREIGN KEY (rule_id) REFERENCES settings.u_string_rule;

ALTER TABLE settings.n_webfilter_settings
    ADD CONSTRAINT fk_tr_httpblk_settings_template
    FOREIGN KEY (template) REFERENCES settings.n_webfilter_template;

ALTER TABLE settings.n_webfilter_settings
    ADD CONSTRAINT fk_tr_httpblk_settings
    FOREIGN KEY (tid) REFERENCES settings.tid;

ALTER TABLE settings.n_webfilter_extensions
    ADD CONSTRAINT fk_tr_httpblk_extensions
    FOREIGN KEY (setting_id) REFERENCES settings.n_webfilter_settings;

ALTER TABLE settings.n_webfilter_extensions
    ADD CONSTRAINT fk_tr_httpblk_extensions_rule
    FOREIGN KEY (rule_id) REFERENCES settings.u_string_rule;

ALTER TABLE settings.n_webfilter_mime_types
    ADD CONSTRAINT fk_tr_httpblk_mime_types
    FOREIGN KEY (setting_id) REFERENCES settings.n_webfilter_settings;

ALTER TABLE settings.n_webfilter_mime_types
    ADD CONSTRAINT fk_tr_httpblk_mime_types_rule
    FOREIGN KEY (rule_id) REFERENCES settings.u_mimetype_rule;

ALTER TABLE settings.n_webfilter_passed_clients
    ADD CONSTRAINT fk_tr_httpblk_passed_clients
    FOREIGN KEY (setting_id) REFERENCES settings.n_webfilter_settings;

ALTER TABLE settings.n_webfilter_passed_clients
    ADD CONSTRAINT fk_tr_httpblk_passed_clients_rule
    FOREIGN KEY (rule_id) REFERENCES settings.u_ipmaddr_rule;

ALTER TABLE settings.n_webfilter_blocked_urls
    ADD CONSTRAINT fk_tr_httpblk_blocked_urls
    FOREIGN KEY (setting_id) REFERENCES settings.n_webfilter_settings;

ALTER TABLE settings.n_webfilter_blocked_urls
    ADD CONSTRAINT fk_tr_httpblk_blocked_urls_rule
    FOREIGN KEY (rule_id) REFERENCES settings.u_string_rule;

ALTER TABLE settings.n_webfilter_blcat
    ADD CONSTRAINT fk_tr_httpblk_blcat
    FOREIGN KEY (setting_id) REFERENCES settings.n_webfilter_settings;
