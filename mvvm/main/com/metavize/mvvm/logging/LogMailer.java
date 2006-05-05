/*
 * Copyright (c) 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.logging;

import java.io.*;
import java.util.*;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;

import com.metavize.mvvm.MailSender;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.MvvmLocalContext;
import com.metavize.mvvm.NetworkingConfiguration;
import com.metavize.mvvm.engine.Version;
import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.TransformManager;
import org.apache.log4j.Logger;


public class LogMailer implements Runnable
{
    public static final String SUBJECT_BASE = "EdgeGuard Logs";
    public static final String BODY_BASE = "Error Logs";

    // Generate no more than one email per this long (5 minutes).
    public static final long MIN_MESSAGE_PERIOD = 300000;

    // This many lines from the end of non-log4j logs (console, gc) for each message
    public static final int OTHER_LOG_LINES = 50;

    private static final Logger logger = Logger
        .getLogger(LogMailer.class);

    private static final Object LOCK = new Object();
    private static LogMailer LOGMAILER;

    private final Map<Tid, SMTPAppender> loggers;

    private String from;
    private String subject;
    private SystemStatus sysstat;

    private long lastSendTime = 0;

    private volatile Thread thread;

    private Object sendMonitor = new Object();
    private Tid sendTriggerer = null;

    private LogMailer()
    {
        loggers = new HashMap<Tid, SMTPAppender>();
        this.sysstat = new SystemStatus();
        thread = new Thread(this);
        thread.start();
    }

    public static LogMailer get()
    {
        if (null == LOGMAILER) {
            synchronized (LOCK) {
                if (null == LOGMAILER) {
                    LOGMAILER = new LogMailer();
                }
            }
        }
        return LOGMAILER;
    }

    public void stop()
    {
        this.sysstat.destroy();
        Thread t = thread;
        thread = null;
        t.interrupt();
    }

    void register(Tid tid, SMTPAppender appender)
    {
        synchronized(loggers) {
            loggers.put(tid, appender);
        }
    }

    public void unregister(Tid tid)
    {
        synchronized(loggers) {
            loggers.remove(tid);
        }
    }

    // Runnable methods ---------------------------------------------------

    public void run()
    {
        while (null != thread) {
            try {
                Tid triggerer;
                synchronized (sendMonitor) {
                    sendMonitor.wait();
                    triggerer = sendTriggerer;
                    sendTriggerer = null;
                }
                long now = System.currentTimeMillis();
                if (now - MIN_MESSAGE_PERIOD < lastSendTime)
                    Thread.sleep(MIN_MESSAGE_PERIOD  - (now - lastSendTime));
                if (MvvmContextFactory.state() == MvvmLocalContext.MvvmState.RUNNING &&
                    MvvmContextFactory.context().networkingManager().get().isExceptionReportingEnabled())
                    sendMessage(triggerer);
                lastSendTime = System.currentTimeMillis();
            } catch (InterruptedException x) {
                // Normal
                logger.info("exiting from interrupt");
            }  catch (Exception exn) {
                logger.warn("danger, danger, will robinson", exn); // never die
            }
//            l.clear();
        }
    }

    // Called from one of the SMTPAppenders to indicate the need to send.
    void sendBuffer(Tid tid)
    {
        synchronized(sendMonitor) {
            // Might just update the value, but that's ok -- we'll still end up putting
            // an interesting error log first.
            sendTriggerer = tid;
            sendMonitor.notify();
        }
    }

  /**
     Send the contents of all the cyclic buffers as an e-mail message.
   */
    protected void sendMessage(Tid triggeringTid) {
        try {
            TransformManager tm = MvvmContextFactory.context().transformManager();
            ArrayList<MimeBodyPart> parts = new ArrayList<MimeBodyPart>();
            for (Tid tid : loggers.keySet()) {
                SMTPAppender sapp = loggers.get(tid);
                String partName;
                if (tid.getId() == 0L) {
                    partName = "MVVM Log";
                } else {
                    TransformContext tc = tm.transformContext(tid);
                    partName = tc.getTransformDesc().getName() + ".log";
                }
                MimeBodyPart part = sapp.getPart();
                if (part != null) {
                    part.setFileName(partName);
                    part.setDisposition(Part.INLINE);
                    if (tid.equals(triggeringTid))
                        parts.add(0, part);
                    else
                        parts.add(part);
                }
            }
            // Finally, get some of non-log4j logs: console.log and gc.log

            MimeBodyPart part = getOtherLogPart("console.log");
            if (part != null) {
                part.setFileName("console.log");
                part.setDisposition(Part.INLINE);
                parts.add(part);
            }

            part = getOtherLogPart("gc.log");
            if (part != null) {
                part.setFileName("gc.log");
                part.setDisposition(Part.INLINE);
                parts.add(part);
            }

            // Send it!
            doSend(SUBJECT_BASE, BODY_BASE, parts);
        } catch(Exception e) {
            logger.warn("Error occured while sending e-mail notification.", e);
        }
    }

    private void doSend(String subjectBase, String bodyBase, List<MimeBodyPart> parts) {
        NetworkingConfiguration netConf = MvvmContextFactory.context().networkingManager().get();
        String host = netConf.host().toString();

        String bodyText = sysstat.systemStatus();
        String version = Version.getVersion();

        StringBuilder sb = new StringBuilder(subjectBase);
        sb.append(" (");
        sb.append(host);
        sb.append(")");
        sb.append(" v");
        sb.append(version);
        String subjectText = sb.toString();

        MailSender ms = MvvmContextFactory.context().mailSender();
        ms.sendErrorLogs(subjectText, bodyText, parts);
    }


    private static final int OTHER_BUF_SIZE = 4096;

    // Because we use RandomAccessFile in here, we're using 8-bit chars (not Locale I18N friendly).  XXX
    private MimeBodyPart getOtherLogPart(String logFileName)
        throws MessagingException
    {
        String bunniculaLog = System.getProperty("bunnicula.log.dir");
        String otherLog = bunniculaLog + File.separator + logFileName;
        ArrayList<String> lastLines = new ArrayList<String>(OTHER_LOG_LINES);
        try {
            RandomAccessFile olfile =  new RandomAccessFile(otherLog, "r");

            byte[] buf = new byte[OTHER_BUF_SIZE];
            long otherLogLen = olfile.length();
            long pos = otherLogLen - OTHER_BUF_SIZE;
            StringBuilder curline = null;
            while (pos >= 0) {
                olfile.seek(pos);
                int numread = olfile.read(buf);
                if (numread <= 0)
                    // Unexpected EOF, file was truncated.
                    break;
                for (int i = numread - 1; i >= 0; i--) {
                    byte c = buf[i];
                    if (c == '\n') {
                        // Found a new line.
                        if (lastLines.size() == OTHER_LOG_LINES)
                            // We're done!
                            break;
                        if (curline != null)
                            lastLines.add(0, curline.reverse().toString());
                        curline = new StringBuilder();
                    }
                    if (curline != null)
                        curline.append((char)c);
                }
                pos -= OTHER_BUF_SIZE;
            }
            olfile.close();
        } catch (Exception e) {
            logger.warn("Error occured while reading other.log.", e);
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (String line : lastLines)
            sb.append(line);
        MimeBodyPart part = new MimeBodyPart();
        part.setText(sb.toString());
        return part;
    }
}

