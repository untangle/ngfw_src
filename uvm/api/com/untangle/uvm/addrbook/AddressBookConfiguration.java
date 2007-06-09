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

package com.untangle.uvm.addrbook;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * Hibernate-friendly enum of the different types
 * of AddressBook configurations.
 */
public class AddressBookConfiguration
    implements Serializable {

    private static final long serialVersionUID = 1398959017723161857L;

    private static final Map INSTANCES = new HashMap();

    protected static final char none_c = 'N';
    protected static final char local_c = 'L';
    protected static final char both_c = 'B';
    protected static final String none_s = "Not Configured";
    protected static final String local_s = "Local-Only";
    protected static final String both_s = "Local and Active Directory";

    /**
     * The no-configuration state for the address book
     */
    public static final AddressBookConfiguration NOT_CONFIGURED =
        new AddressBookConfiguration(none_c, none_s);

    /**
     * The state when the AddressBook is configured to only
     * use a local repository
     */
    public static final AddressBookConfiguration LOCAL_ONLY =
        new AddressBookConfiguration(local_c, local_s);

    /**
     * The state when the AddressBook is using both a local repository
     * as well as a remote Active Directory server.
     */
    public static final AddressBookConfiguration AD_AND_LOCAL =
        new AddressBookConfiguration(both_c, both_s);


    static {
        INSTANCES.put(NOT_CONFIGURED.getKey(), NOT_CONFIGURED);
        INSTANCES.put(LOCAL_ONLY.getKey(), LOCAL_ONLY);
        INSTANCES.put(AD_AND_LOCAL.getKey(), AD_AND_LOCAL);
    }

    private final String name;
    private final char key;

    protected AddressBookConfiguration(char key, String name) {
        this.key = key;
        this.name = name;
    }

    public static AddressBookConfiguration getInstance(char key){
        return (AddressBookConfiguration)INSTANCES.get(key);
    }

    public static AddressBookConfiguration getInstance(String name)
    {
        AddressBookConfiguration a;
        for (Iterator i = INSTANCES.keySet().iterator(); true == i.hasNext(); ) {
            a = (AddressBookConfiguration)INSTANCES.get(i.next());
            if (name.equals(a.getName())) {
                return a;
            }
        }
        return null;
    }

    public String toString()
    {
        return name;
    }

    public char getKey()
    {
        return key;
    }

    public String getName()
    {
        return name;
    }

    Object readResolve()
    {
        return getInstance(key);
    }

    public static AddressBookConfiguration[] getValues()
    {
        AddressBookConfiguration[] azMsgAction = new AddressBookConfiguration[INSTANCES.size()];
        Iterator iter = INSTANCES.keySet().iterator();
        AddressBookConfiguration zMsgAction;
        for (int i = 0; true == iter.hasNext(); i++) {
            zMsgAction = (AddressBookConfiguration)INSTANCES.get(iter.next());
            azMsgAction[i] = zMsgAction;
        }
        return azMsgAction;
    }
}
