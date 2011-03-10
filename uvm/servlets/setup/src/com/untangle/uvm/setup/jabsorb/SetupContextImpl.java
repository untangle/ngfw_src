/* $HeadURL$ */
package com.untangle.uvm.setup.jabsorb;

import java.util.TimeZone;

import javax.transaction.TransactionRolledbackException;

import org.apache.log4j.Logger;

import com.untangle.uvm.LanguageSettings;
import com.untangle.uvm.LanguageManager;
import com.untangle.uvm.AdminManager;
import com.untangle.uvm.RemoteUvmContextFactory;
import com.untangle.uvm.RemoteUvmContext;
import com.untangle.uvm.AdminSettings;
import com.untangle.uvm.User;
import com.untangle.uvm.util.JsonClient;
import com.untangle.uvm.util.XMLRPCUtil;

public class SetupContextImpl implements UtJsonRpcServlet.SetupContext
{
    private final Logger logger = Logger.getLogger( this.getClass());

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
        LanguageManager lm = this.context.languageManager();
        LanguageSettings ls = lm.getLanguageSettings();
        ls.setLanguage( language );
        lm.setLanguageSettings( ls );
    }
    
    public void setAdminPassword( String password ) throws TransactionRolledbackException
    {
        AdminManager am = this.context.adminManager();
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

    public String getOemName()
    {
        return this.context.oemManager().getOemName();
    }

    /**
     * On first boot the netConfig.js doesn't get written out after DHCP is done
     * I'm not sure why. However this function is used to re-sync all network settings
     * so that they are correct on the first run of the wizard
     */
    public void refreshNetworkConfig()
    {
        /**
         * First tell alpaca to write the files
         */
        try {
            JsonClient.getInstance().callAlpaca( XMLRPCUtil.CONTROLLER_UVM, "write_files", null );
        } catch ( Exception e ) {
            logger.warn( "Failed to write UVM config files. (net-alpaca returned an error)", e );
        }

        /**
         * Then tell the UVM to re-read the files
         */
        try {
            this.context.networkManager().refreshNetworkConfig();
        } catch ( Exception e ) {
            logger.warn( "Failed to refresh Network Config", e );
        }
        
        return;
    }
    
    public static UtJsonRpcServlet.SetupContext makeSetupContext()
    {
        RemoteUvmContext uvm = RemoteUvmContextFactory.context();
        return new SetupContextImpl( uvm );
    }
}
