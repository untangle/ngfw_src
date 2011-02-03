/* $HeadURL$*/
package com.untangle.uvm.networking;

import static com.untangle.uvm.networking.ShellFlags.FLAG_HTTPS_EXTERNAL;
import static com.untangle.uvm.networking.ShellFlags.FLAG_HTTPS_RESTRICTED;
import static com.untangle.uvm.networking.ShellFlags.FLAG_HTTPS_RESTRICTED_NET;
import static com.untangle.uvm.networking.ShellFlags.FLAG_HTTPS_RESTRICTED_MASK;
import static com.untangle.uvm.networking.ShellFlags.FLAG_HTTP_INTERNAL;

import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Query;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.node.script.ScriptWriter;
import com.untangle.uvm.util.TransactionWork;

class AccessManagerImpl implements LocalAccessManager
{
    
    /* These are the services keys, each one must be unique */
    private static final String KEY_ADMINISTRATION = "administration";
    private static final String KEY_QUARANTINE = "quarantine";
    private static final String KEY_REPORTING = "reporting";

    private final Logger logger = Logger.getLogger(getClass());

    private AccessSettings accessSettings = null;

    private final Set<String> servicesSet = new HashSet<String>();

    AccessManagerImpl()
    {
    }

    /* Use this to retrieve just the remote settings */
    public AccessSettings getSettings()
    {
        return this.accessSettings;
    }

    /* Use this to mess with the remote settings without modifying the network settings */
    @SuppressWarnings("unchecked")
    public synchronized void setSettings( final AccessSettings settings )
    {
        TransactionWork<Void> tw = new TransactionWork<Void>()
            {
                public boolean doWork(Session s)
                {
                    accessSettings = (AccessSettings)s.merge(settings);
                    return true;
                }
            };
        LocalUvmContextFactory.context().runTransaction(tw);
        
        setSupportAccess( this.accessSettings );
    }

    /* ---------------------- PACKAGE ---------------------- */
    /* Initialize the settings, load at startup */
    synchronized void init()
    {
        TransactionWork<Object> tw = new TransactionWork<Object>()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery( "from " + "AccessSettings");
                    accessSettings = (AccessSettings)q.uniqueResult();
                    
                    return true;
                }
            };

        LocalUvmContextFactory.context().runTransaction(tw);
        
        if ( accessSettings == null ) {
            logger.info( "There are no access settings in the database, must initialize from files." );
            AccessSettings settings = new AccessSettings();

            /* Load the defaults */
            settings.setIsInsideInsecureEnabled( true );
            settings.setIsOutsideAccessEnabled( true );
            settings.setIsOutsideAdministrationEnabled( true );
            settings.setIsOutsideQuarantineEnabled( true );
            settings.setIsOutsideReportingEnabled( false );
            
            setSettings( settings );
        }
    }
    
    /* Invoked at the very end to actually update the settings */
    /* This should also be synchronized from its callers. */
    synchronized void commit( ScriptWriter scriptWriter )
    {
	if ( this.accessSettings == null ) {
	    logger.warn( "Null access settings" );
	    return;
	}
        setServiceStatus( this.accessSettings.getIsOutsideAdministrationEnabled(), KEY_ADMINISTRATION );
        setServiceStatus( this.accessSettings.getIsOutsideQuarantineEnabled(), KEY_QUARANTINE );
        setServiceStatus( this.accessSettings.getIsOutsideReportingEnabled(), KEY_REPORTING );
        
        /* Save the variables that need to be written to networking.sh */
        updateShellScript( scriptWriter, this.accessSettings );
    }


    /** Register a service, used to determine if port 443 should be open or not */
    synchronized void registerService( String name )
    {
        this.servicesSet.add( name );
    }

    /** Register a service, used to determine if port 443 should be open or not */
    synchronized void unregisterService( String name )
    {
        this.servicesSet.remove( name );
    }

    /* ---------------------- PRIVATE ---------------------- */
    private void updateShellScript( ScriptWriter scriptWriter, AccessSettings access )
    {
        if ( access == null ) {
            logger.warn( "unable to save hostname, access settings are not initialized." );            
            return;
        }

        scriptWriter.appendLine("# Is HTTP port open on the internal interface");
        scriptWriter.appendVariable( FLAG_HTTP_INTERNAL, access.getIsInsideInsecureEnabled());

        // HTTPs is automatically opened if there are any services that need it.
        scriptWriter.appendLine("# Is HTTPS port open on the external interface?");
        scriptWriter.appendVariable( FLAG_HTTPS_EXTERNAL, !this.servicesSet.isEmpty());

        scriptWriter.appendLine("# Is HTTPS port open on the external interface restricted (to only some IPs?)");
        scriptWriter.appendVariable( FLAG_HTTPS_RESTRICTED, access.getIsOutsideAccessRestricted());

        IPAddress outsideNetwork = access.getOutsideNetwork();
        IPAddress outsideNetmask = access.getOutsideNetmask();
        
        if (( outsideNetwork != null ) && !outsideNetwork.isEmpty()) {
            scriptWriter.appendLine("# Is HTTPS port restricted to a certain network on external?");
            scriptWriter.appendVariable( FLAG_HTTPS_RESTRICTED_NET, outsideNetwork.toString());
            
            if (( outsideNetmask != null ) && !outsideNetmask.isEmpty()) {
                scriptWriter.appendLine("# Is HTTPS port restricted to a certain netmask on external?");
                scriptWriter.appendVariable( FLAG_HTTPS_RESTRICTED_MASK, outsideNetmask.toString());
            }
        }
    }

    private void setSupportAccess( AccessSettings access )
    {
        if ( access == null ) {
            logger.warn( "unable to set support access, address settings are not initialized." );            
            return;
        }
        
        try {
            if ( access.getIsSupportEnabled()) {
                enableService("untangle-support-agent");
            } else {
                disableService("untangle-support-agent");
            }
        } catch ( Exception ex ) {
            logger.error( "Unable to enable support", ex );
        }
    }

    private void setServiceStatus( boolean status, String key )
    {
        if ( status ) registerService( key );
        else          unregisterService( key );
    }

    private void enableService(String name) 
    {
        try {
            LocalUvmContextFactory.context().toolboxManager().install(name);
        }
        catch (com.untangle.uvm.toolbox.PackageInstallException exc) {
            logger.warn("Failed to install package: " + name, exc );
        }
    }

    private void disableService(String name)
    {
        try {
            LocalUvmContextFactory.context().toolboxManager().uninstall(name);
        }
        catch (com.untangle.uvm.toolbox.PackageUninstallException exc) {
            logger.warn("Failed to uninstall package: " + name, exc );
        }
    }

}


