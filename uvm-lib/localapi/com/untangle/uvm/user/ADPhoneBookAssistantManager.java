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
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;

import com.untangle.node.util.UtLogger;

/**
 * Singleton class for ADPhoneBookAssistant, also handles registering the assistant
 * @author Thomas Belote
 */
public class ADPhoneBookAssistantManager
{
    private final UtLogger logger = new UtLogger( getClass());

    private static ADPhoneBookAssistant assistant = null;

    private ADPhoneBookAssistantManager()
    {
    }

    public static ADPhoneBookAssistant getADPhoneBookAssistant()
    {
	if (assistant == null) {
	    assistant = new ADPhoneBookAssistant();
	    /* register the assistant with the phonebook */
	    LocalUvmContextFactory.context().localPhoneBook().registerAssistant( assistant );
	}
	return assistant;
    }

    public void refresh()
    {
    }

    public void init()
    {
    }

    public void destroy()
    {
    }

    //public static ADPhoneBookAssistantManager makeInstance()
    //{
    //    ADPhoneBookAssistantManager factory = new ADPhoneBookAssistantManager();
        //factory.refresh();
    //    return factory;
    //}
}
