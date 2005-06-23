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

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.MimetypesFileTypeMap;

import com.metavize.mvvm.MailSender;
import com.metavize.mvvm.MailSettings;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.NetworkingConfiguration;
import com.metavize.mvvm.security.AdminSettings;
import com.metavize.mvvm.security.User;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;
import org.logicalcobwebs.proxool.ProxoolException;
import org.logicalcobwebs.proxool.ProxoolFacade;

/**
 * Note that this class is designed to be used <b>BOTH</b> inside the MVVM and
 * as a stand-alone application. The stand-alone mode is used for mailing out
 * EdgeReports.
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
 * @version 1.0
 */
public class MailSenderImpl implements MailSender
{
    public static final String DEFAULT_FROM_ADDRESS = "reports@local.domain";

    // All error log emails go here.
    public static final String ERROR_LOG_RECIPIENT = "exceptions@metavize.com";

    public static final String METAVIZE_SMTP_RELAY = "mail.metavize.com";

    public static final String Mailer = "MVVM MailSender";

    // --

    private static final String MAIL_HOST_PROP = "mail.host";
    private static final String MAIL_FROM_PROP = "mail.from";
    private static final Object LOCK = new Object();

    private static MailSenderImpl MAIL_SENDER;

    public static boolean SessionDebug = false;

    private static final MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();

    // This is the session used to send alert mail inside the organization
    private Session alertSession;

    // This is the session used to send report mail inside the organization
    private Session reportSession;

    // This is the session used to send mail to Metavize Inc.
    private Session mvSession;

    private static final Logger logger = Logger.getLogger(MailSenderImpl.class.getName());

    // NOTE: Only used for stand-alone operation.
    private net.sf.hibernate.SessionFactory sessionFactory;

    private MailSenderImpl() {
        sessionFactory = null;
        init();
    }

    private MailSenderImpl(net.sf.hibernate.SessionFactory sessionFactory)
    {
        this.sessionFactory = sessionFactory;
        init();
    }

    private void init()
    {
	mimetypesFileTypeMap.addMimeTypes("application/pdf pdf PDF");
	mimetypesFileTypeMap.addMimeTypes("text/css css CSS");

        net.sf.hibernate.Session s = getSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery("from MailSettings");
            MailSettings ms = (MailSettings)q.uniqueResult();

            if (null == ms) {
                logger.info("Creating initial default mail settings");
                ms = new MailSettings();
                ms.setFromAddress(DEFAULT_FROM_ADDRESS);
                s.save(ms);
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

    private net.sf.hibernate.Session getSession()
    {
        if (sessionFactory == null) {
            return MvvmContextFactory.context().openSession();
        } else {
            net.sf.hibernate.Session s = null;

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
        net.sf.hibernate.Session s = getSession();
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

        net.sf.hibernate.Session s = getSession();
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
        // If they have set a SMTP host, trust it (since we might not be able to route
        // mail out at all directly), otherwise use us directly.
        if (settings.getSmtpHost() != null)
            mvProps.put(MAIL_HOST_PROP, settings.getSmtpHost());
        else
            mvProps.put(MAIL_HOST_PROP, METAVIZE_SMTP_RELAY);
        // What we really want here is something that will uniquely identify the customer,
        // including the serial # stamped on the CD. XXXX
        mvProps.put(MAIL_FROM_PROP, settings.getFromAddress());
        mvSession = Session.getInstance(mvProps);

        Properties alertProps = new Properties();
        if (settings.getSmtpHost() != null)
            alertProps.put(MAIL_HOST_PROP, settings.getSmtpHost());
        alertProps.put(MAIL_FROM_PROP, settings.getFromAddress());
        alertSession = Session.getInstance(alertProps);

        Properties reportProps = new Properties();
        if (settings.getSmtpHost() != null)
            reportProps.put(MAIL_HOST_PROP, settings.getSmtpHost());
        reportProps.put(MAIL_FROM_PROP, settings.getFromAddress());
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
        String reportEmailAddr = getMailSettings().getReportEmail();
        if (reportEmailAddr == null) {
            logger.info("Not sending report email, no address");
        } else {
            String[] recipients = new String[1];
            recipients[0] = reportEmailAddr;
            sendSimple(reportSession, recipients, subject, bodyText, null);
        }
    }

    public void sendReports(String subject, String bodyHTML, List<String> extraLocations, List<File> extras) {
        String reportEmailAddr = getMailSettings().getReportEmail();
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

        if (parts == null) {
            // Do this simplest thing.  Shouldn't be used. XX
            sendSimple(mvSession, recipients, subject, bodyText, null);
        } else {
            sendMixed(mvSession, recipients, subject, bodyText, parts);
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

    void sendSimple(Session session, String[] to, String subject,
                    String bodyText, MimeBodyPart attachment)
    {
        if (SessionDebug)
            session.setDebug(true);

        // construct the message
        Message msg = prepMessage(session, to, subject);
        if (msg == null)
            // Nevermind after all.
            return;

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
            Transport.send(msg);
            logIt(msg);
        } catch (MessagingException x) {
            logger.error("Unable to send message", x);
        }
    }

    void sendRelated(Session session, String[] to, String subject,
                     String bodyHTML, List<MimeBodyPart> extras)
    {
        if (SessionDebug)
            session.setDebug(true);

        // construct the message
        Message msg = prepMessage(session, to, subject);
        if (msg == null)
            // Nevermind after all.
            return;

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
            Transport.send(msg);
            logIt(msg);
        } catch (MessagingException x) {
            logger.error("Unable to send message", x);
        }
    }

    void sendMixed(Session session, String[] to, String subject,
                   String bodyText, List<MimeBodyPart> extras)
    {
        if (SessionDebug)
            session.setDebug(true);

        // construct the message
        Message msg = prepMessage(session, to, subject);
        if (msg == null)
            // Nevermind after all.
            return;

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
            Transport.send(msg);
            logIt(msg);
        } catch (MessagingException x) {
            logger.error("Unable to send message", x);
        }
    }

    private static void usage() {
        System.err.println("usage: mail-reports [-s subject] bodyhtmlfile { extrafile }");
        System.exit(1);
    }


    // This *so* does not belong here. XXXXXXXXXXXXXXXXXXXXXXXX
    private static void initJdbcPool()
    {
        // logger.info("Initializing Proxool");
        try {
            Class.forName("org.logicalcobwebs.proxool.ProxoolDriver");
        } catch (ClassNotFoundException exn) {
            throw new RuntimeException("could not load Proxool", exn);
        }
        Properties info = new Properties();
        info.setProperty("proxool.maximum-connection-count", "10");
        info.setProperty("proxool.house-keeping-test-sql", "select CURRENT_DATE");
        /* XXX not for production: */
        info.setProperty("proxool.statistics", "1m,15m,1d");
        info.setProperty("user", "metavize");
        info.setProperty("password", "foo");
        String alias = "mvvm";
        String driverClass = "org.postgresql.Driver";
        String driverUrl = "jdbc:postgresql://localhost/mvvm";
        String jdbcUrl = "proxool." + alias + ":" + driverClass + ":" + driverUrl;
        try {
            ProxoolFacade.registerConnectionPool(jdbcUrl, info);
        } catch (ProxoolException exn) {
            // logger.debug("could not set up Proxool", exn);
        }

        String bunniculaHome = System.getProperty("bunnicula.home");

        System.setProperty("derby.system.home", bunniculaHome + "/db");
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

            initJdbcPool();
            List<JarFile> jfs = new ArrayList<JarFile>();
            jfs.add(mvvmJarFile);
            net.sf.hibernate.SessionFactory sessionFactory = Util.makeStandaloneSessionFactory(jfs);
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
            ProxoolFacade.shutdown(0);
        } catch (IOException x) {
            System.err.println("Unable to send message" + x);   
            x.printStackTrace();
            System.exit(2);
        }

    }
}
