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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.mail.internet.MimeBodyPart;
import javax.mail.MessagingException;

import com.metavize.mvvm.MailSender;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.MvvmLocalContext;
import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.tapi.TransformContextFactory;
import com.metavize.mvvm.tran.TransformContext;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.helpers.CyclicBuffer;
import org.apache.log4j.spi.LoggingEvent;
import org.logicalcobwebs.proxool.ConnectionPoolDefinitionIF;
import org.logicalcobwebs.proxool.ProxoolFacade;
import org.logicalcobwebs.proxool.ProxoolListenerIF;


public class SMTPAppender extends AppenderSkeleton
{
    // We use one of these sized buffers for each transform, and one for main.
    public static int CIRCULAR_BUFFER_SIZE = 100;

    private int bufferSize =         CIRCULAR_BUFFER_SIZE;
    protected CyclicBuffer cb = new CyclicBuffer(bufferSize);
    private final Tid tid;

    private LogMailer parent;

    // constructors -----------------------------------------------------------

    private class OurLayout extends PatternLayout {
        // Right now we use the same thing as the main log4j logs
        static final String LAYOUT = "%d{ABSOLUTE} %-5p [%c{1}] %m%n";
        private TransformContext tctx;
        OurLayout(TransformContext tctx) {
            super(LAYOUT);
            this.tctx = tctx;
        }
        @Override public String getHeader() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n");
            String name;
            if (tctx == null)
                name = "MVVM";
            else
                name = tctx.getTransformDesc().getName();
            sb.append(name);
            sb.append("\n");
            for (int i = 0; i < name.length(); i++)
                sb.append('-');
            sb.append("\n");
            return sb.toString();
        }
        @Override public String getFooter() {
            StringBuilder sb = new StringBuilder();
            sb.append("\nEnd of ");
            String name;
            if (tctx == null)
                name = "MVVM";
            else
                name = tctx.getTransformDesc().getName();
            sb.append(name);
            sb.append("\n\n");
            return sb.toString();
        }
    }
            

    public SMTPAppender()
    {
        super();

        TransformContext tctx;
        if (MvvmContextFactory.state() == MvvmLocalContext.MvvmState.LOADED) {
            tid = new Tid(0L);
            tctx = null;
        } else {
            tctx = TransformContextFactory.context();
            if (tctx == null)
                tid = new Tid(0L);
            else
                tid = tctx.getTid();
        }
        Layout layout = new OurLayout(tctx);
        setLayout(layout);

        parent = LogMailer.get();
        parent.register(tid, this);
    }

    // Appender methods -------------------------------------------------------

    protected void append(LoggingEvent event)
    {
        try {
            // For now, ignore anything that happens before we are all the way booted.  This is
            // way safer than the alternative. XXX
            if (MvvmContextFactory.state() != MvvmLocalContext.MvvmState.RUNNING) {
                return;
            }
            event.getThreadName();
            event.getNDC();
            cb.add(event);
            if (event.getLevel().isGreaterOrEqual(Level.ERROR)) {
                parent.sendBuffer(tid);
            }
        } catch (Exception x) {
            // Just to be really really safe.  We should never throw an error up to our caller.
            // After all, this is just logging.
        }
    }

    synchronized public void close()
    {
        this.closed = true;
    }

    public boolean requiresLayout()
    {
        return true;
    }

    // Return null if there's nothing interesting or we can't make it for some reason.
    protected synchronized MimeBodyPart getPart()
        throws MessagingException
    {
        // Note: this code already owns the monitor for this
        // appender. This frees us from needing to synchronize on 'cb'.
        MimeBodyPart part = new MimeBodyPart();

        StringBuffer sbuf = new StringBuffer();
        String t = layout.getHeader();
        if(t != null)
            sbuf.append(t);
        int len =  cb.length();
        if (len == 0)
            return null;
        for(int i = 0; i < len; i++) {
            //sbuf.append(MimeUtility.encodeText(layout.format(cb.get())));
            LoggingEvent event = cb.get();
            sbuf.append(layout.format(event));
            if(layout.ignoresThrowable()) {
                String[] s = event.getThrowableStrRep();
                if (s != null) {
                    for(int j = 0; j < s.length; j++) {
                        sbuf.append(s[j]);
                    }
                }
            }
        }
        t = layout.getFooter();
        if(t != null)
            sbuf.append(t);
        part.setContent(sbuf.toString(), layout.getContentType());
        return part;
    }
}
