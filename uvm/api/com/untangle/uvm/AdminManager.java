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
    /**
     * Return administrator settings.
     * 
     * @return
     *     AdminSettings object.
     */
    AdminSettings getSettings();

    /**
     * Write administrator settings.
     * 
     * @param settings
     *     AdminSettings object.
     */
    void setSettings(AdminSettings settings);

    /**
     * Return whether sytem has been modified from command line.
     * 
     * @return
     *     String of the following values:
     *     none         No history file exists.
     *     blessed      History exists but has been approved.
     *     yes (count)  History file exists and the count of imtems in it.
     */
    String getModificationState();

    /**
     * Return number of reboots and crashes
     * 
     * @return
     *     String of the format "reboot_count (crash_count)"
     */
    String getRebootCount();

    /**
     * Return system version.
     * 
     * @return
     *     String of the ngfw version.  If developer system, this will be "DEVEL_VERSION".
     */
    String getFullVersionAndRevision();

    /**
     * Return Linux kernel version.
     * 
     * @return
     *     String of the Linux kernel version.
     */
    String getKernelVersion();
    
    /**
     * Return email address of administrator (admin) user.
     * 
     * @return
     *     String of email address.
     */
    String getAdminEmail();

    /**
     * Send adminstrator login result to event log.
     * 
     * @param login
     *        String username
     * @param local
     *        boolean.  If true, from 127.0.0.0, false otherwise.
     * @param clientAddress 
     *        Inet address.  Client login address.
     * @param succeeded
     *        boolean.  If true, login was succesful.  Otherwise false.
     * @param reason
     *        String.  Reason for login failure.
     */
    void logAdminLoginEvent( String login, boolean local, InetAddress clientAddress, boolean succeeded, String reason );
}
