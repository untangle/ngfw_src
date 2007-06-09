/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.logging;

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
 * Selects logging repository based on the MvvmLoggingContext.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class MvvmRepositorySelector implements RepositorySelector
{
    public static final String TRAN_LOG_FILE_NAME_TOKEN;

    private static final MvvmLoggingContext MVVM_CONTEXT;
    private static final MvvmLoggingContextFactory MVVM_CONTEXT_FACTORY;
    private static final LogMailer NULL_LOG_MAILER;
    private static final MvvmRepositorySelector SELECTOR;

    private final Map<MvvmLoggingContext, MvvmHierarchy> repositories;
    private final Set<SmtpAppender> smtpAppenders;
    private final ThreadLocal<MvvmLoggingContextFactory> currentContextFactory;

    private LogMailer logMailer = NULL_LOG_MAILER;

    // constructors -----------------------------------------------------------

    private MvvmRepositorySelector()
    {
        repositories = new HashMap<MvvmLoggingContext, MvvmHierarchy>();
        smtpAppenders = new HashSet<SmtpAppender>();
        currentContextFactory = new InheritableThreadLocal<MvvmLoggingContextFactory>();
        currentContextFactory.set(MVVM_CONTEXT_FACTORY);
    }

    // factories --------------------------------------------------------------

    public static MvvmRepositorySelector selector()
    {
        return SELECTOR;
    }

    // RepositorySelector methods ---------------------------------------------

    public LoggerRepository getLoggerRepository()
    {
        MvvmLoggingContext ctx = getContextFactory().get();

        MvvmHierarchy hier;

        synchronized (repositories) {
            hier = repositories.get(ctx);
            if (null == hier) {
                MvvmLoggingContextFactory o = currentContextFactory.get();
                hier = new MvvmHierarchy(ctx);
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
     * Deregister {@link MvvmLoggingContext} from the system.
     *
     * @param ctx {@link MvvmLoggingContext} to remove.
     */
    public void remove(MvvmLoggingContext ctx)
    {
        synchronized (repositories) {
            repositories.remove(ctx);
        }
    }

    /**
     * Causes all logging repositories to reconfigure themselves from
     * the configuration file specified in the {@link
     * MvvmLoggingContext}.
     */
    public void reconfigureAll()
    {
        synchronized (repositories) {
            for (MvvmHierarchy h : repositories.values()) {
                h.configure();
            }
        }
    }

    /**
     * Sets the current context to the MVVM context.
     */
    public void mvvmContext()
    {
        currentContextFactory.set(MVVM_CONTEXT_FACTORY);
    }

    /**
     * Sets the current logging context factory.
     *
     * @param ctx the {@link MvvmLoggingContextFactory} to use.
     */
    public void setContextFactory(MvvmLoggingContextFactory ctx)
    {
        currentContextFactory.set(ctx);
    }

    /**
     * Gets the current logging context factory.
     *
     * @return the current {@link MvvmLoggingContextFactory}.
     */
    public MvvmLoggingContextFactory getContextFactory()
    {
        MvvmLoggingContextFactory ctx = currentContextFactory.get();
        if (null == ctx) {
            ctx = MVVM_CONTEXT_FACTORY;
            currentContextFactory.set(MVVM_CONTEXT_FACTORY);
        }
        return ctx;
    }

    // package protected methods ----------------------------------------------

    /**
     * Adds a {@link SmtpAppender} to the logging system.
     *
     * @param appender {@link SmtpAppender} to add.
     * @return the MvvmLoggingContext for this appender.
     */
    MvvmLoggingContext registerSmtpAppender(SmtpAppender appender)
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

    /**
     * Get the set of {@link SmtpAppender}s registered.
     *
     * @return the set of {@link SmtpAppender}s.
     */
    Set<SmtpAppender> getSmtpAppenders()
    {
        return smtpAppenders;
    }

    // private methods --------------------------------------------------------

    /**
     * A {@link org.apache.log4j.Hierarchy} that associates the
     * current {@link MvvmLoggingContext} and allows configuration
     * based on the contexts configuration file.
     */
    private class MvvmHierarchy extends Hierarchy
    {
        private final MvvmLoggingContext ctx;

        MvvmHierarchy(MvvmLoggingContext ctx)
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
                tok.setKey(TRAN_LOG_FILE_NAME_TOKEN);
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
        TRAN_LOG_FILE_NAME_TOKEN = "TranLogFileName";

        MVVM_CONTEXT = new MvvmLoggingContext()
            {
                public String getConfigName() { return "log4j-mvvm.xml"; }
                public String getFileName() { return "mvvm"; }
                public String getName() { return "mvvm"; }

                public MvvmLoggingContext get() { return this; }
            };

        MVVM_CONTEXT_FACTORY = new MvvmLoggingContextFactory()
            {
                public MvvmLoggingContext get() { return MVVM_CONTEXT; }
            };

        NULL_LOG_MAILER = new LogMailer()
            {
                public void sendBuffer(MvvmLoggingContext ctx) { }

                public void sendMessage(MvvmLoggingContext ctx) { }
            };

        SELECTOR = new MvvmRepositorySelector();
    }
}
