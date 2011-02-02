-- settings conversion for release-4.0
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

-- com.untangle.mvvm.engine.PackageState
CREATE TABLE settings.mackage_state (
    id int8 NOT NULL,
    mackage_name text NOT NULL,
    extra_name text,
    enabled bool NOT NULL,
    PRIMARY KEY (id));

------ Added for Portal

-- com.untangle.mvvm.addrbook.RepositorySettings
DROP TABLE settings.ab_repository_settings;
CREATE TABLE settings.ab_repository_settings (
    settings_id int8 NOT NULL,
    superuser text,
    superuser_pass text,
    domain text,
    ldap_host text,
    ou_filter text,
    port int4,
    PRIMARY KEY (settings_id));

-- com.untangle.mvvm.addrbook.AddressBookSettings
DROP TABLE settings.ab_settings;
CREATE TABLE settings.ab_settings (
    settings_id int8 NOT NULL,
    ad_repo_settings int8 NOT NULL,
    ab_configuration char(1) NOT NULL,
    PRIMARY KEY (settings_id));


-- com.untangle.mvvm.portal.Bookmark -- 4.0
CREATE TABLE settings.portal_bookmark (
        id               INT8 NOT NULL,
        name             TEXT,
        target           TEXT,
        application_name TEXT,
        PRIMARY KEY      (id));

-- com.untangle.mvvm.portal.PortalUser -- 4.0
CREATE TABLE settings.portal_user (
        id               INT8 NOT NULL,
        uid              TEXT,
        live             BOOL,
        description      TEXT,
        group_id         INT8,
        home_settings_id INT8,
        settings_id      INT8,
        position         INT4,
        PRIMARY KEY      (id));

-- com.untangle.mvvm.portal.PortalUser.bookmarks -- 4.0
CREATE TABLE settings.portal_user_bm_mt (
    settings_id int8 NOT NULL,
    bookmark_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (settings_id, position));

-- com.untangle.mvvm.portal.PortalGroup -- 4.0
CREATE TABLE settings.portal_group (
        id               INT8 NOT NULL,
        name             TEXT,
        description      TEXT,
        home_settings_id INT8,
        settings_id      INT8,
        position         INT4,
        PRIMARY KEY      (id));

-- com.untangle.mvvm.portal.PortalGroup.bookmarks -- 4.0
CREATE TABLE settings.portal_group_bm_mt (
    settings_id int8 NOT NULL,
    bookmark_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (settings_id, position));

-- com.untangle.mvvm.portal.PortalGlobal -- 4.0
CREATE TABLE settings.portal_global (
        id               INT8 NOT NULL,
        auto_create_users BOOL,
        login_page_title TEXT,
        login_page_text  TEXT,
        home_settings_id INT8,
        PRIMARY KEY      (id));

-- com.untangle.mvvm.portal.PortalGlobal.bookmarks -- 4.0
CREATE TABLE settings.portal_global_bm_mt (
    settings_id int8 NOT NULL,
    bookmark_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (settings_id, position));

-- com.untangle.mvvm.security.PortalHomeSettings
CREATE TABLE settings.portal_home_settings (
    id              INT8 NOT NULL,
    home_page_title TEXT,
    home_page_text  TEXT,
    bookmark_table_title TEXT,
    show_exploder   BOOL,
    show_bookmarks  BOOL,
    show_add_bookmark BOOL,
    idle_timeout    INT8,
    PRIMARY KEY (id));

-- com.untangle.mvvm.security.PortalSettings
CREATE TABLE settings.portal_settings (
    id int8 NOT NULL,
    global_settings_id INT8,
    PRIMARY KEY (id));

ALTER TABLE settings.portal_group
    ADD CONSTRAINT fk_portal_group_parent
    FOREIGN KEY (settings_id) REFERENCES settings.portal_settings;

ALTER TABLE settings.portal_user
    ADD CONSTRAINT fk_portal_user_parent
    FOREIGN KEY (settings_id) REFERENCES settings.portal_settings;

-- com.untangle.mvvm.networking.RedirectRule
-- Need to insert a column for is_local_redirect
ALTER TABLE settings.mvvm_redirect_rule ADD COLUMN is_local_redirect BOOL;
UPDATE settings.mvvm_redirect_rule SET is_local_redirect = false;

-- Need to insert a column for has user completed wizard
ALTER TABLE settings.mvvm_network_settings ADD COLUMN completed_setup BOOL;
UPDATE settings.mvvm_network_settings SET completed_setup = true;

-- add use_mx_records to MailSettings

ALTER TABLE settings.mail_settings ADD COLUMN use_mx_records bool;
UPDATE settings.mail_settings SET use_mx_records = false;
UPDATE settings.mail_settings SET use_mx_records = true WHERE smtp_host IS NULL;
UPDATE settings.mail_settings SET use_mx_records = true WHERE smtp_host = '';
ALTER TABLE settings.mail_settings ALTER COLUMN use_mx_records SET NOT NULL;
