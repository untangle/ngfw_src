/* $HeadURL$ */
package com.untangle.uvm;

import java.io.IOException;

import com.untangle.uvm.MailSender;
import com.untangle.uvm.RemoteAppServerManager;
import com.untangle.uvm.RemoteBrandingManager;
import com.untangle.uvm.RemoteConnectivityTester;
import com.untangle.uvm.LanguageManager;
import com.untangle.uvm.NetworkManager;
import com.untangle.uvm.SkinManager;
import com.untangle.uvm.RemoteOemManager;
import com.untangle.uvm.SessionMonitor;
import com.untangle.uvm.AdminManager;
import com.untangle.uvm.node.LicenseManager;
import com.untangle.uvm.logging.RemoteLoggingManager;
import com.untangle.uvm.message.MessageManager;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.policy.PolicyManager;
import com.untangle.uvm.reports.RemoteReportingManager;
import com.untangle.uvm.toolbox.ToolboxManager;

/**
 * Provides an interface to get major UVM components that are
 * accessible a remote client.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public interface RemoteUvmContext
{
    /**
     * Get the <code>ToolboxManager</code> singleton.
     *
     * @return the ToolboxManager.
     */
    ToolboxManager toolboxManager();

    /**
     * Get the <code>NodeManager</code> singleton.
     *
     * @return the NodeManager.
     */
    NodeManager nodeManager();

    /**
     * Get the <code>RemoteLoggingManager</code> singleton.
     *
     * @return the RemoteLoggingManager.
     */
    RemoteLoggingManager loggingManager();

    /**
     * Get the <code>PolicyManager</code> singleton.
     *
     * @return the PolicyManager.
     */
    PolicyManager policyManager();

    /**
     * Get the <code>RemoteAdminManager</code> singleton.
     *
     * @return the RemoteAdminManager.
     */
    AdminManager adminManager();

    /**
     * Get the <code>NetworkManager</code> singleton.
     *
     * @return the NetworkManager.
     */
    NetworkManager networkManager();

    /**
     * Get the <code>RemoteReportingManager</code> singleton.
     *
     * @return the RemoteReportingManager.
     */
    RemoteReportingManager reportingManager();

    /**
     * Get the <code>RemoteConnectivityTester</code> singleton.
     *
     * @return the RemoteConnectivityTester
     */
    RemoteConnectivityTester getRemoteConnectivityTester();

    /**
     * get the <code>MailSender</code> - Used for sending mail
     *
     * @return the MailSender
     */
    MailSender mailSender();

    /**
     * Get the AppServerManager singleton for this instance
     *
     * @return the singleton
     */
    RemoteAppServerManager appServerManager();

    /**
     * The LocalDirectory for managing/authenticating users
     *
     * @return the local directory
     */
    LocalDirectory localDirectory();
    
    /**
     * The BrandingManager allows for customization of logo and
     * branding information.
     *
     * @return the BrandingManager.
     */
    RemoteBrandingManager brandingManager();

    /**
     * Get the <code>SkinManager</code> singleton.
     *
     * @return the SkinManager.
     */
    SkinManager skinManager();

    /**
     * Get the <code>MessageManager</code> singleton.
     *
     * @return the MessageManager
     */
    MessageManager messageManager();

    /**
     * Get the <code>LanguageManager</code> singleton.
     *
     * @return the LanguageManager.
     */
    LanguageManager languageManager();

    /**
     * The license manager.
     *
     * @return the RemoteLicenseManager
     */
    LicenseManager licenseManager();

    /**
     * The session monitor
     * This can be used for getting information about current sessions
     *
     * @return the SessionMonitor
     */
    SessionMonitor sessionMonitor();

    /**
     * Get the <code>RemoteOemManager</code> singleton.
     *
     * @return the RemoteOemManager.
     */
    RemoteOemManager oemManager();

    /**
     * Shut down the untangle-vm
     *
     */
    void shutdown();

    /**
     * Once settings have been restored, and the UVM has been booted, call
     * into here to get the corresponding OS files rewritten.  This calls through
     * into callbacks in each manager, as appropriate.  All managers that write
     * OS config files must implement this.
     */
    void syncConfigFiles();

    /**
     * Reboots the Untangle Server. Note that this currently will not reboot a
     * dev box.
     */
    void rebootBox();

    /**
     * Shutdown the Untangle Server
     */
    void shutdownBox();

    /**
     * Force a Full Garbage Collection.
     */   
    void doFullGC();

    /**
     * Return the Version
     */
    String version();

    /**
     * Return true if running in a development environment.
     *
     * @return a <code>boolean</code> true if in development.
     */
    boolean isDevel();

    /**
     * Return true if running inside a Virtualized Platform (like VMWare)
     *
     * @return a <code>boolean</code> true if platform is running in a
     * virtualized machine
     */
    boolean isInsideVM();

    /**
     * Create a backup which the client can save to a local disk.  The
     * returned bytes are for a .tar.gz file, so it is a good idea to
     * either use a ".tar.gz" extension so basic validation can be
     * performed for {@link #restoreBackup restoreBackup}.
     *
     * @return the byte[] contents of the backup.
     *
     * @exception IOException if something goes wrong (a lot can go wrong,
     *            but it is nothing the user did to cause this).
     */
    byte[] createBackup() throws IOException;

    /**
     * Restore from a previous {@link #createBackup backup}.
     *
     * @exception IOException something went wrong to prevent the
     *            restore (not the user's fault).
     * @exception IllegalArgumentException if the provided bytes do not seem
     *            to have come from a valid backup (is the user's fault).
     */
    void restoreBackup(byte[] backupFileBytes)
        throws IOException, IllegalArgumentException;

    /**
     * Restore from a previous {@link #createBackup backup} from a given file.
     *
     * @exception IOException something went wrong to prevent the
     *            restore (not the user's fault).
     * @exception IllegalArgumentException if the provided bytes do not seem
     *            to have come from a valid backup (is the user's fault).
     */
    void restoreBackup(String fileName)
        throws IOException, IllegalArgumentException;

    /**
     * create a UID (if one doesn't exist)
     * Called by the setup wizard
     */
    boolean createUID();

    /**
     * mark the wizard as complete
     * Called by the setup wizard
     */
    void wizardComplete();
    
}
