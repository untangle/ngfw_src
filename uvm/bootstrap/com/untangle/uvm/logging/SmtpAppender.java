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

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.helpers.CyclicBuffer;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Contains circular buffer with recent log messages.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public class SmtpAppender extends AppenderSkeleton
{
    // We use one of these sized buffers for each node, and one for main.
    public static int CIRCULAR_BUFFER_SIZE = 100;

    private final UvmLoggingContext ctx;

    protected CyclicBuffer cb = new CyclicBuffer(CIRCULAR_BUFFER_SIZE);

    // constructors -----------------------------------------------------------

    public SmtpAppender()
    {
        super();

        ctx = UvmRepositorySelector.selector().registerSmtpAppender(this);
        name = ctx.getName();

        // We make the layout ourselves -- it's not in the XML.
        Layout layout = new UtMailLayout(name);
        setLayout(layout);
    }

    // public methods ---------------------------------------------------------

    // DOM XML parser calls in here with LayoutConversionPattern param.
    public void setLayoutConversionPattern(String pattern) {
        UtMailLayout layout = (UtMailLayout) getLayout();
        layout.setConversionPattern(pattern);
    }

    public UvmLoggingContext getLoggingContext()
    {
        return ctx;
    }

    // Appender methods -------------------------------------------------------

    @Override
    protected void append(LoggingEvent event)
    {
        try {
            event.getThreadName();
            event.getNDC();
            cb.add(event);
            if (event.getLevel().isGreaterOrEqual(Level.ERROR)) {
                LogMailer lm = UvmRepositorySelector.selector().getLogMailer();
                Thread t = Thread.currentThread();
                ClassLoader oldCl = t.getContextClassLoader();
                t.setContextClassLoader(lm.getClass().getClassLoader());
                try {
                    lm.sendBuffer(ctx);
                } finally {
                    t.setContextClassLoader(oldCl);
                }
            }
        } catch (Exception exn) {
            LogLog.error(exn.toString(), exn);
        }
    }

    @Override
    synchronized public void close()
    {
        this.closed = true;
        UvmRepositorySelector.selector().deregisterSmtpAppender(this);
    }

    @Override
    public boolean requiresLayout()
    {
        return true;
    }

    // public methods ---------------------------------------------------------

    /**
     * Returns the contents of the circular buffer in the form of a
     * Mime body part.
     *
     * @return a mime part with the buffer contents or null if there
     * is nothing interesting
     * @exception MessagingException if content cannot be set.
     */
    public synchronized MimeBodyPart getPart() throws MessagingException
    {
        // Note: this code already owns the monitor for this
        // appender. This frees us from needing to synchronize on
        // 'cb'.
        MimeBodyPart part = new MimeBodyPart();

        StringBuilder sb = new StringBuilder();
        String t = layout.getHeader();
        if(t != null) {
            sb.append(t);
        }

        int len =  cb.length();
        if (len == 0) {
            return null;
        }

        for(int i = 0; i < len; i++) {
            LoggingEvent event = cb.get();
            sb.append(layout.format(event));
            if(layout.ignoresThrowable()) {
                String[] s = event.getThrowableStrRep();
                if (s != null) {
                    for(int j = 0; j < s.length; j++) {
                        sb.append(s[j]);
                    }
                }
            }
        }

        t = layout.getFooter();
        if(t != null) {
            sb.append(t);
        }

        part.setContent(sb.toString(), layout.getContentType());

        return part;
    }
}
