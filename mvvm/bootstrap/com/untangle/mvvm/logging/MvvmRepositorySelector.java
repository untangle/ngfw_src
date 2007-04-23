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

public class MvvmRepositorySelector implements RepositorySelector
{
    public static final String TRAN_LOG_FILE_NAME_TOKEN
        = "TranLogFileName";

    private static final MvvmLoggingContext MVVM_CONTEXT = new MvvmLoggingContext()
        {
            public String getConfigName() { return "log4j-mvvm.xml"; }
            public String getFileName() { return "mvvm"; }
            public String getName() { return "mvvm"; }

            public MvvmLoggingContext get() { return this; }
        };

    private static final MvvmLoggingContextFactory MVVM_CONTEXT_FACTORY = new MvvmLoggingContextFactory()
        {
            public MvvmLoggingContext get() { return MVVM_CONTEXT; }
        };

    private static final LogMailer NULL_LOG_MAILER = new LogMailer()
        {
            public void sendBuffer(MvvmLoggingContext ctx) { }

            public void sendMessage(MvvmLoggingContext ctx) { }
        };

    private static final MvvmRepositorySelector SELECTOR = new MvvmRepositorySelector();

    private final Map<MvvmLoggingContext, MvvmHierarchy> repositories
        = new HashMap<MvvmLoggingContext, MvvmHierarchy>();

    private final ThreadLocal<MvvmLoggingContextFactory> currentContextFactory;
    private final Set<SMTPAppender> smtpAppenders = new HashSet<SMTPAppender>();

    private LogMailer logMailer = NULL_LOG_MAILER;

    // constructors ----------------------------------------------------------

    private MvvmRepositorySelector()
    {
        currentContextFactory = new InheritableThreadLocal<MvvmLoggingContextFactory>();
        currentContextFactory.set(MVVM_CONTEXT_FACTORY);
    }

    // factories -------------------------------------------------------------

    public static MvvmRepositorySelector selector()
    {
        return SELECTOR;
    }

    // public methods --------------------------------------------------------

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

    public void setLogMailer(LogMailer logMailer)
    {
        this.logMailer = logMailer;
    }

    public LogMailer getLogMailer()
    {
        return logMailer;
    }

    public MvvmLoggingContext registerSmtpAppender(SMTPAppender appender)
    {
        smtpAppenders.add(appender);
        return getContextFactory().get();
    }

    public Set<SMTPAppender> getSmtpAppenders()
    {
        return smtpAppenders;
    }

    public void remove(MvvmLoggingContext ctx)
    {
        synchronized (repositories) {
            repositories.remove(ctx);
        }
    }

    public void reconfigureAll()
    {
        synchronized (repositories) {
            for (MvvmHierarchy h : repositories.values()) {
                h.configure();
            }
        }
    }

    public void mvvmContext()
    {
        currentContextFactory.set(MVVM_CONTEXT_FACTORY);
    }

    public void setContextFactory(MvvmLoggingContextFactory ctx)
    {
        currentContextFactory.set(ctx);
    }

    public MvvmLoggingContextFactory getContextFactory()
    {
        MvvmLoggingContextFactory ctx = currentContextFactory.get();
        if (null == ctx) {
            ctx = MVVM_CONTEXT_FACTORY;
            currentContextFactory.set(MVVM_CONTEXT_FACTORY);
        }
        return ctx;
    }

    private class MvvmHierarchy extends Hierarchy
    {
        private final MvvmLoggingContext ctx;

        MvvmHierarchy(MvvmLoggingContext ctx) {
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
}
