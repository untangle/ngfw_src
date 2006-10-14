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

package com.metavize.mvvm.user;

import java.net.InetAddress;

public interface PhoneBook
{
    /* Lookup the corresponding user user information object user the address */
    public UserInfo lookup( InetAddress address );

    /* Push an update to the phonebook, use this if an assistant gains new information
     * about an active entry outside out of band */
    public void updateEntry( UserInfo info );

    /* Register a phone book assistant which is used to help with addres lookups */
    public void registerAssistant( Assistant assistant );

    /* Unregister a phone book assistant */
    public void unregisterAssistant( Assistant assistant );
}