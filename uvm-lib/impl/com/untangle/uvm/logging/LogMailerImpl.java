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

package com.untangle.uvm.logging;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;

import org.apache.log4j.Logger;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.MailSender;
import com.untangle.uvm.UvmState;
import com.untangle.uvm.Version;

public class LogMailerImpl implements LogMailer, Runnable
{
    public static final String SUBJECT_BASE = "Untangle Platform Logs";
    public static final String BODY_BASE = "Error Logs";

    // Generate no more than one email per this long (5 minutes).
    public static final long MIN_MESSAGE_PERIOD = 300000;

    // This many lines from the end of non-log4j logs (console, gc)
    // for each message
    public static final int OTHER_LOG_LINES = 50;

    private static final int OTHER_BUF_SIZE = 4096;

    private final Logger logger = Logger.getLogger(getClass());

    private SystemStatus sysstat;

    private long lastSendTime = 0;

    private volatile Thread thread;

    private Object sendMonitor = new Object();
    private UvmLoggingContext sendTriggerer = null;

    // constructors -----------------------------------------------------------

    public LogMailerImpl()
    {
        this.sysstat = new SystemStatus();
        thread = new Thread(this);
        thread.start();
    }

    // public methods ---------------------------------------------------------

    public void stop()
    {
        this.sysstat.destroy();
        Thread t = thread;
        thread = null;
        t.interrupt();
    }

    // LogMailer methods ------------------------------------------------------

    // Called from one of the SmtpAppenders to indicate the need to send.
    public void sendBuffer(UvmLoggingContext ctx)
    {
        synchronized(sendMonitor) {
            // Might just update the value, but that's ok -- we'll
            // still end up putting an interesting error log first.
            sendTriggerer = ctx;
            sendMonitor.notify();
        }
    }

    // Runnable methods -------------------------------------------------------

    public void run()
    {
        while (null != thread) {
            try {
                UvmLoggingContext triggerer;
                synchronized (sendMonitor) {
                    sendMonitor.wait();
                    triggerer = sendTriggerer;
                    sendTriggerer = null;
                }
                long now = System.currentTimeMillis();
                if (now - MIN_MESSAGE_PERIOD < lastSendTime)
                    Thread.sleep(MIN_MESSAGE_PERIOD  - (now - lastSendTime));
                if (LocalUvmContextFactory.state() == UvmState.RUNNING &&
                    LocalUvmContextFactory.context().networkManager().getMiscSettings().getIsExceptionReportingEnabled()) {
                    sendMessage(triggerer);
                }
                lastSendTime = System.currentTimeMillis();
            } catch (InterruptedException x) {
                // Normal
                logger.info("exiting from interrupt");
            }  catch (Exception exn) {
                logger.warn("danger, danger, will robinson", exn); // never die
            }
        }
    }

    // private methods --------------------------------------------------------

    /**
     * Send the contents of all the cyclic buffers as an e-mail
     * message.
     */
    private void sendMessage(UvmLoggingContext triggeringCtx) {
        try {
            Set<SmtpAppender> appenders = UvmRepositorySelector.selector()
                .getSmtpAppenders();

            ArrayList<MimeBodyPart> parts = new ArrayList<MimeBodyPart>();
            for (SmtpAppender appender : appenders) {
                UvmLoggingContext ctx = appender.getLoggingContext();
                String partName = ctx.getName() + ".log";
                MimeBodyPart part = appender.getPart();
                if (part != null) {
                    part.setFileName(partName);
                    part.setDisposition(Part.INLINE);
                    if (ctx.equals(triggeringCtx)) {
                        parts.add(0, part);
                    } else {
                        parts.add(part);
                    }
                }
            }

            // Finally, get some of more helpful non-log4j logs: console.log, gc.log, etc.
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

            part = getOtherLogPart("wrapper.log");
            if (part != null) {
                part.setFileName("wrapper.log");
                part.setDisposition(Part.INLINE);
                parts.add(part);
            }

            part = getOtherLogPart("apt.log");
            if (part != null) {
                part.setFileName("apt.log");
                part.setDisposition(Part.INLINE);
                parts.add(part);
            }

            // Send it!
            doSend(SUBJECT_BASE, BODY_BASE, parts);
        } catch(Exception e) {
            logger.warn("Error occured while sending e-mail notification.", e);
        }
    }

    private void doSend(String subjectBase, String bodyBase,
                        List<MimeBodyPart> parts) {
        String host = LocalUvmContextFactory.context().networkManager().getAddressSettings().getHostName().toString();

        String bodyText = sysstat.systemStatus();
        String version = Version.getVersion();

        StringBuilder sb = new StringBuilder(subjectBase);
        sb.append(" (");
        sb.append(host);
        sb.append(")");
        sb.append(" v");
        sb.append(version);
        String subjectText = sb.toString();

        MailSender ms = LocalUvmContextFactory.context().mailSender();
        ms.sendErrorLogs(subjectText, bodyText, parts);
    }

    // private methods --------------------------------------------------------

    // Because we use RandomAccessFile in here, we're using 8-bit
    // chars (not Locale I18N friendly).  XXX
    private MimeBodyPart getOtherLogPart(String logFileName)
        throws MessagingException
    {
        String uvmLog = System.getProperty("uvm.log.dir");
        String otherLog = uvmLog + File.separator + logFileName;
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
