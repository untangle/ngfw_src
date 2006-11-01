-- schema for release 1.5

-------------
-- settings |
-------------

-- com.untangle.tran.httpblocker.BlockTemplate
CREATE TABLE settings.tr_httpblk_template (
    message_id int8 NOT NULL,
    HEADER varchar(255),
    CONTACT varchar(255),
    PRIMARY KEY (message_id));

-- com.untangle.tran.httpblocker.HttpBlockerSettings.blockedUrls
CREATE TABLE settings.tr_httpblk_passed_urls (
    setting_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

-- com.untangle.tran.httpblocker.HttpBlockerSettings
CREATE TABLE settings.tr_httpblk_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    template int8 NOT NULL,
    block_all_ip_hosts bool NOT NULL,
    PRIMARY KEY (settings_id));

-- com.untangle.tran.httpblocker.HttpBlockerSettings.blockedExtensions
CREATE TABLE settings.tr_httpblk_extensions (
    setting_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

-- com.untangle.tran.httpblocker.HttpBlockerSettings.blockedMimeTypes
CREATE TABLE settings.tr_httpblk_mime_types (
    setting_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

-- com.untangle.tran.httpblocker.HttpBlockerSettings.passedClients
CREATE TABLE settings.tr_httpblk_passed_clients (
    setting_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

-- com.untangle.tran.httpblocker.HttpBlockerSettings.blockedUrls
CREATE TABLE settings.tr_httpblk_blocked_urls (
    setting_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

-- com.untangle.tran.httpblocker.BlacklistCategory
CREATE TABLE settings.tr_httpblk_blcat (
    category_id int8 NOT NULL,
    name varchar(255),
    display_name varchar(255),
    description varchar(255),
    block_domains bool,
    block_urls bool,
    block_expressions bool,
    setting_id int8,
    position int4,
    PRIMARY KEY (category_id));

-----------
-- events |
-----------

-- com.untangle.tran.httpblocker.HttpBlockerEvent
CREATE TABLE events.tr_httpblk_evt_blk (
    event_id int8 NOT NULL,
    request_id int8,
    action char(1),
    reason char(1),
    category varchar(255),
    time_stamp timestamp,
    PRIMARY KEY (event_id));

----------------
-- constraints |
----------------

-- foreign key constraints

ALTER TABLE settings.tr_httpblk_passed_urls
    ADD CONSTRAINT fk_tr_httpblk_passed_urls
    FOREIGN KEY (setting_id) REFERENCES settings.tr_httpblk_settings;

ALTER TABLE settings.tr_httpblk_passed_urls
    ADD CONSTRAINT fk_tr_httpblk_passed_urls_rule
    FOREIGN KEY (rule_id) REFERENCES settings.string_rule;

ALTER TABLE settings.tr_httpblk_settings
    ADD CONSTRAINT fk_tr_httpblk_settings_template
    FOREIGN KEY (template) REFERENCES settings.tr_httpblk_template;

ALTER TABLE settings.tr_httpblk_settings
    ADD CONSTRAINT fk_tr_httpblk_settings
    FOREIGN KEY (tid) REFERENCES settings.tid;

ALTER TABLE settings.tr_httpblk_extensions
    ADD CONSTRAINT fk_tr_httpblk_extensions
    FOREIGN KEY (setting_id) REFERENCES settings.tr_httpblk_settings;

ALTER TABLE settings.tr_httpblk_extensions
    ADD CONSTRAINT fk_tr_httpblk_extensions_rule
    FOREIGN KEY (rule_id) REFERENCES settings.string_rule;

ALTER TABLE settings.tr_httpblk_mime_types
    ADD CONSTRAINT fk_tr_httpblk_mime_types
    FOREIGN KEY (setting_id) REFERENCES settings.tr_httpblk_settings;

ALTER TABLE settings.tr_httpblk_mime_types
    ADD CONSTRAINT fk_tr_httpblk_mime_types_rule
    FOREIGN KEY (rule_id) REFERENCES settings.mimetype_rule;

ALTER TABLE settings.tr_httpblk_passed_clients
    ADD CONSTRAINT fk_tr_httpblk_passed_clients
    FOREIGN KEY (setting_id) REFERENCES settings.tr_httpblk_settings;

ALTER TABLE settings.tr_httpblk_passed_clients
    ADD CONSTRAINT fk_tr_httpblk_passed_clients_rule
    FOREIGN KEY (rule_id) REFERENCES settings.ipmaddr_rule;

ALTER TABLE settings.tr_httpblk_blocked_urls
    ADD CONSTRAINT fk_tr_httpblk_blocked_urls
    FOREIGN KEY (setting_id) REFERENCES settings.tr_httpblk_settings;

ALTER TABLE settings.tr_httpblk_blocked_urls
    ADD CONSTRAINT fk_tr_httpblk_blocked_urls_rule
    FOREIGN KEY (rule_id) REFERENCES settings.string_rule;

ALTER TABLE settings.tr_httpblk_blcat
    ADD CONSTRAINT fk_tr_httpblk_blcat
    FOREIGN KEY (setting_id) REFERENCES settings.tr_httpblk_settings;
