/**
 * $Id: HookManager.java,v 1.00 2016/10/28 11:46:12 dmorris Exp $
 */
package com.untangle.uvm;

import java.io.File;

public interface HookManager
{
    public static String NETWORK_SETTINGS_CHANGE = "network-settings-change";
    public static String REPORTS_EVENT_LOGGED = "reports-event-logged";
    public static String LICENSE_CHANGE = "license-change";
    public static String UVM_STARTUP_COMPLETE = "uvm-startup-complete";
    public static String UVM_PRE_UPGRADE = "uvm-pre-upgrade";
    public static String UVM_SETTINGS_CHANGE = "uvm-settings-change";

    public boolean registerCallback( String groupName, HookCallback callback );

    public boolean unregisterCallback( String groupName, HookCallback callback );

    public int callCallbacks( String hookName, Object o );
}
