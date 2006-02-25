/*
 * Copyright (c) 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.security;

import java.util.TimeZone;
import javax.transaction.TransactionRolledbackException;

import com.metavize.mvvm.MailSettings;
import com.metavize.mvvm.snmp.SnmpManager;

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

    // See MailSender for documentation.
    boolean sendTestMessage(String recipient);

    LoginSession[] loggedInUsers();

    void logout();

    LoginSession whoAmI();

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
    void setTimeZone(TimeZone timezone) throws TransactionRolledbackException;

    /**
     * Sets the registration info for the box customer.  The new info
     * will be transmitted to Metavize automatically by cron.
     *
     * @param info a <code>RegistrationInfo</code> giving the new
     * registration info for the customer
     */
    void setRegistrationInfo(RegistrationInfo info)
        throws TransactionRolledbackException;

    /**
     * Returns the registration info previously set, or null if
     * {@link #setRegistrationInfo setRegistrationInfo} has never been set
     *
     * @return the reg info
     */
    RegistrationInfo getRegistrationInfo();

    /**
     * Access the singleton responsible for
     * managing SNMP in this instance
     */
    SnmpManager getSnmpManager();

    /**
     * Returns a nonce to be added to the URL when you want to
     * auto-login to a Tomcat servlet as the current UI user.  The
     * String returned is of the form: 'nonce=resultofthisfunction'
     * and should be stuck into the URL's querstring.
     *
     * Note that this is a single use nonce.
     *
     * @return the nonce to be added to the query string
     */
    String generateAuthNonce();
}
