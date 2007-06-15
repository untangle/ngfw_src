-- schema for release-1.4
-- $HeadURL$
-- Copyright (c) 2003-2007 Untangle, Inc. 
--
-- This program is free software; you can redistribute it and/or modify
-- it under the terms of the GNU General Public License, version 2,
-- as published by the Free Software Foundation.
--
-- This program is distributed in the hope that it will be useful, but
-- AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
-- NONINFRINGEMENT.  See the GNU General Public License for more details.
--
-- You should have received a copy of the GNU General Public License
-- along with this program; if not, write to the Free Software
-- Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
--

CREATE TABLE tr_httpblk_template (
    message_id int8 NOT NULL,
    HEADER varchar(255),
    CONTACT varchar(255),
    PRIMARY KEY (message_id));

CREATE TABLE tr_httpblk_passed_urls (
    setting_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

CREATE TABLE tr_httpblk_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    template int8 NOT NULL,
    block_all_ip_hosts bool NOT NULL,
    PRIMARY KEY (settings_id));

CREATE TABLE tr_httpblk_extensions (
    setting_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

CREATE TABLE tr_httpblk_evt_blk (
    event_id int8 NOT NULL,
    request_id int8,
    reason char(1),
    category varchar(255),
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE TABLE tr_httpblk_mime_types (
    setting_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

CREATE TABLE tr_httpblk_passed_clients (
    setting_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

CREATE TABLE tr_httpblk_blocked_urls (
    setting_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

CREATE TABLE tr_httpblk_blcat (
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

ALTER TABLE tr_httpblk_passed_urls ADD CONSTRAINT FK6C8C0C8C1CAE658A FOREIGN KEY (setting_id) REFERENCES tr_httpblk_settings;

ALTER TABLE tr_httpblk_passed_urls ADD CONSTRAINT FK6C8C0C8C871AAD3E FOREIGN KEY (rule_id) REFERENCES string_rule;

ALTER TABLE tr_httpblk_settings ADD CONSTRAINT FK3F2D0D8ADFE0BC7A FOREIGN KEY (template) REFERENCES tr_httpblk_template;

ALTER TABLE tr_httpblk_settings ADD CONSTRAINT FK3F2D0D8A1446F FOREIGN KEY (tid) REFERENCES tid;

ALTER TABLE tr_httpblk_extensions ADD CONSTRAINT FKBC81FBBB871AAD3E FOREIGN KEY (rule_id) REFERENCES string_rule;

ALTER TABLE tr_httpblk_extensions ADD CONSTRAINT FKBC81FBBB1CAE658A FOREIGN KEY (setting_id) REFERENCES tr_httpblk_settings;

ALTER TABLE tr_httpblk_evt_blk ADD CONSTRAINT FKD760FA7E1F20A4EB FOREIGN KEY (request_id) REFERENCES tr_http_req_line ON DELETE CASCADE;

ALTER TABLE tr_httpblk_mime_types ADD CONSTRAINT FKF4BA8C351CAE658A FOREIGN KEY (setting_id) REFERENCES tr_httpblk_settings;

ALTER TABLE tr_httpblk_mime_types ADD CONSTRAINT FKF4BA8C35871AAD3E FOREIGN KEY (rule_id) REFERENCES mimetype_rule;

ALTER TABLE tr_httpblk_passed_clients ADD CONSTRAINT FKFB0B65401CAE658A FOREIGN KEY (setting_id) REFERENCES tr_httpblk_settings;

ALTER TABLE tr_httpblk_passed_clients ADD CONSTRAINT FKFB0B6540871AAD3E FOREIGN KEY (rule_id) REFERENCES ipmaddr_rule;

ALTER TABLE tr_httpblk_blocked_urls ADD CONSTRAINT FK804E415E871AAD3E FOREIGN KEY (rule_id) REFERENCES string_rule;

ALTER TABLE tr_httpblk_blocked_urls ADD CONSTRAINT FK804E415E1CAE658A FOREIGN KEY (setting_id) REFERENCES tr_httpblk_settings;

ALTER TABLE tr_httpblk_blcat ADD CONSTRAINT FKA0680F251CAE658A FOREIGN KEY (setting_id) REFERENCES tr_httpblk_settings;
