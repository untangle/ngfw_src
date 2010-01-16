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

package com.untangle.uvm.engine;

import com.untangle.uvm.user.ADPhoneBookAssistant;
import com.untangle.uvm.user.LocalADPhoneBookAssistantImpl;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.engine.UvmContextImpl;
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
    private static final String PREMIUM_WEBAPP = "adpb";

    private static ADPhoneBookAssistant assistant = null;
    private static ADPhoneBookAssistant standard = new LocalADPhoneBookAssistantImpl();
    private static ADPhoneBookAssistant premium = null;

    private static UtLogger logger = null;

    private ADPhoneBookAssistantManager()
    {
    }

    public static ADPhoneBookAssistant getADPhoneBookAssistant()
    {
        if (logger == null) {
            logger = new UtLogger( ADPhoneBookAssistantManager.class );
        }
        logger.debug("getADPhoneBookAssistant called");
        refresh();
        logger.debug("getADPhoneBookAssistant refresh done");
        if (premium != null) {
            return premium;
        }
        if (assistant == null) {
            return standard;
        }
        return assistant;
    }

    public static void refresh()
    {
        logger.debug("getADPhoneBookAssistant refresh started");
        if ( premium != null ) {
            return;
        }

        String className = System.getProperty( PROPERTY_ADPHONEBOOKASSISTANT_IMPL );
        if ( null == className ) {
            className = PREMIUM_ADPHONEBOOKASSISTANT_IMPL;
        }
        try {
            premium = (ADPhoneBookAssistant)Class.forName( className ).newInstance();
            logger.debug("getADPhoneBookAssistant loaded premium");
            logger.debug("getADPhoneBookAssistant loading the webapp");
            UvmContextImpl.getInstance().tomcatManager().loadInsecureApp("/"+PREMIUM_WEBAPP, PREMIUM_WEBAPP);
            logger.debug("getADPhoneBookAssistant done loading the webapp");
            /* register the assistant with the phonebook */
            LocalUvmContextFactory.context().localPhoneBook().registerAssistant( premium );
            logger.debug("getADPhoneBookAssistant registering assistant");
        } catch ( Exception e ) {
            premium = null;
            logger.info("getADPhoneBookAssistant " + e.toString());
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
