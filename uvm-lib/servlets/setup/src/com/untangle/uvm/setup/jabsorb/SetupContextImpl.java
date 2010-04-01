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

package com.untangle.uvm.setup.jabsorb;

import java.util.TimeZone;

import javax.transaction.TransactionRolledbackException;

import com.untangle.uvm.LanguageSettings;
import com.untangle.uvm.RemoteLanguageManager;
import com.untangle.uvm.client.RemoteUvmContextFactory;
import com.untangle.uvm.client.RemoteUvmContext;
import com.untangle.uvm.security.AdminSettings;
import com.untangle.uvm.security.RemoteAdminManager;
import com.untangle.uvm.security.User;

public class SetupContextImpl implements UtJsonRpcServlet.SetupContext
{
    private RemoteUvmContext context;

    /* Shamelessly lifted from AdminManagerImpl */
    private static final String INITIAL_USER_NAME = "System Administrator";
    private static final String INITIAL_USER_LOGIN = "admin";

    private SetupContextImpl( RemoteUvmContext context )
    {
        this.context = context;
    }

    public void setLanguage( String language )
    {
        RemoteLanguageManager lm = this.context.languageManager();
        LanguageSettings ls = lm.getLanguageSettings();
        ls.setLanguage( language );
        lm.setLanguageSettings( ls );
    }
    
    public void setAdminPassword( String password ) throws TransactionRolledbackException
    {
        RemoteAdminManager am = this.context.adminManager();
        AdminSettings as = am.getAdminSettings();
        User admin = null;

        for ( User user : as.getUsers()) {
            if ( INITIAL_USER_LOGIN.equals( user.getLogin())) {
                admin = user;
                break;
            }
        }

        if ( admin == null ) {
            admin = new User( INITIAL_USER_LOGIN, password, INITIAL_USER_NAME );
            as.addUser( admin );
        } else {
            admin.setClearPassword( password );
        }

        am.setAdminSettings( as );
    }
    
    public void setTimeZone( TimeZone timeZone ) throws TransactionRolledbackException
    {
        this.context.adminManager().setTimeZone( timeZone );
    }

    public static UtJsonRpcServlet.SetupContext makeSetupContext()
    {
        RemoteUvmContext uvm = RemoteUvmContextFactory.context();
        return new SetupContextImpl( uvm );
    }
}
