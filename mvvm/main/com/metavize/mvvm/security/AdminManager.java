/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.security;

import com.metavize.mvvm.MailSettings;
import com.metavize.mvvm.snmp.SnmpManager;
import javax.transaction.TransactionRolledbackException;
import java.util.TimeZone;

/**
 * Describe interface <code>AdminManager</code> here.
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
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

    LoginSession[] loggedInUsers();

    /**
     * Returns the time zone that the MVVM is currently set to
     *
     * @return the <code>TimeZone</code> that the MVVM is now in
     */
    TimeZone getTimeZone();

    /**
     * Sets the time zone that the MVVM is in.
     *
     * @param timezone a <code>TimeZone</code> value
     * @exception TransactionRolledbackException if an error occurs
     */
    void setTimeZone(TimeZone timezone)
        throws TransactionRolledbackException;

    /**
     * Sets the registration info for the box customer.  The new info will be transmitted to Metavize
     * automatically by cron.
     *
     * @param info a <code>RegistrationInfo</code> giving the new registration info for the customer
     */
    void setRegistrationInfo(RegistrationInfo info)
        throws TransactionRolledbackException;

    /**
     * Access the singleton responsible for
     * managing SNMP in this instance
     */
    SnmpManager getSnmpManager();
}
