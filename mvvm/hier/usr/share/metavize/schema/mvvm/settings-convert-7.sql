-- settings conversion for release-4.0

------ Added for Portal

-- com.metavize.mvvm.portal.Bookmark -- 4.0
CREATE TABLE settings.portal_bookmark (
        id               INT8 NOT NULL,
        name             TEXT,
        target           TEXT,
        application_name TEXT,
        PRIMARY KEY      (id));

-- com.metavize.mvvm.portal.PortalUser -- 4.0
CREATE TABLE settings.portal_user (
        id               INT8 NOT NULL,
        uid              TEXT,
        live             BOOL,
        group_id         INT8,
        home_settings_id INT8,
        PRIMARY KEY      (id));

-- com.metavize.mvvm.portal.PortalUser.bookmarks -- 4.0
CREATE TABLE settings.portal_user_bm_mt (
    settings_id int8 NOT NULL,
    bookmark_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (settings_id, position));

-- com.metavize.mvvm.portal.PortalGroup -- 4.0
CREATE TABLE settings.portal_group (
        id               INT8 NOT NULL,
        name             TEXT,
        home_settings_id INT8,
        PRIMARY KEY      (id));

-- com.metavize.mvvm.portal.PortalGroup.bookmarks -- 4.0
CREATE TABLE settings.portal_group_bm_mt (
    settings_id int8 NOT NULL,
    bookmark_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (settings_id, position));

-- com.metavize.mvvm.portal.PortalGlobal -- 4.0
CREATE TABLE settings.portal_global (
        id               INT8 NOT NULL,
        live             BOOL,
        login_page_title TEXT,
        login_page_text  TEXT,
        home_settings_id INT8,
        PRIMARY KEY      (id));

-- com.metavize.mvvm.portal.PortalGlobal.bookmarks -- 4.0
CREATE TABLE settings.portal_global_bm_mt (
    settings_id int8 NOT NULL,
    bookmark_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (settings_id, position));

-- com.metavize.mvvm.security.PortalHomeSettings
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

-- com.metavize.mvvm.security.PortalSettings
CREATE TABLE settings.portal_settings (
    id int8 NOT NULL,
    global_settings_id INT8,
    PRIMARY KEY (id));
