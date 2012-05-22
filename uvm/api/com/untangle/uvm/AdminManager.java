/**
 * $Id: AdminManager.java,v 1.00 2011/12/08 16:37:33 dmorris Exp $
 */
package com.untangle.uvm;

import java.util.Date;
import java.util.TimeZone;

import javax.transaction.TransactionRolledbackException;

import com.untangle.uvm.MailSettings;

/**
 * Describe interface <code>AdminManager</code> here.
 */
public interface AdminManager
{
    AdminSettings getSettings();

    void setSettings(AdminSettings settings);

    /**
     * Returns the time zone that the UVM is currently set to
     */
    TimeZone getTimeZone();

    /**
     * Sets the time zone that the UVM is in.
     */
    void setTimeZone(TimeZone timezone);

    /**
     * Returns the current time that the UVM is set to
     * Returned as a string to avoid any browser interpretation (with its own timezone)
     */
    String getDate();

    String getAlpacaNonce();

    String getModificationState();

    String getRebootCount();

    String getFullVersionAndRevision();

    String getKernelVersion();
    
}
