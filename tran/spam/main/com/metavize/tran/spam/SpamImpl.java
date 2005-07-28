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
import com.metavize.mvvm.tapi.TransformContextFactory;
import com.metavize.mvvm.tran.Direction;
import com.metavize.tran.mail.SMTPNotifyAction;
import com.metavize.tran.token.TokenAdaptor;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;

public class SpamImpl extends AbstractTransform implements SpamTransform
{
    // XXX not yet tested, still need to index, etc...
    private static final String SMTP_QUERY
        = "SELECT evt.time_stamp, score, "
        + "       CASE WHEN action = 'P' THEN 'PASS' "
        + "            WHEN action = 'M' THEN 'MARK' "
        + "            WHEN action = 'B' THEN 'BLOCK' "
        + "       END AS action, "
        + "       subject, rcpt.addr AS receiver, send.addr AS sender, "
        + "       c_client_addr, c_client_port, s_server_addr, s_server_port, "
        + "       client_intf, server_intf "
        + "FROM tr_spam_evt_smtp evt "
        + "JOIN tr_mail_message_info info ON evt.msg_id = info.id "
        + "JOIN pl_endp endp ON info.session_id = endp.session_id "
        + "LEFT OUTER JOIN tr_mail_message_info_addr rcpt ON info.id = rcpt.msg_id AND rcpt.kind = 'B' "
        + "LEFT OUTER JOIN tr_mail_message_info_addr send ON info.id = send.msg_id AND send.kind = 'G' "
        + "WHERE vendor = ? "
        + "ORDER BY evt.time_stamp DESC LIMIT ?";

    private static final String MAIL_QUERY
        = "SELECT evt.time_stamp, score, action, "
        + "       CASE WHEN action = 'P' THEN 'PASS' "
        + "            WHEN action = 'M' THEN 'MARK' "
        + "       END AS action, "
        + "       subject, rcpt.addr AS receiver, send.addr AS sender, "
        + "       c_client_addr, c_client_port, s_server_addr, s_server_port, "
        + "       client_intf, server_intf "
        + "FROM tr_spam_evt evt "
        + "JOIN tr_mail_message_info info ON evt.msg_id = info.id "
        + "JOIN pl_endp endp ON info.session_id = endp.session_id "
        + "LEFT OUTER JOIN tr_mail_message_info_addr rcpt ON info.id = rcpt.msg_id AND rcpt.kind = 'U' "
        + "LEFT OUTER JOIN tr_mail_message_info_addr send ON info.id = send.msg_id AND send.kind = 'T' "
        + "WHERE vendor = ? "
        + "ORDER BY evt.time_stamp DESC LIMIT ?";

    private static final String[] QUERIES = new String[]
        { SMTP_QUERY, MAIL_QUERY };

    private final PipeSpec[] pipeSpecs = new PipeSpec[] {
        new SoloPipeSpec("spam-smtp", this, new TokenAdaptor(new SpamSmtpFactory(this)), Fitting.SMTP_TOKENS, Affinity.SERVER, 0),
        new SoloPipeSpec("pop-smtp", this, new TokenAdaptor(new SpamPopFactory(this)), Fitting.POP_TOKENS, Affinity.SERVER, 0),
        new SoloPipeSpec("imap-smtp", this, new TokenAdaptor(new SpamImapFactory(this)), Fitting.IMAP_TOKENS, Affinity.SERVER, 0)
    };

    private final SpamScanner scanner;
    private final Logger logger = Logger.getLogger(getClass());

    private SpamSettings zSpamSettings;

    // constructors -----------------------------------------------------------

    public SpamImpl(SpamScanner scanner)
    {
        this.scanner = scanner;
    }

    // Spam methods -----------------------------------------------------------

    public List<SpamLog> getEventLogs(int limit)
    {
        List<SpamLog> l = new ArrayList<SpamLog>(QUERIES.length * limit);

        for (String q : QUERIES) {
            getEventLogs(q, l, limit);
        }

        Collections.sort(l);

        for (int i = Math.min(limit, l.size()); i < l.size(); i++) {
            l.remove(i);
        }

        return l;
    }

    // Transform methods ------------------------------------------------------

    public SpamSettings getSpamSettings()
    {
        return this.zSpamSettings;
    }

    public void setSpamSettings(SpamSettings zSpamSettings)
    {
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdateCopy(zSpamSettings);
            this.zSpamSettings = zSpamSettings;

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get SpamSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close hibernate session", exn);
            }
        }

        reconfigure();
        return;
    }

    public void reconfigure() { return; }

    protected void initializeSettings()
    {
        SpamSettings zTmpSpamSettings = new SpamSettings(getTid());

        zTmpSpamSettings.setSMTPInbound(new SpamSMTPConfig(true, SMTPSpamMessageAction.MARK, SMTPNotifyAction.NEITHER, "Scan incoming SMTP e-mail" ));
        zTmpSpamSettings.setSMTPOutbound(new SpamSMTPConfig(false, SMTPSpamMessageAction.PASS, SMTPNotifyAction.NEITHER, "Scan outgoing SMTP e-mail" ));

        zTmpSpamSettings.setPOPInbound(new SpamPOPConfig(true, SpamMessageAction.MARK, "Scan incoming POP e-mail" ));
        zTmpSpamSettings.setPOPOutbound(new SpamPOPConfig(false, SpamMessageAction.PASS, "Scan outgoing POP e-mail" ));

        zTmpSpamSettings.setIMAPInbound(new SpamIMAPConfig(true, SpamMessageAction.MARK, "Scan incoming IMAP e-mail" ));
        zTmpSpamSettings.setIMAPOutbound(new SpamIMAPConfig(false, SpamMessageAction.PASS, "Scan outgoing IMAP e-mail" ));

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
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery
                ("from SpamSettings ss where ss.tid = :tid");
            q.setParameter("tid", getTid());
            zSpamSettings = (SpamSettings)q.uniqueResult();

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get SpamSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close hibernate session", exn);
            }
        }

        return;
    }

    SpamScanner getScanner()
    {
        return scanner;
    }

    // private methods --------------------------------------------------------

    private List<SpamLog> getEventLogs(String q, List<SpamLog> l,
                                          int limit)
    {
        Session s = TransformContextFactory.context().openSession();
        try {
            Connection c = s.connection();
            PreparedStatement ps = c.prepareStatement(q);
            ps.setInt(1, limit);
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
                byte clientIntf = rs.getByte("client_intf");
                byte serverIntf = rs.getByte("server_intf");

                Direction d = Direction.getDirection(clientIntf, serverIntf);

                SpamLog rl = new SpamLog
                    (timeStamp, score, action, subject, receiver, sender,
                     clientAddr, clientPort, serverAddr, serverPort, d);

                l.add(0, rl);
            }
            long l1 = System.currentTimeMillis();
            logger.debug("getActiveXLogs() in: " + (l1 - l0));
        } catch (SQLException exn) {
            logger.warn("could not get events", exn);
        } catch (HibernateException exn) {
            logger.warn("could not get events", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Hibernate session", exn);
            }
        }

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
