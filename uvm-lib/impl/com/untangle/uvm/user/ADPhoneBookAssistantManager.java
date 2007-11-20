/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.user;

import com.untangle.uvm.user.ADPhoneBookAssistant;
import com.untangle.uvm.user.LocalADPhoneBookAssistantImpl;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;

import com.untangle.node.util.UtLogger;

/**
 * Singleton class for ADPhoneBookAssistant, also handles registering the assistant
 * @author Thomas Belote
 */
public class ADPhoneBookAssistantManager
{

    private static final String PROPERTY_ADPHONEBOOKASSISTANT_IMPL = "com.untangle.uvm.adphonebookassistant";
    private static final String PREMIUM_ADPHONEBOOKASSISTANT_IMPL = "com.untangle.uvm.user.ADPhoneBookAssistantImpl";

    private static ADPhoneBookAssistant assistant = null;
    private static ADPhoneBookAssistant premium = null;

    private ADPhoneBookAssistantManager()
    {
    }

    public static ADPhoneBookAssistant getADPhoneBookAssistant()
    {
	refresh();
	if (premium != null) {
	    return premium;
	}
	if (assistant == null) {
	    assistant = new LocalADPhoneBookAssistantImpl();
	}
	return assistant;
    }

    public static void refresh()
    {
        if ( premium != null ) {
            return;
        }

        String className = System.getProperty( PROPERTY_ADPHONEBOOKASSISTANT_IMPL );
        if ( null == className ) {
            className = PREMIUM_ADPHONEBOOKASSISTANT_IMPL;
        }
        try {
            premium = (ADPhoneBookAssistant)Class.forName( className ).newInstance();
	    /* register the assistant with the phonebook */
	    LocalUvmContextFactory.context().localPhoneBook().registerAssistant( premium );
        } catch ( Exception e ) {
            premium = null;
        }
    }

    public static void init()
    {
    }

    public static void destroy()
    {
	premium = null;
	assistant = null;
    }
}
