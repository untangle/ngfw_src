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
import javax.transaction.TransactionRolledbackException;
import java.util.TimeZone;

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
}
