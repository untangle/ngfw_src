/*
 * $Id: AdminManager.java,v 1.00 2011/12/08 16:37:33 dmorris Exp $
 */
package com.untangle.uvm;

import java.util.Date;
import java.util.TimeZone;

import javax.transaction.TransactionRolledbackException;

import com.untangle.uvm.MailSettings;
import com.untangle.uvm.snmp.SnmpManager;

/**
 * Describe interface <code>AdminManager</code> here.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public interface AdminManager
{
    AdminSettings getAdminSettings();

    void setAdminSettings(AdminSettings settings)
        throws TransactionRolledbackException;

    MailSettings getMailSettings();

    void setMailSettings(MailSettings settings)
        throws TransactionRolledbackException;

    /**
     * See MailSender for documentation.
     */
    boolean sendTestMessage(String recipient);

    /**
     * Returns the time zone that the UVM is currently set to
     *
     * @return the <code>TimeZone</code> that the UVM is now in
     */
    TimeZone getTimeZone();

    /**
     * Sets the time zone that the UVM is in.
     *
     * @param timezone a <code>TimeZone</code> value
     * @exception TransactionRolledbackException if an error occurs
     */
    void setTimeZone(TimeZone timezone) throws TransactionRolledbackException;

    /**
     * Returns the current time that the UVM is set to
     * Returned as a string to avoid any browser interpretation (with its own timezone)
     *
     * @return a <code>Date</code> giving the current time as seen by
     * Untangle Server.
     */
    String getDate();

    /**
     * Access the singleton responsible for
     * managing SNMP in this instance
     */
    SnmpManager getSnmpManager();

    String getAlpacaNonce();

    String getModificationState();

    String getRebootCount();

    String getFullVersionAndRevision();

    String getKernelVersion();
    
}
