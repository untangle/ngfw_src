/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.uvm.user;

import java.net.InetAddress;

public interface LocalPhoneBook extends RemotePhoneBook
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