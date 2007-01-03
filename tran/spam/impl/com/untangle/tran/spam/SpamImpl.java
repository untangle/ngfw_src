/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.spam;

import static com.untangle.tran.util.Ascii.CRLF;

import com.untangle.mvvm.logging.EventLogger;
import com.untangle.mvvm.logging.EventLoggerFactory;
import com.untangle.mvvm.logging.EventManager;
import com.untangle.mvvm.logging.SimpleEventFilter;
import com.untangle.mvvm.tapi.AbstractTransform;
import com.untangle.mvvm.tapi.Affinity;
import com.untangle.mvvm.tapi.Fitting;
import com.untangle.mvvm.tapi.PipeSpec;
import com.untangle.mvvm.tapi.SoloPipeSpec;
import com.untangle.mvvm.tran.Transform;
import com.untangle.mvvm.tran.TransformContext;
import com.untangle.mvvm.util.TransactionWork;
import com.untangle.tran.token.TokenAdaptor;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

public class SpamImpl extends AbstractTransform implements SpamTransform
{
    //===============================
    // Defaults for templates

    private static final String OUT_MOD_SUB_TEMPLATE =
      "[SPAM] $MIMEMessage:SUBJECT$";
    // OLD
    // private static final String OUT_MOD_BODY_TEMPLATE =
    // "The attached message from $MIMEMessage:FROM$ was determined\r\n " +
    // "to be SPAM based on a score of $SPAMReport:SCORE$ where anything above $SPAMReport:THRESHOLD$\r\n" +
    // "is SPAM.  The details of the report are as follows:\r\n\r\n" +
    // "$SPAMReport:FULL$";
     private static final String OUT_MOD_BODY_TEMPLATE =
         "The attached message from $MIMEMessage:FROM$\r\n" +
         "was determined by Untangle Spam Blocker to be spam based on a score\r\n" +
         "of $SPAMReport:SCORE$ where anything above $SPAMReport:THRESHOLD$ is spam.\r\n";

    private static final String OUT_MOD_BODY_SMTP_TEMPLATE =
         "The attached message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$)\r\n" +
         "was determined by Untangle Spam Blocker to be spam based on a score\r\n" +
         "of $SPAMReport:SCORE$ where anything above $SPAMReport:THRESHOLD$ is spam.\r\n";

    private static final String IN_MOD_SUB_TEMPLATE = OUT_MOD_SUB_TEMPLATE;
    private static final String IN_MOD_BODY_TEMPLATE = OUT_MOD_BODY_TEMPLATE;
    private static final String IN_MOD_BODY_SMTP_TEMPLATE = OUT_MOD_BODY_SMTP_TEMPLATE;

    private static final String SPAM_HEADER_NAME = "X-Spam-Flag";
    private final static String IS_SPAM_HDR_VALUE = "YES";
    private final static String IS_HAM_HDR_VALUE = "NO";

    private static final String OUT_NOTIFY_SUB_TEMPLATE =
      "[SPAM NOTIFICATION] re: $MIMEMessage:SUBJECT$";

    private static final String OUT_NOTIFY_BODY_TEMPLATE =
        "On $MIMEHeader:DATE$ a message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$)" + CRLF +
        "was received by $SMTPTransaction:TO$.  The message was determined" + CRLF +
        "by Untangle Spam Blocker to be spam based on a score of $SPAMReport:SCORE$ where anything" + CRLF +
        "above $SPAMReport:THRESHOLD$ is spam.  The details of the report are as follows:" + CRLF + CRLF +
        "$SPAMReport:FULL$";

    private static final String IN_NOTIFY_SUB_TEMPLATE = OUT_NOTIFY_SUB_TEMPLATE;
    private static final String IN_NOTIFY_BODY_TEMPLATE = OUT_NOTIFY_BODY_TEMPLATE;

    // We want to make sure that spam is before virus in the pipeline (towards the client for smtp,
    // server for pop/imap).
    private final PipeSpec[] pipeSpecs = new PipeSpec[] {
        new SoloPipeSpec("spam-smtp", this, new TokenAdaptor(this, new SpamSmtpFactory(this)), Fitting.SMTP_TOKENS, Affinity.CLIENT, 10),
        new SoloPipeSpec("spam-pop", this, new TokenAdaptor(this, new SpamPopFactory(this)), Fitting.POP_TOKENS, Affinity.SERVER, 10),
        new SoloPipeSpec("spam-imap", this, new TokenAdaptor(this, new SpamImapFactory(this)), Fitting.IMAP_TOKENS, Affinity.SERVER, 10)
    };

    private final SpamScanner scanner;
    private final EventLogger<SpamEvent> eventLogger;

    private final Logger logger = Logger.getLogger(getClass());

    private volatile SpamSettings zSpamSettings;

    // constructors -----------------------------------------------------------

    public SpamImpl(SpamScanner scanner)
    {
        TransformContext tctx = getTransformContext();

        this.scanner = scanner;

        String vendor = scanner.getVendorName();

        eventLogger = EventLoggerFactory.factory().getEventLogger(tctx);

        SimpleEventFilter ef = new SpamAllFilter(vendor);
        eventLogger.addSimpleEventFilter(ef);

        ef = new SpamSpamFilter(vendor);
        eventLogger.addSimpleEventFilter(ef);

        ef = new SpamSmtpFilter(vendor);
        eventLogger.addSimpleEventFilter(ef);

        ef = new SpamLogFilter(vendor);
        eventLogger.addSimpleEventFilter(ef);
    }

    // Spam methods -----------------------------------------------------------

    public EventManager<SpamEvent> getEventManager()
    {
        return eventLogger;
    }

    // Transform methods ------------------------------------------------------

    /**
     * Method for subclass (currently - clamphish) to define
     * the default Subject template for POP/IMAP wrapped messages
     */
    public String getDefaultSubjectWrapperTemplate(boolean inbound) {
      return inbound?IN_MOD_SUB_TEMPLATE:OUT_MOD_SUB_TEMPLATE;
    }
    /**
     * Method for subclass (currently - clamphish) to define
     * the default wrapping body template for POP/IMAP
     */
    public String getDefaultBodyWrapperTemplate(boolean inbound) {
      return inbound?IN_MOD_BODY_TEMPLATE:OUT_MOD_BODY_TEMPLATE;
    }
    /**
     * Method for subclass (currently - clamphish) to define
     * the default Subject template for SMTP wrapped messages
     */
    public String getDefaultSMTPSubjectWrapperTemplate(boolean inbound) {
      return getDefaultSubjectWrapperTemplate(inbound);
    }
    /**
     * Method for subclass (currently - clamphish) to define
     * the default wrapping body template for SMTP
     */
    public String getDefaultSMTPBodyWrapperTemplate(boolean inbound) {
      return inbound?IN_MOD_BODY_SMTP_TEMPLATE:OUT_MOD_BODY_SMTP_TEMPLATE;
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
      return isSpam?IS_SPAM_HDR_VALUE:IS_HAM_HDR_VALUE;
    }

    /**
     * Get the default template used for notification messages'
     * subject
     */
    public String getDefaultNotifySubjectTemplate(boolean inbound) {
      return inbound?IN_NOTIFY_SUB_TEMPLATE:OUT_NOTIFY_SUB_TEMPLATE;
    }

    /**
     * Get the default template used to create notification
     * messages' bodies
     */
    public String getDefaultNotifyBodyTemplate(boolean inbound) {
      return inbound?IN_NOTIFY_BODY_TEMPLATE:OUT_NOTIFY_BODY_TEMPLATE;
    }


    /**
     * The settings for the IMAP/POP/SMTP
     * templates have been added to the
     * Config objects, yet not in the database
     * (9/05).  This method makes sure that
     * they are set to the programatic
     * default.
     *
     * Once we move these to the database,
     * this method is obsolete.
     */
    private void ensureTemplateSettings(SpamSettings ss) {
      ss.getIMAPInbound().setSubjectWrapperTemplate(getDefaultSubjectWrapperTemplate(true));
      ss.getIMAPOutbound().setSubjectWrapperTemplate(getDefaultSubjectWrapperTemplate(false));
      ss.getIMAPInbound().setBodyWrapperTemplate(getDefaultBodyWrapperTemplate(true));
      ss.getIMAPOutbound().setBodyWrapperTemplate(getDefaultBodyWrapperTemplate(false));
      ss.getIMAPInbound().setHeaderName(getDefaultIndicatorHeaderName());
      ss.getIMAPOutbound().setHeaderName(getDefaultIndicatorHeaderName());
      ss.getIMAPInbound().setHeaderValue(getDefaultIndicatorHeaderValue(true), true);
      ss.getIMAPInbound().setHeaderValue(getDefaultIndicatorHeaderValue(false), false);
      ss.getIMAPOutbound().setHeaderValue(getDefaultIndicatorHeaderValue(true), true);
      ss.getIMAPOutbound().setHeaderValue(getDefaultIndicatorHeaderValue(false), false);

      ss.getPOPInbound().setSubjectWrapperTemplate(getDefaultSubjectWrapperTemplate(true));
      ss.getPOPOutbound().setSubjectWrapperTemplate(getDefaultSubjectWrapperTemplate(false));
      ss.getPOPInbound().setBodyWrapperTemplate(getDefaultBodyWrapperTemplate(true));
      ss.getPOPOutbound().setBodyWrapperTemplate(getDefaultBodyWrapperTemplate(false));
      ss.getPOPInbound().setHeaderName(getDefaultIndicatorHeaderName());
      ss.getPOPOutbound().setHeaderName(getDefaultIndicatorHeaderName());
      ss.getPOPInbound().setHeaderValue(getDefaultIndicatorHeaderValue(true), true);
      ss.getPOPInbound().setHeaderValue(getDefaultIndicatorHeaderValue(false), false);
      ss.getPOPOutbound().setHeaderValue(getDefaultIndicatorHeaderValue(true), true);
      ss.getPOPOutbound().setHeaderValue(getDefaultIndicatorHeaderValue(false), false);

      ss.getSMTPInbound().setSubjectWrapperTemplate(getDefaultSMTPSubjectWrapperTemplate(true));
      ss.getSMTPOutbound().setSubjectWrapperTemplate(getDefaultSMTPSubjectWrapperTemplate(false));
      ss.getSMTPInbound().setBodyWrapperTemplate(getDefaultSMTPBodyWrapperTemplate(true));
      ss.getSMTPOutbound().setBodyWrapperTemplate(getDefaultSMTPBodyWrapperTemplate(false));
      ss.getSMTPInbound().setHeaderName(getDefaultIndicatorHeaderName());
      ss.getSMTPOutbound().setHeaderName(getDefaultIndicatorHeaderName());
      ss.getSMTPInbound().setHeaderValue(getDefaultIndicatorHeaderValue(true), true);
      ss.getSMTPInbound().setHeaderValue(getDefaultIndicatorHeaderValue(false), false);
      ss.getSMTPOutbound().setHeaderValue(getDefaultIndicatorHeaderValue(true), true);
      ss.getSMTPOutbound().setHeaderValue(getDefaultIndicatorHeaderValue(false), false);
      ss.getSMTPOutbound().setNotifySubjectTemplate(getDefaultNotifySubjectTemplate(false));
      ss.getSMTPInbound().setNotifySubjectTemplate(getDefaultNotifySubjectTemplate(true));
      ss.getSMTPOutbound().setNotifyBodyTemplate(getDefaultNotifyBodyTemplate(false));
      ss.getSMTPInbound().setNotifyBodyTemplate(getDefaultNotifyBodyTemplate(true));
    }


    public SpamSettings getSpamSettings()
    {
    if( this.zSpamSettings == null )
        logger.error("Settings not yet initialized. State: " + getTransformContext().getRunState() );
        return this.zSpamSettings;
    }

    public void setSpamSettings(final SpamSettings newSettings)
    {
        //TEMP HACK, Until we move the templates to database
        ensureTemplateSettings(newSettings);
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.merge(newSettings);
                    SpamImpl.this.zSpamSettings = newSettings;

                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);

        return;
    }

    /**
     * Increment the counter for messages scanned
     */
    public void incrementScanCounter() {
      //The scanning blingy-bringer has been disabled
//      incrementCount(Transform.GENERIC_0_COUNTER);
    }
    /**
     * Increment the counter for blocked (SMTP only).
     */
    public void incrementBlockCounter() {
      incrementCount(Transform.GENERIC_1_COUNTER);
    }
    /**
     * Increment the counter for messages passed
     */
    public void incrementPassCounter() {
      incrementCount(Transform.GENERIC_0_COUNTER);
    }
    /**
     * Increment the counter for messages marked
     */
    public void incrementMarkCounter() {
      incrementCount(Transform.GENERIC_2_COUNTER);
    }
    /**
     * Increment the count for messages quarantined.
     */
    public void incrementQuarantineCount() {
      incrementCount(Transform.GENERIC_3_COUNTER);
    }


    public void initializeSettings()
    {
        logger.debug("Initializing Settings");

        SpamSettings zTmpSpamSettings = new SpamSettings(getTid());

        zTmpSpamSettings.setSMTPInbound(
          new SpamSMTPConfig(true,
                             SMTPSpamMessageAction.QUARANTINE,
                             SpamSMTPNotifyAction.NEITHER,
                             SpamProtoConfig.DEFAULT_STRENGTH,
                             "Scan incoming SMTP e-mail",
                             getDefaultSubjectWrapperTemplate(true),
                             getDefaultBodyWrapperTemplate(true),
                             getDefaultIndicatorHeaderName(),
                             getDefaultIndicatorHeaderValue(true),
                             getDefaultIndicatorHeaderValue(false),
                             getDefaultNotifySubjectTemplate(true),
                             getDefaultNotifyBodyTemplate(true),
                             false,
                             15));

        zTmpSpamSettings.setSMTPOutbound(
          new SpamSMTPConfig(false,
                             SMTPSpamMessageAction.PASS,
                             SpamSMTPNotifyAction.NEITHER,
                             SpamProtoConfig.DEFAULT_STRENGTH,
                             "Scan outgoing SMTP e-mail",
                             getDefaultSubjectWrapperTemplate(false),
                             getDefaultBodyWrapperTemplate(false),
                             getDefaultIndicatorHeaderName(),
                             getDefaultIndicatorHeaderValue(true),
                             getDefaultIndicatorHeaderValue(false),
                             getDefaultNotifySubjectTemplate(false),
                             getDefaultNotifyBodyTemplate(false),
                             false,
                             15));

        zTmpSpamSettings.setPOPInbound(
          new SpamPOPConfig(true,
                            SpamMessageAction.MARK,
                            SpamProtoConfig.DEFAULT_STRENGTH,
                            "Scan incoming POP e-mail",
                            getDefaultSubjectWrapperTemplate(true),
                            getDefaultBodyWrapperTemplate(true),
                            getDefaultIndicatorHeaderName(),
                             getDefaultIndicatorHeaderValue(true),
                             getDefaultIndicatorHeaderValue(false) ));

        zTmpSpamSettings.setPOPOutbound(
          new SpamPOPConfig(false,
                            SpamMessageAction.PASS,
                            SpamProtoConfig.DEFAULT_STRENGTH,
                            "Scan outgoing POP e-mail",
                            getDefaultSubjectWrapperTemplate(false),
                            getDefaultBodyWrapperTemplate(false),
                            getDefaultIndicatorHeaderName(),
                             getDefaultIndicatorHeaderValue(true),
                             getDefaultIndicatorHeaderValue(false) ));

        zTmpSpamSettings.setIMAPInbound(
          new SpamIMAPConfig(true,
                             SpamMessageAction.MARK,
                             SpamProtoConfig.DEFAULT_STRENGTH,
                             "Scan incoming IMAP e-mail",
                             getDefaultSMTPSubjectWrapperTemplate(true),
                             getDefaultSMTPBodyWrapperTemplate(true),
                             getDefaultIndicatorHeaderName(),
                             getDefaultIndicatorHeaderValue(true),
                             getDefaultIndicatorHeaderValue(false) ));

        zTmpSpamSettings.setIMAPOutbound(
          new SpamIMAPConfig(false,
                             SpamMessageAction.PASS,
                             SpamProtoConfig.DEFAULT_STRENGTH,
                             "Scan outgoing IMAP e-mail",
                             getDefaultSMTPSubjectWrapperTemplate(false),
                             getDefaultSMTPBodyWrapperTemplate(false),
                             getDefaultIndicatorHeaderName(),
                             getDefaultIndicatorHeaderValue(true),
                             getDefaultIndicatorHeaderValue(false) ));

        ensureTemplateSettings(zTmpSpamSettings);
        setSpamSettings(zTmpSpamSettings);
        return;
    }

    // AbstractTransform methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    protected void preInit(String args[])
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery
                        ("from SpamSettings ss where ss.tid = :tid");
                    q.setParameter("tid", getTid());
                    zSpamSettings = (SpamSettings)q.uniqueResult();

                    ensureTemplateSettings(zSpamSettings);

                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);

        return;
    }

    public SpamScanner getScanner()
    {
        return scanner;
    }

    void log(SpamEvent se)
    {
        eventLogger.log(se);
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
