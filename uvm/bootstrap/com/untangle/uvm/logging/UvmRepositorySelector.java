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
import java.io.BufferedReader;
import java.io.Reader;
import java.io.Writer;
import java.io.StringWriter;
import java.io.ByteArrayInputStream;
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

/**
 * Selects logging repository based on the UvmLoggingContext.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class UvmRepositorySelector implements RepositorySelector
{
    private static final UvmLoggingContext UVM_CONTEXT;
    private static final UvmLoggingContextFactory UVM_CONTEXT_FACTORY;
    private static final UvmRepositorySelector SELECTOR;

    private final Map<UvmLoggingContext, UvmHierarchy> repositories;
    private final ThreadLocal<UvmLoggingContextFactory> currentContextFactory;

    // constructors -----------------------------------------------------------

    private UvmRepositorySelector()
    {
        repositories = new HashMap<UvmLoggingContext, UvmHierarchy>();
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

        public String convertStreamToString(InputStream is) throws java.io.IOException
        {
            if (is != null) {
                Writer writer = new StringWriter();
 
                char[] buffer = new char[1024];
                try {
                    Reader reader = new BufferedReader(
                                                       new InputStreamReader(is, "UTF-8"));
                    int n;
                    while ((n = reader.read(buffer)) != -1) {
                        writer.write(buffer, 0, n);
                    }
                } finally {
                    is.close();
                }
                return writer.toString();
            } else {       
                return "";
            }
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
                try {
                    String fileStr = convertStreamToString(is);
                    fileStr = fileStr.replace("@NodeLogFileName@", ctx.getFileName());
                    InputStream newInputStream = new ByteArrayInputStream(fileStr.getBytes("UTF-8"));
                    configurator.doConfigure(newInputStream, this);
                    this.setThrowableRenderer(new UtThrowableRenderer("node-" + ctx.getFileName() + ": "));
                }
                catch (java.io.IOException e) {
                    System.err.println("Exceptiong configuring logging exception: " + e);
                }
            } else {
                configurator.doConfigure(is, this);
            }
        }
    }

    // static initialization --------------------------------------------------

    static {
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

        SELECTOR = new UvmRepositorySelector();
    }
}
