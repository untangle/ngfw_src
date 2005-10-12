/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.spam;

import static com.metavize.tran.util.Ascii.CRLF;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tran.Direction;
import com.metavize.mvvm.tran.Transform;
import com.metavize.mvvm.util.TransactionWork;
import com.metavize.tran.mail.papi.smtp.SMTPNotifyAction;
import com.metavize.tran.token.TokenAdaptor;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

public class SpamImpl extends AbstractTransform implements SpamTransform
{
    // XXX not yet tested, still need to index, etc...
    private static final String SMTP_QUERY_BASE
        = "SELECT evt.time_stamp, score, "
        + "       CASE WHEN action = 'P' OR NOT is_spam THEN 'PASS' "
        + "            WHEN action = 'M' THEN 'MARK' "
        + "            WHEN action = 'B' THEN 'BLOCK' "
        + "       END AS action, "
        + "       subject, rcpt.addr AS receiver, send.addr AS sender, "
        + "       c_client_addr, c_client_port, s_server_addr, s_server_port, "
        + "       policy_inbound AS incoming "
        + "FROM tr_spam_evt_smtp evt "
        + "JOIN tr_mail_message_info info ON evt.msg_id = info.id "
        + "JOIN pl_endp endp ON info.session_id = endp.session_id "
        + "LEFT OUTER JOIN tr_mail_message_info_addr rcpt ON info.id = rcpt.msg_id AND rcpt.kind = 'B' "
        + "LEFT OUTER JOIN tr_mail_message_info_addr send ON info.id = send.msg_id AND send.kind = 'G' "
        + "WHERE vendor_name = ? "
        + "AND endp.policy_id = ? ";

    private static final String SMTP_QUERY
        = SMTP_QUERY_BASE
        + "ORDER BY evt.time_stamp DESC LIMIT ?";

    private static final String SMTP_SPAM_QUERY
        = SMTP_QUERY_BASE
        + "AND is_spam "
        + "ORDER BY evt.time_stamp DESC LIMIT ?";

    private static final String MAIL_QUERY_BASE
        = "SELECT evt.time_stamp, score, "
        + "       CASE WHEN action = 'P' OR NOT is_spam THEN 'PASS' "
        + "            WHEN action = 'M' THEN 'MARK' "
        + "       END AS action, "
        + "       subject, rcpt.addr AS receiver, send.addr AS sender, "
        + "       c_client_addr, c_client_port, s_server_addr, s_server_port, "
        + "       NOT policy_inbound AS incoming "
        + "FROM tr_spam_evt evt "
        + "JOIN tr_mail_message_info info ON evt.msg_id = info.id "
        + "JOIN pl_endp endp ON info.session_id = endp.session_id "
        + "LEFT OUTER JOIN tr_mail_message_info_addr rcpt ON info.id = rcpt.msg_id AND rcpt.kind = 'U' "
        + "LEFT OUTER JOIN tr_mail_message_info_addr send ON info.id = send.msg_id AND send.kind = 'T' "
        + "WHERE vendor_name = ? "
        + "AND endp.policy_id = ? ";

    private static final String MAIL_QUERY
        = MAIL_QUERY_BASE
        + "ORDER BY evt.time_stamp DESC LIMIT ?";

    private static final String MAIL_SPAM_QUERY
        = MAIL_QUERY_BASE
        + "AND is_spam "
        + "ORDER BY evt.time_stamp DESC LIMIT ?";

    private static final String[] QUERIES = new String[]
        { SMTP_QUERY, MAIL_QUERY };

    private static final String[] SPAM_QUERIES = new String[]
        { SMTP_SPAM_QUERY, MAIL_SPAM_QUERY };


    //===============================
    // Defaults for templates

    private static final String OUT_MOD_SUB_TEMPLATE =
      "[SPAM] $MIMEMessage:SUBJECT$";
    private static final String OUT_MOD_BODY_TEMPLATE =
      "The attached message from $MIMEMessage:FROM$ was determined\r\n " +
      "to be SPAM based on a score of $SPAMReport:SCORE$ where anything above $SPAMReport:THRESHOLD$\r\n" +
      "is SPAM.  The details of the report are as follows:\r\n\r\n" +
      "$SPAMReport:FULL$";
    private static final String OUT_MOD_BODY_SMTP_TEMPLATE =
      "The attached message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$) was determined\r\n " +
      "to be SPAM based on a score of $SPAMReport:SCORE$ where anything above $SPAMReport:THRESHOLD$\r\n" +
      "is SPAM.  The details of the report are as follows:\r\n\r\n" +
      "$SPAMReport:FULL$";

    private static final String IN_MOD_SUB_TEMPLATE = OUT_MOD_SUB_TEMPLATE;
    private static final String IN_MOD_BODY_TEMPLATE = OUT_MOD_BODY_TEMPLATE;
    private static final String IN_MOD_BODY_SMTP_TEMPLATE = OUT_MOD_BODY_SMTP_TEMPLATE;

    private static final String SPAM_HEADER_NAME = "X-Spam-Flag";
    private final static String IS_SPAM_HDR_VALUE = "YES";
    private final static String IS_HAM_HDR_VALUE = "NO";

    private static final String OUT_NOTIFY_SUB_TEMPLATE =
      "[SPAM NOTIFICATION] re: $MIMEMessage:SUBJECT$";

    private static final String OUT_NOTIFY_BODY_TEMPLATE =
        "On $MIMEHeader:DATE$ a message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$) was received " + CRLF +
      "and determined to be spam based on a score of $SPAMReport:SCORE$ (where anything " + CRLF +
      "above $SPAMReport:THRESHOLD$ is SPAM).  The details of the report are as follows:" + CRLF + CRLF +
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
    private final Logger logger = Logger.getLogger(getClass());

    private volatile SpamSettings zSpamSettings;

    // constructors -----------------------------------------------------------

    public SpamImpl(SpamScanner scanner)
    {
        this.scanner = scanner;
    }

    // Spam methods -----------------------------------------------------------

    // backwards compat
    public List<SpamLog> getEventLogs(int limit)
    {
        return getEventLogs(limit, false);
    }

    public List<SpamLog> getEventLogs(int limit, boolean spamOnly)
    {
        String[] queries;
        if (spamOnly)
            queries = SPAM_QUERIES;
        else
            queries = QUERIES;

        List<SpamLog> l = new ArrayList<SpamLog>(queries.length * limit);

        for (String q : queries) {
            getEventLogs(q, l, limit);
        }

        Collections.sort(l);

        while (l.size() > limit) { l.remove(l.size() - 1); }

        return l;
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
                    s.saveOrUpdate(newSettings);
                    SpamImpl.this.zSpamSettings = newSettings;

                    reconfigure();
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
      incrementCount(Transform.GENERIC_0_COUNTER);
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
      incrementCount(Transform.GENERIC_2_COUNTER);
    }
    /**
     * Increment the counter for messages marked
     */
    public void incrementMarkCounter() {
      incrementCount(Transform.GENERIC_3_COUNTER);
    }
    /**
     * Increment the count for messages quarantined.
     */
    public void incrementQuarantineCount() {
      //TODO bscott implement me
    }
    

    public void reconfigure() { return; }

    protected void initializeSettings()
    {
        logger.debug("Initializing Settings");

        SpamSettings zTmpSpamSettings = new SpamSettings(getTid());

        zTmpSpamSettings.setSMTPInbound(
          new SpamSMTPConfig(true,
                             SMTPSpamMessageAction.MARK,
                             SMTPNotifyAction.NEITHER,
                             SpamProtoConfig.DEFAULT_STRENGTH,
                             "Scan incoming SMTP e-mail",
                             getDefaultSubjectWrapperTemplate(true),
                             getDefaultBodyWrapperTemplate(true),
                             getDefaultIndicatorHeaderName(),
                             getDefaultIndicatorHeaderValue(true),
                             getDefaultIndicatorHeaderValue(false),
                             getDefaultNotifySubjectTemplate(true),
                             getDefaultNotifyBodyTemplate(true) ));

        zTmpSpamSettings.setSMTPOutbound(
          new SpamSMTPConfig(false,
                             SMTPSpamMessageAction.PASS,
                             SMTPNotifyAction.NEITHER,
                             SpamProtoConfig.DEFAULT_STRENGTH,
                             "Scan outgoing SMTP e-mail",
                             getDefaultSubjectWrapperTemplate(false),
                             getDefaultBodyWrapperTemplate(false),
                             getDefaultIndicatorHeaderName(),
                             getDefaultIndicatorHeaderValue(true),
                             getDefaultIndicatorHeaderValue(false),
                             getDefaultNotifySubjectTemplate(false),
                             getDefaultNotifyBodyTemplate(false)  ));

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

                    reconfigure();
                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);

        return;
    }

    SpamScanner getScanner()
    {
        return scanner;
    }

    // private methods --------------------------------------------------------

    private List<SpamLog> getEventLogs(final String q, final List<SpamLog> l,
                                       final int limit)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s) throws SQLException
                {
                    Connection c = s.connection();
                    PreparedStatement ps = c.prepareStatement(q);
                    ps.setString(1, scanner.getVendorName());
                    ps.setLong(2, getPolicy().getId());
                    ps.setInt(3, limit);
                    long l0 = System.currentTimeMillis();
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        long ts = rs.getTimestamp("time_stamp").getTime();
                        Date timeStamp = new Date(ts);
                        float score = rs.getFloat("score");
                        String action = rs.getString("action");
                        String subject = rs.getString("subject");
                        String receiver = rs.getString("receiver");
                        String sender = rs.getString("sender");
                        String clientAddr = rs.getString("c_client_addr");
                        int clientPort = rs.getInt("c_client_port");
                        String serverAddr = rs.getString("s_server_addr");
                        int serverPort = rs.getInt("s_server_port");
                        boolean incoming = rs.getBoolean("incoming");

                        Direction d = incoming ? Direction.INCOMING : Direction.OUTGOING;

                        SpamLog rl = new SpamLog
                            (timeStamp, score, action, subject, receiver, sender,
                             clientAddr, clientPort, serverAddr, serverPort, d);

                        l.add(rl);
                    }
                    long l1 = System.currentTimeMillis();
                    logger.debug("getSpamLogs() in: " + (l1 - l0));
                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);

        return l;
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
