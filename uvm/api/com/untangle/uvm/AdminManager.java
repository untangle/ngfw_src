/**
 * $Id$
 */
package com.untangle.uvm;

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

}
