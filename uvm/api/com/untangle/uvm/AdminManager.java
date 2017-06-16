/**
 * $Id$
 */
package com.untangle.uvm;

import java.net.InetAddress;

/**
 * Describe interface <code>AdminManager</code> here.
 */
public interface AdminManager
{
    AdminSettings getSettings();

    void setSettings(AdminSettings settings);

    String getModificationState();

    String getRebootCount();

    String getFullVersionAndRevision();

    String getKernelVersion();
    
    String getAdminEmail();

    void logAdminLoginEvent( String login, boolean local, InetAddress clientAddress, boolean succeeded, String reason );
}
