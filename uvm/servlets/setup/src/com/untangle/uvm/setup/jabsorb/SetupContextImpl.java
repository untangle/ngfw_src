/* $HeadURL$ */
package com.untangle.uvm.setup.jabsorb;

import java.util.TimeZone;

import javax.transaction.TransactionRolledbackException;

import org.apache.log4j.Logger;

import com.untangle.uvm.LanguageSettings;
import com.untangle.uvm.LanguageManager;
import com.untangle.uvm.AdminManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.AdminSettings;
import com.untangle.uvm.AdminUserSettings;
import com.untangle.uvm.util.JsonClient;

public class SetupContextImpl implements UtJsonRpcServlet.SetupContext
{
    private final Logger logger = Logger.getLogger( this.getClass());

    private UvmContext context;

    /* Shamelessly lifted from AdminManagerImpl */
    private static final String INITIAL_USER_NAME = "System Administrator";
    private static final String INITIAL_USER_LOGIN = "admin";

    private SetupContextImpl( UvmContext context )
    {
        this.context = context;
    }

    public void setLanguage( String language )
    {
        LanguageManager lm = this.context.languageManager();
        LanguageSettings ls = lm.getLanguageSettings();
        ls.setLanguage( language );
        lm.setLanguageSettings( ls );
    }
    
    public void setAdminPassword( String password ) throws TransactionRolledbackException
    {
        AdminSettings adminSettings = this.context.adminManager().getSettings();
        AdminUserSettings admin = null;

        /**
         * Find the "admin" user
         */
        for ( AdminUserSettings user : adminSettings.getUsers()) {
            if ( INITIAL_USER_LOGIN.equals( user.getUsername())) {
                admin = user;
                break;
            }
        }

        /**
         * If not found, create it, otherwise just set the existing admin user's password
         */
        if ( admin == null ) {
            admin = new AdminUserSettings( INITIAL_USER_LOGIN, password, INITIAL_USER_NAME );
            adminSettings.addUser( admin );
        } else {
            admin.setPassword( password );
        }

        this.context.adminManager().setSettings( adminSettings );
    }
    
    public void setTimeZone( TimeZone timeZone ) throws TransactionRolledbackException
    {
        this.context.adminManager().setTimeZone( timeZone );
    }

    public String getTimeZones()
    {
        return this.context.adminManager().getTimeZones( );
    }
    
    public String getOemName()
    {
        return this.context.oemManager().getOemName();
    }

    public static UtJsonRpcServlet.SetupContext makeSetupContext()
    {
        UvmContext uvm = UvmContextFactory.context();
        return new SetupContextImpl( uvm );
    }
}
