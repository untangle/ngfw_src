-- events conversion for release-4.0

-- com.untangle.mvvm.portal.PortalLoginEvent
CREATE TABLE events.portal_login_evt (
    event_id    int8 NOT NULL,
    client_addr inet,
    uid         text,
    succeeded   bool,
    reason      char(1),
    time_stamp  timestamp,
    PRIMARY KEY (event_id));

-- com.untangle.mvvm.portal.PortalLogoutEvent
CREATE TABLE events.portal_logout_evt (
    event_id    int8 NOT NULL,
    client_addr inet,
    uid         text,
    reason      char(1),
    time_stamp  timestamp,
    PRIMARY KEY (event_id));

-- com.untangle.mvvm.portal.PortalAppLaunchEvent
CREATE TABLE events.portal_app_launch_evt (
    event_id    int8 NOT NULL,
    client_addr inet,
    uid         text,
    succeeded   bool,
    reason      char(1),
    app         text,
    destination text,
    time_stamp  timestamp,
    PRIMARY KEY (event_id));
