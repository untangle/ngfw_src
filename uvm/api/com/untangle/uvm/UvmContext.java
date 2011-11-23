/*
 * $Id: UvmContext.java,v 1.00 2011/11/23 13:06:17 dmorris Exp $
 */
package com.untangle.uvm;

import java.io.File;

import java.io.IOException;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.LoggingManager;
import com.untangle.uvm.logging.SyslogManager;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.message.MessageManager;
import com.untangle.uvm.message.MessageManager;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.LicenseManager;
import com.untangle.uvm.policy.PolicyManager;
import com.untangle.uvm.reports.ReportingManager;
import com.untangle.uvm.AdminManager;
import com.untangle.uvm.servlet.UploadManager;
import com.untangle.uvm.toolbox.ToolboxManager;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.PipelineFoundry;
import com.untangle.uvm.LocalTomcatManager;

/**
 * Provides an interface to get all local UVM components from an UVM
 * instance.  This interface is accessible locally.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public interface UvmContext
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
     * Get the <code>LoggingManager</code> singleton.
     *
     * @return the LoggingManager.
     */
    LoggingManager loggingManager();

    /**
     * Get the <code>PolicyManager</code> singleton.
     *
     * @return the PolicyManager.
     */
    PolicyManager policyManager();

    /**
     * Get the <code>AdminManager</code> singleton.
     *
     * @return the AdminManager.
     */
    AdminManager adminManager();

    /**
     * Get the <code>NetworkManager</code> singleton.
     *
     * @return the NetworkManager.
     */
    NetworkManager networkManager();

    /**
     * Get the <code>ReportingManager</code> singleton.
     *
     * @return the ReportingManager.
     */
    ReportingManager reportingManager();

    /**
     * Get the <code>ConnectivityTester</code> singleton.
     *
     * @return the ConnectivityTester
     */
    ConnectivityTester getConnectivityTester();

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
    AppServerManager appServerManager();

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
    BrandingManager brandingManager();

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
     * @return the LicenseManager
     */
    LicenseManager licenseManager();

    /**
     * The settings manager.
     * 
     * @return the SettingsManager
     */
    SettingsManager settingsManager();

    /**
     * The session monitor
     * This can be used for getting information about current sessions
     *
     * @return the SessionMonitor
     */
    SessionMonitor sessionMonitor();

    /**
     * Get the <code>OemManager</code> singleton.
     *
     * @return the OemManager.
     */
    OemManager oemManager();

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
    
    /**
     * Gets the current state of the UVM
     *
     * @return a <code>UvmState</code> enumerated value
     */
    UvmState state();

    SyslogManager syslogManager();

    ArgonManager argonManager();

    /**
     * Get the AppServerManager singleton for this instance
     *
     * @return the singleton
     */
    LocalAppServerManager localAppServerManager();

    Process exec(String cmd) throws IOException;
    Process exec(String[] cmd) throws IOException;
    Process exec(String[] cmd, String[] envp) throws IOException;
    Process exec(String[] cmd, String[] envp, File dir) throws IOException;

    String getFullVersion();

    /**
     * The pipeline compiler.
     *
     * @return a <code>PipelineFoundry</code> value
     */
    PipelineFoundry pipelineFoundry();

    /**
     * Returns true if the setup wizard has been completed
     *
     * @return a <code>boolean</code> value
     */
    boolean isWizardComplete();

    boolean runTransaction(TransactionWork<?> tw);

    Thread newThread(Runnable runnable);

    Thread newThread(Runnable runnable, String name);

    EventLogger<LogEvent> eventLogger();

    void waitForStartup();

    LocalTomcatManager tomcatManager();
    
    CronJob makeCronJob(Period p, Runnable r);

    /*
     * Loads a shared library (.so) into the UVM classloader.  This
     * is so a node dosen't load it into its own, which doesn't
     * work right.
     */
    void loadLibrary(String libname);

    /**
     * Returns the UID of the server
     * Example: aaaa-bbbb-cccc-dddd
     */
    String getServerUID();
    
    UploadManager uploadManager();
    
}
