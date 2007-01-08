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
import java.util.Map;

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

    private static final MvvmLoggingContext BOOTSTRAP_CONTEXT = new MvvmLoggingContext()
        {
            public String getConfigName() { return "log4j.xml"; }
            public String getFileName() { return "server.log"; }
        };

    private static final MvvmLoggingContext MVVM_CONTEXT = new MvvmLoggingContext()
        {
            public String getConfigName() { return "log4j-mvvm.xml"; }
            public String getFileName() { return "mvvm.log"; }
        };

    private final Map<MvvmLoggingContext, MvvmHierarchy> repositories
        = new HashMap<MvvmLoggingContext, MvvmHierarchy>();

    private final ThreadLocal<MvvmLoggingContext> currentContext;

    // constructors ----------------------------------------------------------

    public MvvmRepositorySelector()
    {
        currentContext = new InheritableThreadLocal<MvvmLoggingContext>();
        currentContext.set(BOOTSTRAP_CONTEXT);
    }

    // public methods --------------------------------------------------------

    public LoggerRepository getLoggerRepository()
    {
        MvvmLoggingContext ctx = currentContext.get();
        if (null == ctx) {
            LogLog.warn("null logging context, using bootstrap context");
            ctx = BOOTSTRAP_CONTEXT;
            currentContext.set(BOOTSTRAP_CONTEXT);
        }

        MvvmHierarchy hier;

        synchronized (repositories) {
            hier = repositories.get(ctx);
            if (null == hier) {
                MvvmLoggingContext oldCtx = currentContext.get();
                try {
                    currentContext.set(BOOTSTRAP_CONTEXT);
                    hier = new MvvmHierarchy(ctx);
                    hier.configure();
                    repositories.put(ctx, hier);
                } finally {
                    currentContext.set(oldCtx);
                }
            }
        }

        return hier;
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

    public void bootstrapContext()
    {
        currentContext.set(BOOTSTRAP_CONTEXT);
    }

    public void mvvmContext()
    {
        currentContext.set(MVVM_CONTEXT);
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
