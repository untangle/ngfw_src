/**
 * $Id$
 */
package com.untangle.node.smtp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import com.untangle.node.smtp.quarantine.Quarantine;
import com.untangle.node.smtp.safelist.SafelistManager;
import com.untangle.node.smtp.SmtpCasingFactory;
import com.untangle.node.smtp.MailExport;
import com.untangle.node.smtp.MailExportFactory;
import com.untangle.node.smtp.SmtpNode;
import com.untangle.node.smtp.SmtpNodeSettings;
import com.untangle.node.smtp.quarantine.BadTokenException;
import com.untangle.node.smtp.quarantine.Inbox;
import com.untangle.node.smtp.quarantine.InboxAlreadyRemappedException;
import com.untangle.node.smtp.quarantine.InboxArray;
import com.untangle.node.smtp.quarantine.InboxIndex;
import com.untangle.node.smtp.quarantine.InboxRecord;
import com.untangle.node.smtp.quarantine.InboxRecordArray;
import com.untangle.node.smtp.quarantine.MailSummary;
import com.untangle.node.smtp.quarantine.NoSuchInboxException;
import com.untangle.node.smtp.quarantine.QuarantineMaintenenceView;
import com.untangle.node.smtp.quarantine.QuarantineNodeView;
import com.untangle.node.smtp.quarantine.QuarantineSettings;
import com.untangle.node.smtp.quarantine.QuarantineUserActionFailedException;
import com.untangle.node.smtp.quarantine.QuarantineUserView;
import com.untangle.node.smtp.safelist.NoSuchSafelistException;
import com.untangle.node.smtp.safelist.SafelistActionFailedException;
import com.untangle.node.smtp.safelist.SafelistAdminView;
import com.untangle.node.smtp.safelist.SafelistCount;
import com.untangle.node.smtp.safelist.SafelistEndUserView;
import com.untangle.node.smtp.safelist.SafelistManipulation;
import com.untangle.node.smtp.safelist.SafelistNodeView;
import com.untangle.node.smtp.safelist.SafelistSettings;
import com.untangle.node.smtp.mime.EmailAddress;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.CasingPipeSpec;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.SettingsManager;

public class SmtpNodeImpl extends NodeBase implements SmtpNode, MailExport
{
    private static final String QUARANTINE_JS_URL = "/quarantine/app.js";

    private final SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
    private final Logger logger = Logger.getLogger(SmtpNodeImpl.class);

    private final CasingPipeSpec SMTP_PIPE_SPEC = new CasingPipeSpec("smtp", this, SmtpCasingFactory.factory(),Fitting.SMTP_STREAM, Fitting.SMTP_TOKENS);

    private final PipeSpec[] pipeSpecs = new PipeSpec[] { SMTP_PIPE_SPEC };

    private SmtpNodeSettings settings;
    private static Quarantine s_quarantine;//This will never be null for *instances* of SmtpNodeImpl
    private static final long ONE_GB = (1024L * 1024L * 1024L);

    //HAck instances for RMI issues
    private QuarantineUserViewWrapper m_quv = new QuarantineUserViewWrapper();
    private QuarantineMaintenenceViewWrapper m_qmv = new QuarantineMaintenenceViewWrapper();
    private QuarantineNodeViewWrapper m_qtv = new QuarantineNodeViewWrapper();
    private static SafelistManager s_safelistMngr;
    private SafelistNodeViewWrapper m_stv = new SafelistNodeViewWrapper();
    private SafelistEndUserViewWrapper m_suv = new SafelistEndUserViewWrapper();
    private SafelistAdminViewWrapper m_sav = new SafelistAdminViewWrapper();
    private static boolean s_deployedWebApp = false;
    private static boolean s_unDeployedWebApp = false;

    // constructors -----------------------------------------------------------

    public SmtpNodeImpl( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

        createSingletonsIfRequired();

        MailExportFactory.factory().registerExport(this);
    }

    private static synchronized void createSingletonsIfRequired()
    {
        if(s_quarantine == null) {
            s_quarantine = new Quarantine();
        }
        if(s_safelistMngr == null) {
            s_safelistMngr = new SafelistManager(s_quarantine);
        }
    }

    private static synchronized void deployWebAppIfRequired(Logger logger)
    {
        if(!s_deployedWebApp) {
            if (null != UvmContextFactory.context().tomcatManager().loadServlet("/quarantine", "quarantine")) {
                logger.debug("Deployed Quarantine web app");
            }
            else {
                logger.error("Unable to deploy Quarantine web app");
            }
            s_deployedWebApp = true;
        }
    }

    private static synchronized void unDeployWebAppIfRequired(Logger logger)
    {
        if(!s_unDeployedWebApp) {
            if (UvmContextFactory.context().tomcatManager().unloadServlet("/quarantine")) {
                logger.debug("Unloaded Quarantine web app");
            }
            else {
                logger.error("Unable to unload Quarantine web app");
            }
            s_unDeployedWebApp = true;
        }
    }

    private void initializeSmtpNodeSettings()
    {
        SmtpNodeSettings ns = new SmtpNodeSettings();
        ns.setSmtpEnabled(true);
        ns.setSmtpTimeout(1000*60*4);
        ns.setSmtpAllowTLS(false);

        QuarantineSettings qs = new QuarantineSettings();
        qs.setMaxQuarantineTotalSz(10 * ONE_GB);            // 10GB
        qs.setDigestHourOfDay(6);                           // 6 am
        qs.setDigestMinuteOfDay(0);                         // 6 am
        byte[] binaryKey = new byte[4];
        new java.util.Random().nextBytes(binaryKey);
        qs.initBinaryKey(binaryKey);
        qs.setMaxMailIntern(QuarantineSettings.WEEK * 2);
        qs.setMaxIdleInbox(QuarantineSettings.WEEK * 4);
        ns.setQuarantineSettings(qs);

        ArrayList<SafelistSettings> ss = new ArrayList<SafelistSettings>();
        //TODO Set defaults here - DEFAULT TO WHAT?????
        ns.setSafelistSettings(ss);

        setSmtpNodeSettings(ns);
    }

    // SmtpNode methods --------------------------------------------------------

    public SmtpNodeSettings getSmtpNodeSettings()
    {
        return settings;
    }

    public void setSmtpNodeSettings(final SmtpNodeSettings newSettings)
    {
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsName = System.getProperty("uvm.settings.dir") + "/untangle-casing-smtp/settings_" + nodeID;
        String settingsFile = settingsName + ".js";

        try {
            settingsManager.save(SmtpNodeSettings.class, settingsName, newSettings);
        } catch(Exception exn) {
            logger.error("setSmtpNodeSettings()",exn);
            return;
        }

        this.settings = newSettings;
        
        reconfigure();
        s_quarantine.setSettings(this, settings.getQuarantineSettings());
        s_safelistMngr.setSettings(this, settings);
    }

    public void setSmtpNodeSettingsWithoutSafelists(final SmtpNodeSettings settings)
    {
        // if the settings object being passed does not have a valid secret
        // key then we copy the key from the existing settings
        if (settings.getQuarantineSettings().getSecretKey() == null)
            settings.getQuarantineSettings().initBinaryKey(this.settings.getQuarantineSettings().grabBinaryKey());

        //get initial safelists
        settings.setSafelistSettings(this.settings.getSafelistSettings());
        setSmtpNodeSettings(settings);
    }

    public QuarantineUserView getQuarantineUserView()
    {
        return m_quv;
    }

    public QuarantineMaintenenceView getQuarantineMaintenenceView()
    {
        return m_qmv;
    }

    public SafelistEndUserView getSafelistEndUserView()
    {
        return m_suv;
    }

    public SafelistAdminView getSafelistAdminView()
    {
        return m_sav;
    }

    public long getMinAllocatedStoreSize(boolean inGB)
    {
        if (false == inGB) {
            return ONE_GB;
        }
        return ONE_GB / ONE_GB;
    }

    // max is arbitrarily set to 30 GB
    public long getMaxAllocatedStoreSize(boolean inGB)
    {
        if (false == inGB) {
            return (30 * ONE_GB);
        }
        return 30 * (ONE_GB / ONE_GB);
    }

    public String createAuthToken(String account)
    {
        return m_qtv.createAuthToken(account);
    }

    // MailExport methods -----------------------------------------------------

    public SmtpNodeSettings getExportSettings()
    {
        return getSmtpNodeSettings();
    }

    public QuarantineNodeView getQuarantineNodeView()
    {
        return m_qtv;
    }

    public SafelistNodeView getSafelistNodeView()
    {
        return m_stv;
    }

    private void reconfigure()
    {
        SMTP_PIPE_SPEC.setEnabled(settings.isSmtpEnabled());
    }

    // Node methods -----------------------------------------------------------

    @Override
    protected void preDestroy()
    {
        super.preDestroy();
        logger.debug("preDestroy()");
        unDeployWebAppIfRequired(logger);
        s_quarantine.close();
    }

    protected void postInit()
    {
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsName = System.getProperty("uvm.settings.dir") + "/untangle-casing-smtp/settings_" + nodeID;
        String settingsFile = settingsName + ".js";

        SmtpNodeSettings readSettings = null;
        logger.info("Loading settings from " + settingsFile );

        try {
            // first we try to read our json settings
            readSettings = settingsManager.load( SmtpNodeSettings.class, settingsName );
        } catch (Exception exn) {
            logger.error("postInit()",exn);
        }


        try {
            // still no settings found so init with defaults
            if (readSettings == null) {
                logger.warn("No settings found... initializing with defaults");
                initializeSmtpNodeSettings();
            }
            // otherwise apply the loaded or imported settings from the file
            else {
                logger.info("Loaded settings from " + settingsFile);
                
                settings = readSettings;
                s_quarantine.setSettings(this, settings.getQuarantineSettings());
                s_safelistMngr.setSettings(this, settings);
                reconfigure();
            }
        } catch (Exception exn) {
            logger.error("Could not apply node settings",exn);
        }

        // At this point the settings have either been loaded from disk
        // or initialized to defaults so now we do all the other setup
        try {
            // XXX what is this?
            s_safelistMngr.createSafelist("GLOBAL");
        } catch (Exception exn) {
            logger.error("Could not create global safelist",exn);
        }

        deployWebAppIfRequired(logger);
        s_quarantine.open();
    }

    // NodeBase methods ---------------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    public Object getSettings()
    {
        return getSmtpNodeSettings();
    }

    public void setSettings(Object settings)
    {
        setSmtpNodeSettings((SmtpNodeSettings)settings);
    }

    //================================================================
    //Hacks to work around issues w/ the implicit RMI proxy stuff

    public abstract class QuarantineManipulationWrapper
    {
        public InboxIndex purge(String account, String...doomedMails)
            throws NoSuchInboxException, QuarantineUserActionFailedException
        {
            return s_quarantine.purge(account, doomedMails);
        }

        public InboxIndex rescue(String account, String...rescuedMails)
            throws NoSuchInboxException, QuarantineUserActionFailedException
        {
            return s_quarantine.rescue(account, rescuedMails);
        }

        public InboxIndex getInboxIndex(String account)
            throws NoSuchInboxException, QuarantineUserActionFailedException
        {
            return s_quarantine.getInboxIndex(account);
        }

        public void test() {}
    }

    public class QuarantineUserViewWrapper
        extends QuarantineManipulationWrapper
        implements QuarantineUserView
    {

        public String getAccountFromToken(String token)
            throws BadTokenException
        {
            return s_quarantine.getAccountFromToken(token);
        }

        public boolean requestDigestEmail(String account)
            throws NoSuchInboxException, QuarantineUserActionFailedException
        {
            return s_quarantine.requestDigestEmail(account);
        }

        public void remapSelfService(String from, String to)
            throws QuarantineUserActionFailedException, InboxAlreadyRemappedException
        {
            s_quarantine.remapSelfService(from, to);
        }

        public boolean unmapSelfService(String inboxName, String aliasToRemove)
            throws QuarantineUserActionFailedException
        {
            return s_quarantine.unmapSelfService(inboxName, aliasToRemove);
        }

        public String getMappedTo(String account)
            throws QuarantineUserActionFailedException
        {
            return s_quarantine.getMappedTo(account);
        }

        public String[] getMappedFrom(String account)
            throws QuarantineUserActionFailedException
        {
            return s_quarantine.getMappedFrom(account);
        }
    }

    public class QuarantineMaintenenceViewWrapper
        extends QuarantineManipulationWrapper
        implements QuarantineMaintenenceView {

        public long getInboxesTotalSize()
            throws QuarantineUserActionFailedException
        {
            return s_quarantine.getInboxesTotalSize();
        }

        public InboxArray getInboxArray( int start, int limit, String sortColumn, boolean isAscending )
            throws QuarantineUserActionFailedException
        {
            return s_quarantine.getInboxArray( start, limit, sortColumn, isAscending );
        }

        public InboxRecordArray getInboxRecordArray( String account, int start, int limit, String sortColumn, boolean isAscending )
            throws NoSuchInboxException, QuarantineUserActionFailedException
        {
            return s_quarantine.getInboxRecordArray( account, start, limit, sortColumn, isAscending );
        }

        public InboxRecordArray getInboxRecordArray(String account)
            throws NoSuchInboxException, QuarantineUserActionFailedException
        {
            return s_quarantine.getInboxRecordArray(account);
        }

        public List<InboxRecord> getInboxRecords(String account, int start, int limit, String... sortColumns)
            throws NoSuchInboxException, QuarantineUserActionFailedException
        {
            return s_quarantine.getInboxRecords(account, start, limit,
                                                sortColumns);
        }

        public int getInboxTotalRecords(String account)
            throws NoSuchInboxException, QuarantineUserActionFailedException
        {
            return s_quarantine.getInboxTotalRecords(account);
        }

        public String getFormattedInboxesTotalSize(boolean inMB)
        {
            return s_quarantine.getFormattedInboxesTotalSize(inMB);
        }

        public List<Inbox> listInboxes()
            throws QuarantineUserActionFailedException
        {
            return s_quarantine.listInboxes();
        }

        public void deleteInbox(String account)
            throws NoSuchInboxException, QuarantineUserActionFailedException
        {
            s_quarantine.deleteInbox(account);
        }

        public void deleteInboxes(String[] accounts)
            throws NoSuchInboxException, QuarantineUserActionFailedException
        {
            s_quarantine.deleteInboxes(accounts);
        }

        public void rescueInbox(String account)
            throws NoSuchInboxException, QuarantineUserActionFailedException
        {
            s_quarantine.rescueInbox(account);
        }

        public void rescueInboxes(String[] accounts)
            throws NoSuchInboxException, QuarantineUserActionFailedException
        {
            s_quarantine.rescueInboxes(accounts);
        }
    }

    public class QuarantineNodeViewWrapper implements QuarantineNodeView
    {
        public boolean quarantineMail(File file, MailSummary summary, EmailAddress...recipients)
        {
            return s_quarantine.quarantineMail(file, summary, recipients);
        }

        public String createAuthToken(String account)
        {
            return s_quarantine.createAuthToken(account);
        }
    }

    public class SafelistNodeViewWrapper implements SafelistNodeView
    {
        public boolean isSafelisted(EmailAddress envelopeSender, EmailAddress mimeFrom, List<EmailAddress> recipients)
        {
            return s_safelistMngr.isSafelisted(envelopeSender, mimeFrom, recipients);
        }
    }

    public abstract class SafelistManipulationWrapper implements SafelistManipulation
    {
        public String[] addToSafelist(String safelistOwnerAddress, String toAdd)
            throws NoSuchSafelistException, SafelistActionFailedException
        {
            return s_safelistMngr.addToSafelist(safelistOwnerAddress, toAdd);
        }

        public String[] removeFromSafelist(String safelistOwnerAddress, String toRemove)
            throws NoSuchSafelistException, SafelistActionFailedException
        {
            return s_safelistMngr.removeFromSafelist(safelistOwnerAddress, toRemove);
        }

        public String[] removeFromSafelists(String safelistOwnerAddress, String[] toRemove)
            throws NoSuchSafelistException, SafelistActionFailedException
        {
            return s_safelistMngr.removeFromSafelists(safelistOwnerAddress, toRemove);
        }

        public String[] replaceSafelist(String safelistOwnerAddress, String...listContents)
            throws NoSuchSafelistException, SafelistActionFailedException
        {
            return s_safelistMngr.replaceSafelist(safelistOwnerAddress, listContents);
        }

        public String[] getSafelistContents(String safelistOwnerAddress)
            throws NoSuchSafelistException, SafelistActionFailedException
        {
            return s_safelistMngr.getSafelistContents(safelistOwnerAddress);
        }

        public int getSafelistCnt(String safelistOwnerAddress)
            throws NoSuchSafelistException, SafelistActionFailedException
        {
            return s_safelistMngr.getSafelistCnt(safelistOwnerAddress);
        }

        public boolean hasOrCanHaveSafelist(String address)
        {
            return s_safelistMngr.hasOrCanHaveSafelist(address);
        }

        public void test() { }
    }

    public class SafelistEndUserViewWrapper
        extends SafelistManipulationWrapper
        implements SafelistEndUserView {}

    public class SafelistAdminViewWrapper
        extends SafelistManipulationWrapper
        implements SafelistAdminView
    {
        public List<String> listSafelists()
            throws SafelistActionFailedException
        {
            return s_safelistMngr.listSafelists();
        }

        public void deleteSafelist(String safelistOwnerAddress)
            throws SafelistActionFailedException
        {
            s_safelistMngr.deleteSafelist(safelistOwnerAddress);
        }

        public void deleteSafelists(String[] safelistOwnerAddresses)
            throws SafelistActionFailedException
        {
            s_safelistMngr.deleteSafelists(safelistOwnerAddresses);
        }

        public void createSafelist(String newListOwnerAddress)
            throws SafelistActionFailedException
        {
            s_safelistMngr.createSafelist(newListOwnerAddress);
        }

        public boolean safelistExists(String safelistOwnerAddress)
            throws SafelistActionFailedException
        {
            return s_safelistMngr.safelistExists(safelistOwnerAddress);
        }

        public List<SafelistCount> getUserSafelistCounts()
            throws NoSuchSafelistException, SafelistActionFailedException
        {
            return s_safelistMngr.getUserSafelistCounts();
        }
    }
}
