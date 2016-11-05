/**
 * $Id: HookManager.java,v 1.00 2016/10/28 11:46:12 dmorris Exp $
 */
package com.untangle.uvm;

import java.io.File;

public interface HookManager
{
    public static String NETWORK_SETTINGS_CHANGE = "network-settings-change";
    public static String REPORTS_EVENT_LOGGED = "reports-event-logged";

    public boolean registerCallback( String groupName, HookCallback callback );

    public boolean unregisterCallback( String groupName, HookCallback callback );

    public int callCallbacks( String hookName, Object o );
}
