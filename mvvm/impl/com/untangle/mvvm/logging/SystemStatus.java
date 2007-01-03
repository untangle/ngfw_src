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

import java.io.*;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.networking.NetworkManagerImpl;
import com.untangle.mvvm.networking.internal.NetworkSpacesInternalSettings;
import com.untangle.mvvm.security.Tid;
import com.untangle.mvvm.tran.LocalTransformManager;
import com.untangle.mvvm.tran.Transform;
import com.untangle.mvvm.tran.TransformContext;
import org.apache.log4j.Logger;

public class SystemStatus
{
    public static final String  JITTER_THREAD_PROPERTY = "mvvm.jitter.thread";
    public static final boolean JITTER_THREAD_DEFAULT = true;

    public static final String  JITTER_THREAD_FREQ_PROPERTY = "mvvm.jitter.thread.freq";
    public static final long    JITTER_THREAD_FREQ_DEFAULT = 500;

    public static boolean JITTER_THREAD;
    public static long    JITTER_THREAD_FREQ;

    static {
        JITTER_THREAD = JITTER_THREAD_DEFAULT;
        String p = System.getProperty(JITTER_THREAD_PROPERTY);
        if (p != null) {
            JITTER_THREAD = Boolean.parseBoolean(p);
        }
        JITTER_THREAD_FREQ = JITTER_THREAD_FREQ_DEFAULT;
        p = System.getProperty(JITTER_THREAD_FREQ_PROPERTY);
        if (p != null) {
            try {
                JITTER_THREAD_FREQ = Long.parseLong(p);
            } catch (NumberFormatException x) {
                System.err.println("cannot parse jitter thread freq: " + p);
                JITTER_THREAD_FREQ = JITTER_THREAD_FREQ_DEFAULT;
            }
        }
    }

    private final Logger logger = Logger.getLogger(getClass());

    private JitterThread jitter = null;

    private static final String SPACER = "========================================================\n";

    public String staticConf = null;

    public SystemStatus()
    {
        staticConf = _buildStaticConf();

        if (JITTER_THREAD) {
            jitter = new JitterThread(JITTER_THREAD_FREQ);
            Thread newThread = MvvmContextFactory.context().newThread(jitter, "Jitter Thread");
            newThread.start();
        }
        else {
            jitter = null;
        }
    }

    public void destroy()
    {
        if (null != jitter) {
            jitter.destroy();
            jitter = null;
        }
    }

    public void test()
    {
        SystemStatus stat = new SystemStatus();
        System.out.print(stat.staticConf);
        String dynamicStat = _buildDynamicStat();
        System.out.print(dynamicStat);

        return;
    }

    public String systemStatus()
    {
        String dyn  = _buildDynamicStat();
        String mvvm = _buildMVVMStat();
        StringBuilder sb = new StringBuilder();

        sb.append(staticConf);
        sb.append(dyn);
        sb.append(mvvm);

        return sb.toString();
    }


    private String _buildStaticConf ()
    {
        StringBuilder sb = new StringBuilder();
        String line;
        Process proc;
        BufferedReader input;
        int i = 0;
        try {
            /**
             * Uname info
             */
            sb.append(SPACER);
            proc = MvvmContextFactory.context().exec("/bin/uname -a");
            input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ((line = input.readLine()) != null) {
                sb.append(line+"\n");
            }
            proc.destroy();

            /**
             * lspci
             */
            sb.append(SPACER);
            proc = MvvmContextFactory.context().exec("/usr/bin/lspci");
            input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ((line = input.readLine()) != null) {
                sb.append(line+"\n");
            }
            proc.destroy();

            /**
             * /proc/cpuinfo
             */
            sb.append(SPACER);
            input = new BufferedReader(new FileReader("/proc/cpuinfo"));
            for ( i=0 ; i<8 && ((line = input.readLine()) != null) ; i++ ) {
                sb.append(line+"\n");
            }
            input.close();

        }
        catch (Exception e) {
            logger.error("Exception: ", e);
            return null;
        }

        return sb.toString();
    }

    private String _buildDynamicStat ()
    {
        StringBuilder sb = new StringBuilder();
        String line;
        Process proc;
        BufferedReader input;
        int i = 0;

        proc = null;
        try {
            /**
             * uptime
             */
            sb.append(SPACER);
            proc = MvvmContextFactory.context().exec("/usr/bin/uptime");
            input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ((line = input.readLine()) != null) {
                sb.append(line+"\n");
            }
        }
        catch (Exception e) {
            logger.error("Exception: ", e);
            sb.append("Exception on exec (/usr/bin/uptime): " + e.toString() + "\n");
        }
        finally {
            if (proc != null)
                proc.destroy();
        }

        proc = null;
        try {
            /**
             * mvvm uptime
             */
            sb.append(SPACER);
            proc = MvvmContextFactory.context().exec("/usr/bin/mvuptime");
            input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ((line = input.readLine()) != null) {
                sb.append("MVVM uptime: "+line+"\n");
            }
        }
        catch (Exception e) {
            logger.error("Exception: ", e);
            sb.append("Exception on exec (/usr/bin/mvuptime): " + e.toString() + "\n");
        }
        finally {
            if (proc != null)
                proc.destroy();
        }

        
        try {
            /**
             * /proc/loadavg
             */
            sb.append(SPACER);
            input = new BufferedReader(new FileReader("/proc/loadavg"));
            while ((line = input.readLine()) != null) {
                sb.append("LOAD: "+line+"\n");
            }
            input.close();
        }
        catch (Exception e) {
            logger.error("Exception: ", e);
        }

        try {
            /**
             * Jitter
             */
            sb.append(SPACER);
            if (this.jitter != null) {
                sb.append("JITTER: " + jitter.toString() +"\n");
                jitter.resetMaxDelay();
            }
            else {
                sb.append("jitter: disabled\n");
            }
        }
        catch (Exception e) {
            logger.error("Exception: ", e);
        }

        proc = null;
        try {
            /**
             * free -m
             */
            sb.append(SPACER);
            proc = MvvmContextFactory.context().exec("/usr/bin/free -m");
            input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ((line = input.readLine()) != null) {
                sb.append(line+"\n");
            }
        }
        catch (Exception e) {
            logger.error("Exception: ", e);
            sb.append("Exception on exec (/usr/bin/free -m): " + e.toString() + "\n");
        }
        finally {
            if (proc != null)
                proc.destroy();
        }

        try {
            /**
             * /proc/meminfo
             */
            sb.append(SPACER);
            input = new BufferedReader(new FileReader("/proc/meminfo"));
            while ((line = input.readLine()) != null) {
                sb.append("MEM: "+line+"\n");
            }
            input.close();
        }
        catch (Exception e) {
            logger.error("Exception: ", e);
        }

        proc = null;
        try {
            /**
             * df
             */
            sb.append(SPACER);
            proc = MvvmContextFactory.context().exec("/bin/df -h");
            input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ((line = input.readLine()) != null) {
                sb.append(line+"\n");
            }
        }
        catch (Exception e) {
            logger.error("Exception: ", e);
            sb.append("Exception on exec (/bin/df): " + e.toString() + "\n");
        }
        finally {
            if (proc != null)
                proc.destroy();
        }

        proc = null;
        try {
            /**
             * ps aux
             */
            sb.append(SPACER);
            proc = MvvmContextFactory.context().exec("/bin/ps --sort -rss aux");
            input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ((line = input.readLine()) != null) {
                sb.append(line+"\n");
            }
        }
        catch (Exception e) {
            logger.error("Exception: ", e);
            sb.append("Exception on exec (/bin/ps): " + e.toString() + "\n");
        }
        finally {
            if (proc != null)
                proc.destroy();
        }

        proc = null;
        try {
            /**
             * mii-tool
             */
            sb.append(SPACER);
            proc = MvvmContextFactory.context().exec("/sbin/mii-tool");
            input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ((line = input.readLine()) != null) {
                sb.append(line+"\n");
            }
        }
        catch (Exception e) {
            logger.error("Exception: ", e);
            sb.append("Exception on exec (/sbin/mii-tool): " + e.toString() + "\n");
        }
        finally {
            if (proc != null)
                proc.destroy();
        }

        return sb.toString();
    }

    private String _buildMVVMStat ()
    {
        StringBuilder sb = new StringBuilder();
        String line;
        Process proc;
        BufferedReader input;
        int i = 0;
        try {

            /**
             * Network Config
             */
            sb.append(SPACER);
            NetworkSpacesInternalSettings netConf = ((NetworkManagerImpl) MvvmContextFactory.context().networkManager()).getNetworkInternalSettings();
            sb.append(netConf.toString());
            sb.append("\n");

            /**
             * Transform Config
             */
            sb.append(SPACER);
            LocalTransformManager tm = MvvmContextFactory.context().transformManager();
            for (Tid t : tm.transformInstances()) {
                TransformContext tctx = tm.transformContext(t);
                if (tctx == null) {
                    sb.append(t + "\tNULL Transform Context\n");
                    continue;
                }
                Transform tran = tctx.transform();
                if (tran == null) {
                    sb.append(t + "\tNULL Transform Context\n");
                    continue;
                }
                String name = pad(tctx.getTransformDesc().getName(), 25);
                sb.append(t.getName() + "\t" + name + "\t" + t.getPolicy()
                          + "\t" + tran.getRunState() + "\n");
            }

            /**
             * Session Count
             */
            sb.append(SPACER);
            sb.append("Estimated Sesssion Count: ");
            sb.append(MvvmContextFactory.context().argonManager().getSessionCount());
            sb.append("\n");
            /* Insert anything else here */
        }
        catch (Exception e) {
            logger.error("Exception: ", e);
            return null;
        }

        return sb.toString();
    }

    private static final String pad(String str, int padsize)
    {
        StringBuilder sb = new StringBuilder(str.trim());
        if (str.length() >= padsize) {
            return sb.append(' ').toString();
        }
        for (int i = str.length(); i < padsize; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }


    private class JitterThread implements Runnable
    {
        private long millifreq;

        private long maxDelay;
        private long lastDelay;

        private double ext_1min;
        private double ext_5min;
        private double ext_15min;

        private double load_1min;
        private double load_5min;
        private double load_15min;

        private volatile Thread thread;

        JitterThread( long millifreq )
        {
            this.millifreq = millifreq;
            this.maxDelay = 0;

            this.ext_1min  = java.lang.Math.exp(-millifreq/ (60.0*1000.0));
            this.ext_5min  = java.lang.Math.exp(-millifreq/ (5.0*60.0*1000.0));
            this.ext_15min = java.lang.Math.exp(-millifreq/ (15.0*60.0*1000.0));

            this.load_1min  = 0.0;
            this.load_5min  = 0.0;
            this.load_15min = 0.0;
        }

        public void destroy()
        {
            Thread t = this.thread;
            if (null != t) {
                this.thread = null;
                t.interrupt();
            }
        }

        public void run()
        {
            this.thread = Thread.currentThread();

            while (this.thread == Thread.currentThread()) {

                /**
                 * Measure jitter (wake up delay)
                 */
                long startTime = System.currentTimeMillis();
                try {
                    Thread.sleep(this.millifreq);
                }
                catch (InterruptedException e) {
                    continue;
                }
                long endTime   = System.currentTimeMillis();
                long wakeupDelay = (endTime - startTime) - this.millifreq;

                /**
                 * Adjust loads and bookkeeping
                 */
                load_1min  = (load_1min  * ext_1min)  + wakeupDelay * ( 1 - ext_1min);
                load_5min  = (load_5min  * ext_5min)  + wakeupDelay * ( 1 - ext_5min);
                load_15min = (load_15min * ext_15min) + wakeupDelay * ( 1 - ext_15min);

                lastDelay = wakeupDelay;

                if (wakeupDelay > maxDelay) {
                    maxDelay = wakeupDelay;
                }
            }
        }

        public void resetMaxDelay()
        {
            this.maxDelay = 0;
        }

        public void resetLoads()
        {
            this.load_1min = 0.0;
            this.load_5min = 0.0;
            this.load_15min = 0.0;
        }

        public String toString()
        {
            StringBuilder sb = new StringBuilder();

            sb.append("1min: ");
            sb.append(String.format("%03.2f",load_1min));
            sb.append("  5min: ");
            sb.append(String.format("%03.2f",load_5min));
            sb.append("  15min: ");
            sb.append(String.format("%03.2f",load_15min));
            sb.append("    max: ");
            sb.append(String.format("%7d",maxDelay));
            sb.append("  last: ");
            sb.append(String.format("%7d",lastDelay));

            return sb.toString();
        }

    }

}
