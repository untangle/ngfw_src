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

package com.untangle.node.spam;

import java.util.LinkedList;
import java.util.List;

import com.untangle.node.token.TokenAdaptor;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import static com.untangle.node.util.Ascii.CRLF;

public class SpamImpl extends AbstractNode implements SpamNode
{
    private final Logger logger = Logger.getLogger(getClass());

    //===============================
    // Defaults for templates

    private static final String MOD_SUB_TEMPLATE =
        "[SPAM] $MIMEMessage:SUBJECT$";

    private static final String MOD_BODY_TEMPLATE =
        "The attached message from $MIMEMessage:FROM$\r\n" +
        "was determined by the Spam Blocker to be spam based on a score\r\n" +
        "of $SPAMReport:SCORE$ where anything above $SPAMReport:THRESHOLD$ is spam.\r\n";

    private static final String MOD_BODY_SMTP_TEMPLATE =
        "The attached message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$)\r\n" +
        "was determined by the Spam Blocker to be spam based on a score\r\n" +
        "of $SPAMReport:SCORE$ where anything above $SPAMReport:THRESHOLD$ is spam.\r\n";

    private static final String SPAM_HEADER_NAME = "X-Spam-Flag";
    private final static String SPAM_HDR_VALUE = "YES";
    private final static String HAM_HDR_VALUE = "NO";

    private static final String NOTIFY_SUB_TEMPLATE =
        "[SPAM NOTIFICATION] re: $MIMEMessage:SUBJECT$";

    private static final String NOTIFY_BODY_TEMPLATE =
        "On $MIMEHeader:DATE$ a message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$)" + CRLF +
        "was received by $SMTPTransaction:TO$.  The message was determined" + CRLF +
        "by the Spam Blocker to be spam based on a score of $SPAMReport:SCORE$ where anything" + CRLF +
        "above $SPAMReport:THRESHOLD$ is spam.  The details of the report are as follows:" + CRLF + CRLF +
        "$SPAMReport:FULL$";

    private final RBLEventHandler rblHandler = new RBLEventHandler(this);

    // We want to make sure that spam is before virus in the pipeline (towards the client for smtp,
    // server for pop/imap).
    // Would want the RBL to get evaluated before the casing, this way if it blocks a session
    // the casing doesn't have to be initialized.
    private final PipeSpec[] pipeSpecs = new PipeSpec[] {
        new SoloPipeSpec("spam-smtp", this, new TokenAdaptor(this, new SpamSmtpFactory(this)), Fitting.SMTP_TOKENS, Affinity.CLIENT, 10),
        new SoloPipeSpec("spam-smtp-rbl", this, this.rblHandler, Fitting.SMTP_STREAM, Affinity.CLIENT, 11),
        new SoloPipeSpec("spam-pop", this, new TokenAdaptor(this, new SpamPopFactory(this)), Fitting.POP_TOKENS, Affinity.SERVER, 10),
        new SoloPipeSpec("spam-imap", this, new TokenAdaptor(this, new SpamImapFactory(this)), Fitting.IMAP_TOKENS, Affinity.SERVER, 10)
    };

    private final SpamScanner scanner;
    private final SpamAssassinDaemon saDaemon;
    private final EventLogger<SpamEvent> eventLogger;
    private final EventLogger<SpamSMTPRBLEvent> rblEventLogger;

    private volatile SpamSettings spamSettings;

    // constructors -----------------------------------------------------------

    public SpamImpl(SpamScanner scanner)
    {
        this.scanner = scanner;
        saDaemon = new SpamAssassinDaemon();

        NodeContext tctx = getNodeContext();

        eventLogger = EventLoggerFactory.factory().getEventLogger(tctx);
        rblEventLogger = EventLoggerFactory.factory().getEventLogger(tctx);

        String vendor = scanner.getVendorName();

        SimpleEventFilter ef = new SpamAllFilter(vendor);
        eventLogger.addSimpleEventFilter(ef);

        ef = new SpamSpamFilter(vendor);
        eventLogger.addSimpleEventFilter(ef);

        ef = new SpamSmtpFilter(vendor);
        eventLogger.addSimpleEventFilter(ef);

        ef = new SpamLogFilter(vendor);
        eventLogger.addSimpleEventFilter(ef);

        // filters for RBL events
        ef = new RBLAllFilter();
        rblEventLogger.addSimpleEventFilter(ef);

        ef = new RBLSkippedFilter();
        rblEventLogger.addSimpleEventFilter(ef);
    }

    // Spam methods -----------------------------------------------------------

    public EventManager<SpamEvent> getEventManager()
    {
        return eventLogger;
    }

    public EventManager<SpamSMTPRBLEvent> getRBLEventManager()
    {
        return rblEventLogger;
    }

    // Node methods ------------------------------------------------------

    /**
     * Increment the counter for messages scanned
     */
    public void incrementScanCounter() {
        //The scanning blingy-bringer has been disabled
        //      incrementCount(Node.GENERIC_0_COUNTER);
    }
    /**
     * Increment the counter for blocked (SMTP only).
     */
    public void incrementBlockCounter() {
        incrementCount(Node.GENERIC_1_COUNTER);
    }
    /**
     * Increment the counter for messages passed
     */
    public void incrementPassCounter() {
        incrementCount(Node.GENERIC_0_COUNTER);
    }
    /**
     * Increment the counter for messages marked
     */
    public void incrementMarkCounter() {
        incrementCount(Node.GENERIC_2_COUNTER);
    }
    /**
     * Increment the count for messages quarantined.
     */
    public void incrementQuarantineCount() {
        incrementCount(Node.GENERIC_3_COUNTER);
    }

    /**
     * Method for subclass (currently - clamphish) to define
     * the default Subject template for POP/IMAP wrapped messages
     */
    public String getDefaultSubjectWrapperTemplate() {
        return MOD_SUB_TEMPLATE;
    }
    /**
     * Method for subclass (currently - clamphish) to define
     * the default wrapping body template for POP/IMAP
     */
    public String getDefaultBodyWrapperTemplate() {
        return MOD_BODY_TEMPLATE;
    }
    /**
     * Method for subclass (currently - clamphish) to define
     * the default Subject template for SMTP wrapped messages
     */
    public String getDefaultSMTPSubjectWrapperTemplate() {
        return getDefaultSubjectWrapperTemplate();
    }
    /**
     * Method for subclass (currently - clamphish) to define
     * the default wrapping body template for SMTP
     */
    public String getDefaultSMTPBodyWrapperTemplate() {
        return MOD_BODY_SMTP_TEMPLATE;
    }

    /**
     * Get the default name of the header added to mails
     * to indicate "spamminess"
     */
    public String getDefaultIndicatorHeaderName() {
        return SPAM_HEADER_NAME;
    }

    /**
     * Get the default value for the {@link #getDefaultIndicatorHeaderName header}
     * indicating if the email is/is not spam
     */
    public String getDefaultIndicatorHeaderValue(boolean isSpam) {
        return isSpam?SPAM_HDR_VALUE:HAM_HDR_VALUE;
    }

    /**
     * Get the default template used for notification messages'
     * subject
     */
    public String getDefaultNotifySubjectTemplate() {
        return NOTIFY_SUB_TEMPLATE;
    }

    /**
     * Get the default template used to create notification
     * messages' bodies
     */
    public String getDefaultNotifyBodyTemplate() {
        return NOTIFY_BODY_TEMPLATE;
    }

    /**
     * The settings for the IMAP/POP/SMTP templates have been added
     * to the Config objects, yet not in the database (9/05).
     * This method makes sure that they are set to the programatic default.
     *
     * Once we move these to the database, this method is obsolete.
     */
    private void ensureTemplateSettings(SpamSettings ss) {
        ss.getImapConfig().setSubjectWrapperTemplate(getDefaultSubjectWrapperTemplate());
        ss.getImapConfig().setBodyWrapperTemplate(getDefaultBodyWrapperTemplate());
        ss.getImapConfig().setHeaderName(getDefaultIndicatorHeaderName());
        ss.getImapConfig().setHeaderValue(getDefaultIndicatorHeaderValue(true), true);
        ss.getImapConfig().setHeaderValue(getDefaultIndicatorHeaderValue(false), false);

        ss.getPopConfig().setSubjectWrapperTemplate(getDefaultSubjectWrapperTemplate());
        ss.getPopConfig().setBodyWrapperTemplate(getDefaultBodyWrapperTemplate());
        ss.getPopConfig().setHeaderName(getDefaultIndicatorHeaderName());
        ss.getPopConfig().setHeaderValue(getDefaultIndicatorHeaderValue(true), true);
        ss.getPopConfig().setHeaderValue(getDefaultIndicatorHeaderValue(false), false);

        ss.getSmtpConfig().setSubjectWrapperTemplate(getDefaultSMTPSubjectWrapperTemplate());
        ss.getSmtpConfig().setBodyWrapperTemplate(getDefaultSMTPBodyWrapperTemplate());
        ss.getSmtpConfig().setHeaderName(getDefaultIndicatorHeaderName());
        ss.getSmtpConfig().setHeaderValue(getDefaultIndicatorHeaderValue(true), true);
        ss.getSmtpConfig().setHeaderValue(getDefaultIndicatorHeaderValue(false), false);
        ss.getSmtpConfig().setNotifySubjectTemplate(getDefaultNotifySubjectTemplate());
        ss.getSmtpConfig().setNotifyBodyTemplate(getDefaultNotifyBodyTemplate());
    }

    protected void initSpamRBLList(SpamSettings tmpSpamSettings) {
        List<SpamRBL> spamRBLList = tmpSpamSettings.getSpamRBLList();
        if (( null == spamRBLList) || ( false == spamRBLList.isEmpty())) {
            // if already initialized,
            // use list as-is (e.g., database contains final word)
            //
            // if need be, update existing list here
            //
            // on return, save to database
            return;
        } // else initialize list now (e.g., upgrade has just occurred)

        spamRBLList.add(new SpamRBL("zen.spamhaus.org", "Spamhaus SBL, XBL and PBL lists.", true));
        // spamRBLList.add(new SpamRBL("list.dsbl.org", "Distributed Sender Blackhole List", true));
        // spamRBLList.add(new SpamRBL("sbl-xbl.spamhaus.org", "Spamhaus Block and Exploits Block Lists", true));
        // spamRBLList.add(new SpamRBL("bl.spamcop.net", "SpamCop Blocking List", true));

        return;
    }

    protected void initSpamAssassinDefList(SpamSettings tmpSpamSettings) {
        List<SpamAssassinDef> saDefList = tmpSpamSettings.getSpamAssassinDefList();
        SpamAssassinDefFile saDefFile = new SpamAssassinDefFile();

        if (false == saDefList.isEmpty()) {
            // if already initialized,
            // use list as-is (e.g., database contains final word)
            //
            // if need be, update existing list here
            //
            // write to file and on return, save to database
            // Writing disabled for 4.2... jdi XXXXXXXXXXXXXXXX
            // saDefFile.writeToFile(saDefList);
            // note that we do not automatically restart spamd
            // after every write to file because there may be no changes
            // - someone needs to separately call restartSpamAssassinDaemon
            //   if there are changes
            return;
        } // else initialize list now (e.g., upgrade has just occurred)

        // to initialize, merge existing system def confs from file
        // with required system def confs
        saDefFile.readFromFile(saDefList);
        initSpamAssassinDefList(saDefList);
        // Writing disabled for 4.2... jdi XXXXXXXXXXXXXXXX
        // saDefFile.writeToFile(saDefList);
        // note that we automatically restart spamd
        // after we've initialized because there may be changes
        // Restarting disabled (not writing) for 4.2... jdi XXXXXXXXXXXXXXXX
        // if (false == restartSpamAssassinDaemon())
        //    logger.error("Could not restart SpamAssassin Mail Filter Daemon");

        return;
    }

    // initialize system def conf settings
    // - delete any dupls
    // - swap required system def conf with its constant equivalent
    //   (to ensure that its value is correctly set)
    // - add any missing required system def conf with its constant equivalent
    private void initSpamAssassinDefList(List<SpamAssassinDef> saDefList)
    {
        List<SpamAssassinDef> duplSADefList = new LinkedList<SpamAssassinDef>();
        List<SpamAssassinDef> copySADefList = new LinkedList<SpamAssassinDef>(saDefList);

        boolean isDup;

        for (SpamAssassinDef saDef : saDefList) {
            if (false == saDef.getActive())
                continue;

            isDup = false;
            for (SpamAssassinDef copySADef : copySADefList) {
                if (false == copySADef.getActive())
                    continue;

                if (true == saDef.equalsOptName(copySADef)) {
                    if (true == isDup) {
                        duplSADefList.add(copySADef); // mark dupl for removal
                    } else {
                        isDup = true; // keep 1st one
                    }
                }
            }

            if (true == isDup) {
                copySADefList.removeAll(duplSADefList);
                duplSADefList.clear();
            }
        }

        saDefList.clear();
        saDefList.addAll(copySADefList); // add whatever remains
        copySADefList.clear();

        boolean hasEnabled = false;
        boolean hasOptions = false;

        for (SpamAssassinDef saDef : saDefList) {
            if (false == saDef.getActive()) {
                copySADefList.add(saDef);
            } else if (true == SpamAssassinDef.ENABLED_DEF.equalsOptName(saDef)){
                copySADefList.add(SpamAssassinDef.ENABLED_DEF); // swap
                hasEnabled = true;
            } else if (true == SpamAssassinDef.OPTIONS_DEF.equalsOptName(saDef)) {
                copySADefList.add(SpamAssassinDef.OPTIONS_DEF); // swap
                hasOptions = true;
            } else {
                copySADefList.add(saDef);
            }
        }

        saDefList.clear();
        // add required comment to beginning
        saDefList.add(SpamAssassinDef.DEF_COMMENT_DEF);
        saDefList.add(SpamAssassinDef.EMPTY_DEF);
        // add rest
        if (false == hasEnabled) {
            saDefList.add(SpamAssassinDef.ENABLED_DEF); // add missing
        } else if (false == hasOptions) {
            saDefList.add(SpamAssassinDef.OPTIONS_DEF); // add missing
        }
        saDefList.addAll(copySADefList);
        copySADefList.clear();

        return;
    }

    protected void initSpamAssassinLclList(SpamSettings tmpSpamSettings) {
        List<SpamAssassinLcl> saLclList = tmpSpamSettings.getSpamAssassinLclList();
        SpamAssassinLclFile saLclFile = new SpamAssassinLclFile();

        if (false == saLclList.isEmpty()) {
            // if already initialized,
            // use list as-is (e.g., database contains final word)
            //
            // if need be, update existing list here
            //
            // write to file and on return, save to database
            saLclFile.writeToFile(saLclList);
            return;
        } // else initialize list now (e.g., upgrade has just occurred)

        // to initialize, define local def confs now
        //
        // add required comment and local def conf
        saLclList.add(SpamAssassinLcl.LCL_COMMENT_LCL);
        saLclList.add(SpamAssassinLcl.EMPTY_LCL);
        saLclList.add(SpamAssassinLcl.SCORE_LCL);
        saLclList.add(SpamAssassinLcl.LOCK_DB_FILES_LCL);
        saLclList.add(SpamAssassinLcl.AUTO_WL_FACTOR_LCL);
        saLclList.add(SpamAssassinLcl.PYZOR_TIMEOUT_LCL);
        saLclList.add(SpamAssassinLcl.RESET_RPT_TEMPL_LCL);
        saLclList.add(SpamAssassinLcl.RPT_LCL);
        saLclFile.writeToFile(saLclList);

        return;
    }

    protected void configureSpamSettings(SpamSettings tmpSpamSettings) {
        tmpSpamSettings.setSmtpConfig(new SpamSMTPConfig(true,
                                                         SMTPSpamMessageAction.QUARANTINE,
                                                         SpamSMTPNotifyAction.NEITHER,
                                                         SpamProtoConfig.DEFAULT_STRENGTH,
                                                         "Scan incoming SMTP e-mail",
                                                         getDefaultSubjectWrapperTemplate(),
                                                         getDefaultBodyWrapperTemplate(),
                                                         getDefaultIndicatorHeaderName(),
                                                         getDefaultIndicatorHeaderValue(true),
                                                         getDefaultIndicatorHeaderValue(false),
                                                         getDefaultNotifySubjectTemplate(),
                                                         getDefaultNotifyBodyTemplate(),
                                                         true,
                                                         15));

        tmpSpamSettings.setPopConfig(new SpamPOPConfig(true,
                                                       SpamMessageAction.MARK,
                                                       SpamProtoConfig.DEFAULT_STRENGTH,
                                                       "Scan incoming POP e-mail",
                                                       getDefaultSubjectWrapperTemplate(),
                                                       getDefaultBodyWrapperTemplate(),
                                                       getDefaultIndicatorHeaderName(),
                                                       getDefaultIndicatorHeaderValue(true),
                                                       getDefaultIndicatorHeaderValue(false) ));

        tmpSpamSettings.setImapConfig(new SpamIMAPConfig(true,
                                                         SpamMessageAction.MARK,
                                                         SpamProtoConfig.DEFAULT_STRENGTH,
                                                         "Scan incoming IMAP e-mail",
                                                         getDefaultSMTPSubjectWrapperTemplate(),
                                                         getDefaultSMTPBodyWrapperTemplate(),
                                                         getDefaultIndicatorHeaderName(),
                                                         getDefaultIndicatorHeaderValue(true),
                                                         getDefaultIndicatorHeaderValue(false) ));
    }

    public SpamSettings getSpamSettings()
    {
        if( this.spamSettings == null )
            logger.error("Settings not yet initialized. State: " + getNodeContext().getRunState() );

        return this.spamSettings;
    }

    public void setSpamSettings(final SpamSettings newSpamSettings)
    {
        //TEMP HACK, Until we move the templates to database
        ensureTemplateSettings(newSpamSettings);
        // set lists if not already set
        initSpamRBLList(newSpamSettings);
        //initSpamAssassinLclList(newSpamSettings);
        //initSpamAssassinDefList(newSpamSettings); // def must be last

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.merge(newSpamSettings);
                    SpamImpl.this.spamSettings = newSpamSettings;

                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);

        return;
    }

    public void initializeSettings()
    {
        logger.debug("Initializing Settings");

        SpamSettings tmpSpamSettings = new SpamSettings(getTid());
        configureSpamSettings(tmpSpamSettings);
        setSpamSettings(tmpSpamSettings);
    }

    // convenience method for GUI
    public boolean startSpamAssassinDaemon() {
        return saDaemon.start();
    }

    // convenience method for GUI
    public boolean stopSpamAssassinDaemon() {
        return saDaemon.stop();
    }

    // convenience method for GUI
    public boolean restartSpamAssassinDaemon() {
        return saDaemon.restart();
    }

    // convenience method for GUI
    public List<String> getUnWhitelistFromList() {
        return getSALclOptValueList(SpamAssassinLcl.UNWHITELIST_FROM_SETTING);
    }

    // convenience method for GUI
    public void setUnWhitelistFromList(List<String> unWhitelistFromList) {
        swapSALclOptValues(unWhitelistFromList, SpamAssassinLcl.UNWHITELIST_FROM_SETTING);
        return;
    }

    // convenience method for GUI
    public List<String> getUnWhitelistFromRcvdList() {
        return getSALclOptValueList(SpamAssassinLcl.UNWHITELIST_FROM_RCVD_SETTING);
    }

    // convenience method for GUI
    public void setUnWhitelistFromRcvdList(List<String> unWhitelistFromRcvdList) {
        swapSALclOptValues(unWhitelistFromRcvdList, SpamAssassinLcl.UNWHITELIST_FROM_RCVD_SETTING);
        return;
    }

    private List<String> getSALclOptValueList(String optName) {
        List<String> optValueList = new LinkedList<String>();
        List<SpamAssassinLcl> saLclList = getSALclList(optName);
        for (SpamAssassinLcl saLcl : saLclList) {
            optValueList.add(saLcl.getOptValue());
        }

        return optValueList;
    }

    private void swapSALclOptValues(List<String> newOptValueList, String optName) {
        if (true == newOptValueList.isEmpty())
            return;

        // replace existing local def conf with new ones
        List<SpamAssassinLcl> rmSALclList = getSALclList(optName);
        List<SpamAssassinLcl> srcSALclList = spamSettings.getSpamAssassinLclList();
        srcSALclList.removeAll(rmSALclList);
        rmSALclList.clear();
        for (String newOptValue : newOptValueList) {
            srcSALclList.add(new SpamAssassinLcl(optName, newOptValue, null, true));
        }

        return;
    }

    private List<SpamAssassinLcl> getSALclList(String optName) {
        List<SpamAssassinLcl> saLclList = new LinkedList<SpamAssassinLcl>();
        List<SpamAssassinLcl> srcSALclList = spamSettings.getSpamAssassinLclList();
        for (SpamAssassinLcl srcSALcl : srcSALclList) {
            if (false == srcSALcl.getActive())
                continue;

            if (true == optName.equals(srcSALcl.getOptName()))
                saLclList.add(srcSALcl);
        }

        return saLclList;
    }

    // AbstractNode methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    @Override
    protected void preInit(String args[])
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = getSettingsQuery(s);
                    q.setParameter("tid", getTid());
                    spamSettings = (SpamSettings)q.uniqueResult();

                    ensureTemplateSettings(spamSettings);
                    // set lists if not already set
                    initSpamRBLList(spamSettings);
                    //initSpamAssassinLclList(spamSettings);
                    //initSpamAssassinDefList(spamSettings); // def must be last

                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);

        return;
    }

    protected Query getSettingsQuery(Session s)
    {
        Query q = s.createQuery("from SpamSettings ss where ss.tid = :tid");
        q.setParameter("tid", getTid());
        return q;
    }

    public SpamScanner getScanner()
    {
        return scanner;
    }

    void log(SpamEvent se)
    {
        eventLogger.log(se);
    }

    void logRBL(SpamSMTPRBLEvent spamSmtpRBLEvent) {
        rblEventLogger.log(spamSmtpRBLEvent);
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return getSpamSettings();
    }

    public void setSettings(Object settings)
    {
        setSpamSettings((SpamSettings)settings);
        return;
    }
}
