/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: MailSenderImpl.java,v 1.7 2005/02/04 09:19:17 amread Exp $
 */

package com.metavize.mvvm.engine;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

import com.metavize.mvvm.MailSender;
import com.metavize.mvvm.MailSettings;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.security.AdminSettings;
import com.metavize.mvvm.security.User;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;

public class MailSenderImpl implements MailSender
{
    private static final Object LOCK = new Object();

    private static MailSenderImpl MAIL_SENDER;

    public static boolean SessionDebug = false;

    public static final String Mailer = "MVVM MailSender";

    // This is the session used to send alert mail inside the organization
    private Session alertSession;

    // This is the session used to send report mail inside the organization
    private Session reportSession;

    // This is the session used to send mail to Metavize Inc.
    private Session mvSession;

    private Logger logger;

    private MailSenderImpl() {
        logger = Logger.getLogger(MailSender.class.getName());

        net.sf.hibernate.Session s = MvvmContextFactory.context()
            .openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery("from MailSettings");
            MailSettings ms = (MailSettings)q.uniqueResult();

            if (null == ms) {
                logger.info("Creating initial default mail settings");
                ms = new MailSettings();
            } else {
                refreshSessions(ms);
            }

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get MailSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Session", exn);
            }
        }
    }

    static MailSenderImpl mailSender() {
        synchronized (LOCK) {
            if (null == MAIL_SENDER) {
                MAIL_SENDER = new MailSenderImpl();
            }
        }
        return MAIL_SENDER;
    }

    public void setMailSettings(MailSettings settings)
    {
        net.sf.hibernate.Session s = MvvmContextFactory.context()
            .openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdateCopy(settings);

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not save MailSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Session", exn);
            }
        }

        refreshSessions(settings);
    }

    public MailSettings getMailSettings()
    {
        MailSettings ms = null;

        net.sf.hibernate.Session s = MvvmContextFactory.context()
            .openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery("from MailSettings ms");
            ms = (MailSettings)q.uniqueResult();

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not save MailSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close session", exn);
            }
        }

        return ms;
    }

    // Called when settings updated.
    private void refreshSessions(MailSettings settings) {
        Properties mvProps = new Properties();
        mvProps.put("mail.host", "mail.metavize.com");
        // What we really want here is something that will uniquely identify the customer,
        // including the serial # stamped on the CD. XXXX
        mvProps.put("mail.from", settings.getFromAddress());
        mvSession = Session.getInstance(mvProps);

        Properties alertProps = new Properties();
        if (settings.getSmtpHost() != null)
            alertProps.put("mail.host", settings.getSmtpHost());
        alertProps.put("mail.from", settings.getFromAddress());
        alertSession = Session.getInstance(alertProps);

        Properties reportProps = new Properties();
        if (settings.getSmtpHost() != null)
            reportProps.put("mail.host", settings.getSmtpHost());
        reportProps.put("mail.from", settings.getFromAddress());
        reportSession = Session.getInstance(reportProps);
    }

    private static final String[] RECIPIENTS_PROTO = new String[0];

    public void sendAlert(String subject, String bodyText) {
        sendAlertWithAttachment(subject, bodyText, null);
    }

    public void sendAlertWithAttachment(String subject, String bodyText, List attachment) {
        // Compute the list of recipients from the user list.
        AdminSettings adminSettings = MvvmContextFactory.context()
            .adminManager().getAdminSettings();
        Set users = adminSettings.getUsers();
        List alertableUsers = new ArrayList();
        for (Iterator iter = users.iterator(); iter.hasNext();) {
            User user = (User) iter.next();
            String userEmail = user.getEmail();
            if (userEmail == null) {
                if (logger.isDebugEnabled())
                    logger.debug("Skipping user " + user.getLogin()
                                 + " with no email address");
            } else {
                if (!user.getSendAlerts())
                    logger.debug("Skipping user " + user.getLogin()
                                 + " with sendAlerts off");
                else
                    alertableUsers.add(userEmail);
            }
        }

        String[] recipients = (String[]) alertableUsers.toArray(RECIPIENTS_PROTO);
        if (recipients.length == 0) {
            logger.warn("Not sending alert email, no recipients");
        } else if (attachment == null) {
            sendit(alertSession, recipients, subject, bodyText, null);
        } else {
            MimeBodyPart part = makeAttachmentFromList(attachment);
            sendit(alertSession, recipients, subject, bodyText, part);
        }
    }

    private MimeBodyPart makeAttachmentFromList(List list) {
        if (list == null) return null;

        int bodySize = 0;
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            ByteBuffer buf = (ByteBuffer) iter.next();
            bodySize += buf.remaining();
        }
        byte[] text = new byte[bodySize];
        int pos = 0;
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            ByteBuffer buf = (ByteBuffer) iter.next();
            int size = buf.remaining();
            buf.get(text, pos, size);
            pos += size;
        }

        try {
            MimeBodyPart attachment = new MimeBodyPart(new InternetHeaders(), text);
            return attachment;
        } catch (MessagingException x) {
            logger.error("Unable to make attachment", x);
            return null;
        }
    }

    public void sendReport(String subject, String bodyText) {
        String reportEmailAddr = getMailSettings().getReportEmail();
        if (reportEmailAddr == null) {
            logger.info("Not sending report email, no address");
        } else {
            String[] recipients = new String[1];
            recipients[0] = reportEmailAddr;
            sendit(reportSession, recipients, subject, bodyText, null);
        }
    }

    public void sendMessage(String[] recipients, String subject,
                            String bodyText)
    {
        sendMessageWithAttachment(recipients, subject, bodyText, null);
    }

    public void sendMessageWithAttachment(String[] recipients, String subject,
                                          String bodyText, List attachment)
    {
        if (attachment == null) {
            sendit(alertSession, recipients, subject, bodyText, null);
        } else {
            MimeBodyPart part = makeAttachmentFromList(attachment);
            sendit(alertSession, recipients, subject, bodyText, part);
        }
    }


    void sendit(Session session, String[] to, String subject,
                String bodyText, MimeBodyPart attachment)
    {
        if (SessionDebug)
            session.setDebug(true);

        // construct the message
        Message msg = new MimeMessage(session);

        // come up with all recipients
        Address[][] addrs = new Address[to.length][];
        for (int i = 0; i < to.length; i++) {
            try {
                addrs[i] = InternetAddress.parse(to[i], false);
            } catch (AddressException x) {
                logger.error("Failed to parse receipient address " + to[i] + ", ignoring");
                addrs[i] = null;
            }
        }
        int addrCount = 0;
        for (int i = 0; i < addrs.length; i++)
            if (addrs[i] != null)
                addrCount += addrs[i].length;
        if (addrCount == 0) {
            logger.warn("No recipients for email, ignoring");
            return;
        }

        Address[] recipients = new Address[addrCount];
        for (int i = 0, c = 0; i < addrs.length; i++) {
            if (addrs[i] != null) {
                for (int j = 0; j < addrs[i].length; j++)
                    recipients[c++] = addrs[i][j];
            }
        }

        try {
            msg.setRecipients(Message.RecipientType.TO, recipients);
            // msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc, false));
            // msg.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bcc, false));
            msg.setFrom();
            msg.setSubject(subject);
            if (attachment == null) {
                msg.setText(bodyText);
            } else {
                MimeBodyPart main = new MimeBodyPart();
                main.setText(bodyText);
                main.setDisposition(Part.INLINE);
                Multipart mp = new MimeMultipart();
                mp.addBodyPart(main);
                mp.addBodyPart(attachment);
                msg.setContent(mp);
            }
            msg.setHeader("X-Mailer", Mailer);
            msg.setSentDate(new Date());

            // send it
            Transport.send(msg);
            if (logger.isInfoEnabled()) {
                StringBuffer sb = new StringBuffer("Successfully sent message '");
                sb.append(subject);
                sb.append("' to ");
                sb.append(recipients.length);
                sb.append(" recipients (");
                for (int i = 0; i < recipients.length; i++) {
                    if (i > 0)
                        sb.append(", ");
                    sb.append(recipients[i]);
                }
                sb.append(")");
                logger.info(sb.toString());
            }
        } catch (MessagingException x) {
            logger.error("Unable to send message", x);
        }
    }
}
