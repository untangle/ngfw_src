/**
 * $Id: HookManager.java,v 1.00 2016/10/28 11:46:12 dmorris Exp $
 */
package com.untangle.uvm;

public interface HookManager
{
    public static String SETTINGS_CHANGE = "settings-change";
    public static String NETWORK_SETTINGS_CHANGE = "network-settings-change";
    public static String REPORTS_EVENT_LOGGED = "reports-event-logged";
    public static String LICENSE_CHANGE = "license-change";
    public static String UVM_STARTUP_COMPLETE = "uvm-startup-complete";
    public static String UVM_PRE_UPGRADE = "uvm-pre-upgrade";
    public static String UVM_SETTINGS_CHANGE = "uvm-settings-change";
    public static String HOST_TABLE_REMOVE = "host-table-remove";
    public static String HOST_TABLE_ADD = "host-table-add";
    public static String HOST_TABLE_TAGGED = "host-table-tagged";
    public static String HOST_TABLE_QUOTA_GIVEN = "host-table-quota-given";
    public static String HOST_TABLE_QUOTA_EXCEEDED = "host-table-quota-exceeded";
    public static String HOST_TABLE_QUOTA_REMOVED = "host-table-quota-removed";
    public static String HOST_TABLE_RESUME_TAG = "host-table-resume-tag";
    public static String HOST_TABLE_ADD_TAG = "host-table-add-tag";
    public static String HOST_TABLE_REMOVE_TAG = "host-table-remove-tag";
    public static String USER_TABLE_REMOVE = "user-table-remove";
    public static String USER_TABLE_ADD = "user-table-add";
    public static String USER_TABLE_QUOTA_GIVEN = "user-table-quota-given";
    public static String USER_TABLE_QUOTA_EXCEEDED = "user-table-quota-exceeded";
    public static String USER_TABLE_QUOTA_REMOVED = "user-table-quota-removed";
    public static String CAPTURE_USERNAME_CHECK = "capture-username-check";
    public static String WEBFILTER_BASE_CATEGORIZE_SITE = "webfilter-base-categorize-site";
    
    public boolean isRegistered( String hookName, HookCallback callback );

    public boolean registerCallback( String groupName, HookCallback callback );

    public boolean unregisterCallback( String groupName, HookCallback callback );

    public void callCallbacks( String hookName, Object... arguments );

    public int callCallbacksSynchronous( String hookName, Object... arguments );

    public boolean hooksExist(String hookName);
}
