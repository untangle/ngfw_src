/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import com.metavize.mvvm.MailSender;
import com.metavize.mvvm.MailSettings;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.security.AdminSettings;
import com.metavize.mvvm.security.User;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.jar.JarFile;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.MimetypesFileTypeMap;
import javax.mail.*;
import javax.mail.internet.*;
import com.sun.mail.smtp.SMTPTransport;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Transaction;

/**
 * Note that this class is designed to be used <b>BOTH</b> inside the MVVM and
 * as a stand-alone application. The stand-alone mode is used for mailing out
 * EdgeReports.
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
 * @version 1.0
 */
class MailSenderImpl implements MailSender
{
    public static final String DEFAULT_FROM_ADDRESS = "reports@local.domain";

    // All error log emails go here.
    public static final String ERROR_LOG_RECIPIENT = "exceptions@metavize.com";

    public static final String METAVIZE_SMTP_RELAY = "mail.metavize.com";

    public static final String Mailer = "MVVM MailSender";

    // --

    private static final String MAIL_HOST_PROP = "mail.host";
    private static final String MAIL_FROM_PROP = "mail.from";
    private static final String MAIL_TRANSPORT_PROTO_PROP = "mail.transport.protocol";

    private static final Object LOCK = new Object();

    private static MailSenderImpl MAIL_SENDER;

    public static boolean SessionDebug = false;

    private static final MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();

    private MailSettings mailSettings;

    // This is the session used to send alert mail inside the organization, including
    // SMTP spam/virus notification.
    private Session alertSession;

    // This is the session used to send report mail inside the organization
    private Session reportSession;

    // This is the session used to send mail to Metavize Inc.
    private Session mvSession;

    private static final Logger logger = Logger.getLogger(MailSenderImpl.class.getName());

    // NOTE: Only used for stand-alone operation.
    private org.hibernate.SessionFactory sessionFactory;

    private MailSenderImpl() {
        sessionFactory = null;
        init();
    }

    private MailSenderImpl(org.hibernate.SessionFactory sessionFactory)
    {
        this.sessionFactory = sessionFactory;
        init();
    }

    private void init()
    {
        mimetypesFileTypeMap.addMimeTypes("application/pdf pdf PDF");
        mimetypesFileTypeMap.addMimeTypes("text/css css CSS");

        org.hibernate.Session s = getSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery("from MailSettings");
            mailSettings = (MailSettings)q.uniqueResult();

            if (null == mailSettings) {
                logger.info("Creating initial default mail settings");
                mailSettings = new MailSettings();
                mailSettings.setFromAddress(DEFAULT_FROM_ADDRESS);
                s.save(mailSettings);
            } else {
                refreshSessions();
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

    private org.hibernate.Session getSession()
    {
        if (sessionFactory == null) {
            return MvvmContextFactory.context().openSession();
        } else {
            org.hibernate.Session s = null;

            try {
                s = sessionFactory.openSession();
            } catch (HibernateException exn) {
                logger.warn("Could not create Hibernate Session", exn);
            }

            return s;
        }
    }

    public void setMailSettings(MailSettings settings)
    {
        org.hibernate.Session s = getSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdate(mailSettings);

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

        mailSettings = settings;
        refreshSessions();
    }

    public MailSettings getMailSettings()
    {
        return mailSettings;
    }

    // Called when settings updated.
    private void refreshSessions() {
        Properties commonProps = new Properties();
        if (mailSettings.getSmtpHost() != null && !"".equals(mailSettings.getSmtpHost()))
            commonProps.put(MAIL_HOST_PROP, mailSettings.getSmtpHost());
        commonProps.put(MAIL_FROM_PROP, mailSettings.getFromAddress());
        commonProps.put(MAIL_TRANSPORT_PROTO_PROP, "smtp");

        Properties mvProps = (Properties) commonProps.clone();
        // This one always uses our SMTP host.
        mvProps.put(MAIL_HOST_PROP, METAVIZE_SMTP_RELAY);
        // What we really want here is something that will uniquely identify the customer,
        // including the serial # stamped on the CD. XXXX
        mvSession = Session.getInstance(mvProps);

        Properties alertProps = (Properties) commonProps.clone();
        alertSession = Session.getInstance(alertProps);

        Properties reportProps = (Properties) commonProps.clone();
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
            sendSimple(alertSession, recipients, subject, bodyText, null);
        } else {
            MimeBodyPart part = makeAttachmentFromList(attachment);
            sendSimple(alertSession, recipients, subject, bodyText, part);
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

    // Not currently used
    public void sendReport(String subject, String bodyText) {
        String reportEmailAddr = mailSettings.getReportEmail();
        if (reportEmailAddr == null) {
            logger.info("Not sending report email, no address");
        } else {
            String[] recipients = new String[1];
            recipients[0] = reportEmailAddr;
            sendSimple(reportSession, recipients, subject, bodyText, null);
        }
    }

    public void sendReports(String subject, String bodyHTML, List<String> extraLocations, List<File> extras) {
        String reportEmailAddr = mailSettings.getReportEmail();
        if (reportEmailAddr == null) {
            logger.info("Not sending report email, no address");
        } else {
            String[] recipients = new String[1];
            recipients[0] = reportEmailAddr;

            if (extraLocations == null && extras == null) {
                // Do this simplest thing.  Shouldn't be used. XX
                sendSimple(reportSession, recipients, subject, bodyHTML, null);
                return;
            } else if ((extraLocations == null && extras != null) ||
                       (extraLocations != null && extras == null) ||
                       (extraLocations.size() != extras.size())) {
                throw new IllegalArgumentException("sendReports mismatch of locations and extras");
            }

            List<MimeBodyPart> parts = new ArrayList<MimeBodyPart>();

            try {
                for (int i = 0; i < extras.size(); i++) {
                    String location = extraLocations.get(i);
                    File extra = extras.get(i);
                    DataSource ds = new FileDataSource(extra);
            ((FileDataSource)ds).setFileTypeMap( mimetypesFileTypeMap );
                    DataHandler dh = new DataHandler(ds);
                    MimeBodyPart part = new MimeBodyPart();
                    part.setDataHandler(dh);
                    part.setHeader("Content-Location", location);
                    part.setFileName(extra.getName());
                    parts.add(part);
                }
            } catch (MessagingException x) {
                logger.error("Unable to parse extras", x);
                return;
            }

            sendRelated(reportSession, recipients, subject, bodyHTML, parts);
        }
    }

    public void sendErrorLogs(String subject, String bodyText, List<MimeBodyPart> parts) {
        String[] recipients = new String[1];
        recipients[0] = ERROR_LOG_RECIPIENT;

        // New behavior 8/15/05: First try our own mail server, if that doesn't work
        // (they have a firewall rule preventing it, for instance), try their email
        // server.  If that doesn't work, go ahead and drop it.  jdi
        Session[] trySessions = new Session[] { mvSession, alertSession };
        boolean success = false;
        for (Session session : trySessions) {
            if (parts == null) {
                // Do this simplest thing.  Shouldn't be used. XX
                success = sendSimple(session, recipients, subject, bodyText, null);
            } else {
                success = sendMixed(session, recipients, subject, bodyText, parts);
            }
            if (success)
                break;
        }
        if (!success) {
            logger.error("Unable to send exception email, dropping");
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
            sendSimple(alertSession, recipients, subject, bodyText, null);
        } else {
            MimeBodyPart part = makeAttachmentFromList(attachment);
            sendSimple(alertSession, recipients, subject, bodyText, part);
        }
    }

    /*
     * See doc on interface
     */
    public boolean sendMessage(InputStream msgStream) {
      //TODO bscott Need better error handling
      //TODO bscott by using JavaMail, we don't seem to be able to have
      //     a null ("<>") MAIL FROM.  This is a violation of some spec
      //     or another, which declares that the envelope from should
      //     be blank for notifications (so other servers don't send
      //     dead letters causing a loop).

      MimeMessage msg = null;

      try {
        msg = new MimeMessage(alertSession, msgStream);
        msg.setHeader("X-Mailer", Mailer);
      }
      catch(Exception ex) {
        logger.error("Unable to convert input stream to MIMEMessage", ex);
        return false;
      }

      //Send the message
      try {
        dosend(alertSession, msg);
        logIt(msg);
        return true;
      }
      catch(Exception ex) {
        logger.warn("Unable to send Message", ex);
        return false;
      }
    }


    private Message prepMessage(Session session, String[] to, String subject)
    {
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
            return null;
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
            msg.setHeader("X-Mailer", Mailer);
            msg.setSentDate(new Date());
            return msg;
        } catch (MessagingException x) {
            logger.error("Unable to send message", x);
            return null;
        }
    }

    private void logIt(Message msg)
        throws MessagingException
    {
        if (logger.isInfoEnabled()) {
            StringBuffer sb = new StringBuffer("Successfully sent message '");
            sb.append(msg.getSubject());
            sb.append("' to ");
            sb.append(msg.getRecipients(Message.RecipientType.TO).length);
            sb.append(" recipients (");
            for (int i = 0; i < msg.getRecipients(Message.RecipientType.TO).length; i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append(msg.getRecipients(Message.RecipientType.TO)[i]);
            }
            sb.append(")");
            logger.info(sb.toString());
        }
    }

    boolean sendSimple(Session session, String[] to, String subject,
                       String bodyText, MimeBodyPart attachment)
    {
        if (SessionDebug)
            session.setDebug(true);

        // construct the message
        Message msg = prepMessage(session, to, subject);
        if (msg == null)
            // Nevermind after all.
            return true;

        try {
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

            // send it
            dosend(session, msg);
            logIt(msg);
            return true;
        } catch (MessagingException x) {
            logger.warn("Unable to send message", x);
            return false;
        }
    }

    boolean sendRelated(Session session, String[] to, String subject,
                        String bodyHTML, List<MimeBodyPart> extras)
    {
        if (SessionDebug)
            session.setDebug(true);

        // construct the message
        Message msg = prepMessage(session, to, subject);
        if (msg == null)
            // Nevermind after all.
            return true;

        try {
            Multipart mp = new MimeMultipart("related");
            MimeBodyPart main = new MimeBodyPart();
            main.setContent(bodyHTML, "text/html");
            // main.setDisposition(Part.INLINE);
            mp.addBodyPart(main);
            for (MimeBodyPart part : extras)
                mp.addBodyPart(part);
            msg.setContent(mp);

            // send it
            dosend(session, msg);
            logIt(msg);
            return true;
        } catch (MessagingException x) {
            logger.warn("Unable to send message", x);
            return false;
        }
    }

    boolean sendMixed(Session session, String[] to, String subject,
                   String bodyText, List<MimeBodyPart> extras)
    {
        if (SessionDebug)
            session.setDebug(true);

        // construct the message
        Message msg = prepMessage(session, to, subject);
        if (msg == null)
            // Nevermind after all.
            return true;

        try {
            Multipart mp = new MimeMultipart("mixed");
            MimeBodyPart main = new MimeBodyPart();
            main.setText(bodyText);
            // main.setDisposition(Part.INLINE);
            mp.addBodyPart(main);
            for (MimeBodyPart part : extras)
                mp.addBodyPart(part);
            msg.setContent(mp);

            // send it
            dosend(session, msg);
            logIt(msg);
            return true;
        } catch (MessagingException x) {
            logger.warn("Unable to send message", x);
            return false;
        }
    }

    // --

    // Here's where we actually do the sending
    private void dosend(Session session, Message msg)
        throws MessagingException
    {
        SMTPTransport transport = null;
        try {
            transport = (SMTPTransport) session.getTransport();
            // We get the host from the session since it can differ (mv errors).
            String host = session.getProperty(MAIL_HOST_PROP);
            String localhost = mailSettings.getLocalHostName();
            int port = mailSettings.getSmtpPort();
            String user = mailSettings.getAuthUser();
            String pass = mailSettings.getAuthPass();
            if ((user == null && pass != null) || (user != null && pass == null)) {
                logger.warn("SMTP AUTH user/pass -- only one set, ignoring");
                user = null;
                pass = null;
            }
            transport.setStartTLS(mailSettings.isUseTls());
            transport.setLocalHost(mailSettings.getLocalHostName());
            transport.connect(host, port, user, pass);
            transport.send(msg);
        } catch (MessagingException x) {
            throw x;
        } catch (Exception x) {
            // Uh oh...
            logger.error("Unexpected exception in dosend", x);
            throw new MessagingException("Unexpected exception in dosend", x);
        } finally {
            try { if (transport != null) transport.close(); } catch (MessagingException x) { }
        }
    }

    // --

    private static void usage() {
        System.err.println("usage: mail-reports [-s subject] bodyhtmlfile { extrafile }");
        System.exit(1);
    }

    public static void main(String[] args)
    {
        String subject = "Metavize EdgeGuard Reports"; // XXX Make default unsuck.
        JarFile mvvmJarFile = null;
        File bodyFile = null;
        List<File> extraFiles = new ArrayList<File>();
        List<String> extraLocations = new ArrayList<String>();

        try {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-s")) {
                    subject = args[++i];
                } else if (mvvmJarFile == null) {
                    mvvmJarFile = new JarFile(args[i]);
                } else if (bodyFile == null) {
                    bodyFile = new File(args[i]);
                } else {
                    // For now just use the name plus two parent directories as the content-location
                    String extraLocation;
                    File extraFile = new File(args[i]);
                    File parentFile = extraFile.getParentFile();
                    if (parentFile == null) {
                        extraLocation = extraFile.getName();
                    } else {
                        File grandParentFile = parentFile.getParentFile();
                        // XXX super ugly fixme XXX
                        if ((parentFile.getName().equals("images") && !extraFile.getName().endsWith(".png")) ||
                            grandParentFile == null) {
                            extraLocation = parentFile.getName() + File.separator + extraFile.getName();
                        } else {
                            extraLocation = grandParentFile.getName() + File.separator + parentFile.getName() + File.separator + extraFile.getName();
                        }
                    }

                    extraLocations.add(extraLocation);
                    extraFiles.add(extraFile);
                }
            }
            if (mvvmJarFile == null || bodyFile == null)
                usage();

            List<JarFile> jfs = new ArrayList<JarFile>();
            jfs.add(mvvmJarFile);
            org.hibernate.SessionFactory sessionFactory = Util.makeStandaloneSessionFactory(jfs);
            MailSenderImpl us = new MailSenderImpl(sessionFactory);

            // Read in the body file.
            FileReader frmain = new FileReader(bodyFile);
            StringBuilder sbmain = new StringBuilder();
            char[] buf = new char[1024];
            int rs;
            while ((rs = frmain.read(buf)) > 0)
                sbmain.append(buf, 0, rs);
            frmain.close();
            String bodyHTML = sbmain.toString();

            us.sendReports(subject, bodyHTML, extraLocations, extraFiles);
        } catch (IOException x) {
            System.err.println("Unable to send message" + x);
            x.printStackTrace();
            System.exit(2);
        }

    }
}
