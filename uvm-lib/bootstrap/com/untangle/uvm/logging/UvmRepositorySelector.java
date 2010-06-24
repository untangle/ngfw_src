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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.RepositorySelector;
import org.apache.log4j.spi.RootLogger;
import org.apache.log4j.xml.DOMConfigurator;
import org.apache.tools.ant.filters.ReplaceTokens;

/**
 * Selects logging repository based on the UvmLoggingContext.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class UvmRepositorySelector implements RepositorySelector
{
    public static final String NODE_LOG_FILE_NAME_TOKEN;

    private static final UvmLoggingContext UVM_CONTEXT;
    private static final UvmLoggingContextFactory UVM_CONTEXT_FACTORY;
    private static final LogMailer NULL_LOG_MAILER;
    private static final UvmRepositorySelector SELECTOR;

    private final Map<UvmLoggingContext, UvmHierarchy> repositories;
    private final Set<SmtpAppender> smtpAppenders;
    private final ThreadLocal<UvmLoggingContextFactory> currentContextFactory;

    private LogMailer logMailer = NULL_LOG_MAILER;

    // constructors -----------------------------------------------------------

    private UvmRepositorySelector()
    {
        repositories = new HashMap<UvmLoggingContext, UvmHierarchy>();
        smtpAppenders = new HashSet<SmtpAppender>();
        currentContextFactory = new InheritableThreadLocal<UvmLoggingContextFactory>();
        currentContextFactory.set(UVM_CONTEXT_FACTORY);
    }

    // factories --------------------------------------------------------------

    public static UvmRepositorySelector selector()
    {
        return SELECTOR;
    }

    // RepositorySelector methods ---------------------------------------------

    public LoggerRepository getLoggerRepository()
    {
        UvmLoggingContext ctx = getContextFactory().get();

        UvmHierarchy hier;

        synchronized (repositories) {
            hier = repositories.get(ctx);
            if (null == hier) {
                hier = new UvmHierarchy(ctx);
                hier.configure();
                repositories.put(ctx, hier);
            }
        }

        return hier;
    }

    // public methods ---------------------------------------------------------

    /**
     * Sets the {@link LogMailer} that will be used to email logs.
     *
     * @param logMailer the system {@link LogMailer}
     */
    public void setLogMailer(LogMailer logMailer)
    {
        this.logMailer = logMailer;
    }

    /**
     * Gets the system {@link LogMailer}.
     *
     * @return the system {@link LogMailer}
     */
    public LogMailer getLogMailer()
    {
        return logMailer;
    }

    /**
     * Deregister {@link UvmLoggingContext} from the system.
     *
     * @param ctx {@link UvmLoggingContext} to remove.
     */
    public void remove(UvmLoggingContext ctx)
    {
        synchronized (repositories) {
            repositories.remove(ctx);
        }
    }

    /**
     * Causes all logging repositories to reconfigure themselves from
     * the configuration file specified in the {@link
     * UvmLoggingContext}.
     */
    public void reconfigureAll()
    {
        synchronized (repositories) {
            for (UvmHierarchy h : repositories.values()) {
                h.configure();
            }
        }
    }

    /**
     * Sets the current context to the UVM context.
     */
    public void uvmContext()
    {
        currentContextFactory.set(UVM_CONTEXT_FACTORY);
    }

    /**
     * Sets the current logging context factory.
     *
     * @param ctx the {@link UvmLoggingContextFactory} to use.
     */
    public void setContextFactory(UvmLoggingContextFactory ctx)
    {
        currentContextFactory.set(ctx);
    }

    /**
     * Gets the current logging context factory.
     *
     * @return the current {@link UvmLoggingContextFactory}.
     */
    public UvmLoggingContextFactory getContextFactory()
    {
        UvmLoggingContextFactory ctx = currentContextFactory.get();
        if (null == ctx) {
            ctx = UVM_CONTEXT_FACTORY;
            currentContextFactory.set(UVM_CONTEXT_FACTORY);
        }
        return ctx;
    }

    /**
     * Get the set of {@link SmtpAppender}s registered.
     *
     * @return the set of {@link SmtpAppender}s.
     */
    public Set<SmtpAppender> getSmtpAppenders()
    {
        return smtpAppenders;
    }

    // package protected methods ----------------------------------------------

    /**
     * Adds a {@link SmtpAppender} to the logging system.
     *
     * @param appender {@link SmtpAppender} to add.
     * @return the UvmLoggingContext for this appender.
     */
    UvmLoggingContext registerSmtpAppender(SmtpAppender appender)
    {
        smtpAppenders.add(appender);
        return getContextFactory().get();
    }

    /**
     * Removes a {@link SmtpAppender} from the logging system.
     *
     * @param appender {@link SmtpAppender} to remove.
     */
    void deregisterSmtpAppender(SmtpAppender appender)
    {
        smtpAppenders.remove(appender);
    }

    // private methods --------------------------------------------------------

    /**
     * A {@link org.apache.log4j.Hierarchy} that associates the
     * current {@link UvmLoggingContext} and allows configuration
     * based on the contexts configuration file.
     */
    private class UvmHierarchy extends Hierarchy
    {
        private final UvmLoggingContext ctx;

        UvmHierarchy(UvmLoggingContext ctx)
        {
            super(new RootLogger(Level.DEBUG));

            this.ctx = ctx;
        }

        void configure()
        {
            String n = ctx.getConfigName();
            InputStream is = getClass().getClassLoader().getResourceAsStream(n);
            if (null == is) {
                LogLog.warn("could not open: " + n);
                return;
            }

            DOMConfigurator configurator = new DOMConfigurator();
            if (null != ctx) {
                Reader r = new InputStreamReader(is);
                ReplaceTokens rts = new ReplaceTokens(r);
                ReplaceTokens.Token tok = new ReplaceTokens.Token();
                tok.setKey(NODE_LOG_FILE_NAME_TOKEN);
                tok.setValue(ctx.getFileName());
                rts.addConfiguredToken(tok);

                configurator.doConfigure(rts, this);
            } else {
                configurator.doConfigure(is, this);
            }
        }
    }

    // static initialization --------------------------------------------------

    static {
        NODE_LOG_FILE_NAME_TOKEN = "NodeLogFileName";

        UVM_CONTEXT = new UvmLoggingContext()
            {
                public String getConfigName() { return "log4j-uvm.xml"; }
                public String getFileName() { return "uvm"; }
                public String getName() { return "uvm"; }
            };

        UVM_CONTEXT_FACTORY = new UvmLoggingContextFactory()
            {
                public UvmLoggingContext get() { return UVM_CONTEXT; }
            };

        NULL_LOG_MAILER = new LogMailer()
            {
                public void sendBuffer(UvmLoggingContext ctx) { }

                public void sendMessage(UvmLoggingContext ctx) { }
            };

        SELECTOR = new UvmRepositorySelector();
    }
}
