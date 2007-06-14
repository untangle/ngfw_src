/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
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
