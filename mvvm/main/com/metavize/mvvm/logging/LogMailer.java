/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: HibernateAppender.java 869 2005-06-03 18:11:09Z rbscott $
 */

package com.metavize.mvvm.logging;

import java.lang.ref.WeakReference;
import java.io.*;
import java.util.*;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.MessagingException;

import com.metavize.mvvm.MailSender;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.MvvmLocalContext;
import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.tapi.TransformContextFactory;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.TransformManager;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.CyclicBuffer;
import org.apache.log4j.spi.LoggingEvent;
import org.logicalcobwebs.proxool.ConnectionPoolDefinitionIF;
import org.logicalcobwebs.proxool.ProxoolFacade;
import org.logicalcobwebs.proxool.ProxoolListenerIF;


public class LogMailer implements Runnable
{
    // Generate no more than one email per this long (5 minutes).
    public static final long MIN_MESSAGE_PERIOD = 300000;

    // This many lines from the end of console.log for each message
    public static final int CONSOLE_LOG_LINES = 50;

    private static final Logger logger = Logger
        .getLogger(LogMailer.class);

    private static final Object LOCK = new Object();
    private static LogMailer LOGMAILER;

    private final Map<Tid, SMTPAppender> loggers;

    private String from;
    private String subject;

    private long lastSendTime = 0;

    private volatile Thread thread;

    private Object sendMonitor = new Object();
    private Tid sendTriggerer = null;

    private LogMailer()
    {
        loggers = new HashMap<Tid, SMTPAppender>();
        thread = new Thread(this);
        thread.start();
    }

    static LogMailer get()
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
                    // Broken at the moment... MvvmContextFactory.context().networkingManager().get().isExceptionReportingEnabled()
                    true
                    )
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
    public void sendBuffer(Tid tid)
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
            // Here add some console.log
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
            // Finally, get some of console.log
            MimeBodyPart part = getConsoleLogPart();
            part.setFileName("console.log");
            part.setDisposition(Part.INLINE);
            if (part != null)
                parts.add(part);

            // Send it!
            MailSender ms = MvvmContextFactory.context().mailSender();
            String header = "Error Logs";
            ms.sendErrorLogs("EdgeGuard Logs", header, parts);
        } catch(Exception e) {
            logger.warn("Error occured while sending e-mail notification.", e);
        }
    }

    private static final int CONSOLE_BUF_SIZE = 1024;

    // Because we use RandomAccessFile in here, we're using 8-bit chars (not Locale I18N friendly).  XXX
    private MimeBodyPart getConsoleLogPart()
        throws MessagingException
    {
        String bunniculaLog = System.getProperty("bunnicula.log.dir");
        String consoleLog = bunniculaLog + File.separator + "console.log";
        ArrayList<String> lastLines = new ArrayList<String>(CONSOLE_LOG_LINES);
        try {
            RandomAccessFile clfile =  new RandomAccessFile(consoleLog, "r");
            
            byte[] buf = new byte[CONSOLE_BUF_SIZE];
            long consoleLogLen = clfile.length();
            long pos = consoleLogLen - CONSOLE_BUF_SIZE;
            StringBuilder curline = null;
            while (pos >= 0) {
                clfile.seek(pos);
                int numread = clfile.read(buf);
                if (numread <= 0)
                    // Unexpected EOF, file was truncated.
                    break;
                for (int i = numread - 1; i >= 0; i--) {
                    byte c = buf[i];
                    if (c == '\n') {
                        // Found a new line.
                        if (lastLines.size() == CONSOLE_LOG_LINES)
                            // We're done!
                            break;
                        if (curline != null)
                            lastLines.add(0, curline.reverse().toString());
                        curline = new StringBuilder();
                    }
                    if (curline != null)
                        curline.append((char)c);
                }
                pos -= CONSOLE_BUF_SIZE;
            }
            clfile.close();
        } catch (Exception e) {
            logger.warn("Error occured while reading console.log.", e);
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

