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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.networking.NetworkManagerImpl;
import com.untangle.uvm.networking.internal.NetworkSpacesInternalSettings;
import com.untangle.uvm.node.LocalNodeManager;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.security.Tid;

public class SystemStatus
{
    public static final String  JITTER_THREAD_PROPERTY = "uvm.jitter.thread";
    public static final boolean JITTER_THREAD_DEFAULT = true;

    public static final String  JITTER_THREAD_FREQ_PROPERTY = "uvm.jitter.thread.freq";
    public static final long    JITTER_THREAD_FREQ_DEFAULT = 500;

    private static final int DEFAULT_HALF_KEY_LENGTH = 9;

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

    private static final String SPACER = "========================================================\r\n";
    private static final String RETCHAR = "\r\n";

    private volatile String staticConf = "";
    private volatile boolean staticConfFinal = false;

    public SystemStatus()
    {
        if (JITTER_THREAD) {
            jitter = new JitterThread(JITTER_THREAD_FREQ);
            Thread newThread = new Thread(jitter, "Jitter Thread");
            newThread.start();
        } else {
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
        System.out.print(stat.getStaticConf());
        String dynamicStat = _buildDynamicStat();
        System.out.print(dynamicStat);

        return;
    }

    public String systemStatus()
    {
        String dyn  = _buildDynamicStat();
        String uvm = _buildUVMStat();
        StringBuilder sb = new StringBuilder();

        sb.append(getStaticConf());
        sb.append(dyn);
        sb.append(uvm);

        return sb.toString();
    }

    public String getStaticConf()
    {
        if (!staticConfFinal) {
            synchronized (this) {
                if (!staticConfFinal) {
                    buildStaticConf();
                }
            }
        }

        return staticConf;
    }


    private void buildStaticConf ()
    {
        StringBuilder sb = new StringBuilder();
        String line;
        Process proc;
        BufferedReader input;
        int i = 0;
        boolean hasActivationKey = false;
        try {
            hasActivationKey = appendActivationKey(sb);
            String version = LocalUvmContextFactory.context().getFullVersion();
            sb.append("full version: " + version + "\n");
            /**
             * Uname info
             */
            sb.append(SPACER);
            proc = LocalUvmContextFactory.context().exec("/bin/uname -a");
            input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ((line = input.readLine()) != null) {
                sb.append(line + RETCHAR);
            }
            proc.destroy();

            /**
             * lspci
             */
            sb.append(SPACER);
            proc = LocalUvmContextFactory.context().exec("/usr/bin/lspci");
            input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ((line = input.readLine()) != null) {
                sb.append(line + RETCHAR);
            }
            proc.destroy();

            /**
             * /proc/cpuinfo
             */
            sb.append(SPACER);
            input = new BufferedReader(new FileReader("/proc/cpuinfo"));
            for ( i=0 ; i<8 && ((line = input.readLine()) != null) ; i++ ) {
                sb.append(line + RETCHAR);
            }
            input.close();

        }
        catch (Exception e) {
            logger.error("Exception: ", e);
            hasActivationKey = false;
        }

        staticConf = sb.toString();
        staticConfFinal = hasActivationKey;
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
            proc = LocalUvmContextFactory.context().exec("/usr/bin/uptime");
            input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ((line = input.readLine()) != null) {
                sb.append(line + RETCHAR);
            }
        }
        catch (Exception e) {
            logger.error("Exception: ", e);
            sb.append("Exception on exec (/usr/bin/uptime): " + e.toString() + RETCHAR);
        }
        finally {
            if (proc != null)
                proc.destroy();
        }

        proc = null;
        try {
            /**
             * uvm uptime
             */
            sb.append(SPACER);
            proc = LocalUvmContextFactory.context().exec("/usr/share/untangle/bin/untangle-vm-uptime");
            input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ((line = input.readLine()) != null) {
                sb.append("UVM uptime: " + line + RETCHAR);
            }
        }
        catch (Exception e) {
            logger.error("Exception: ", e);
            sb.append("Exception on exec (/usr/share/untangle/bin/utuptime): " + e.toString() + RETCHAR);
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
                sb.append("LOAD: " + line + RETCHAR);
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
                sb.append("JITTER: " + jitter.toString() + RETCHAR);
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
            proc = LocalUvmContextFactory.context().exec("/usr/bin/free -m");
            input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ((line = input.readLine()) != null) {
                sb.append(line + RETCHAR);
            }
        }
        catch (Exception e) {
            logger.error("Exception: ", e);
            sb.append("Exception on exec (/usr/bin/free -m): " + e.toString() + RETCHAR);
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
                sb.append("MEM: " + line + RETCHAR);
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
            proc = LocalUvmContextFactory.context().exec("/bin/df -h");
            input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ((line = input.readLine()) != null) {
                sb.append(line + RETCHAR);
            }
        }
        catch (Exception e) {
            logger.error("Exception: ", e);
            sb.append("Exception on exec (/bin/df): " + e.toString() + RETCHAR);
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
            proc = LocalUvmContextFactory.context().exec("/bin/ps --sort -rss aux");
            input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ((line = input.readLine()) != null) {
                sb.append(line + RETCHAR);
            }
        }
        catch (Exception e) {
            logger.error("Exception: ", e);
            sb.append("Exception on exec (/bin/ps): " + e.toString() + RETCHAR);
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
            proc = LocalUvmContextFactory.context().exec("/sbin/mii-tool");
            input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ((line = input.readLine()) != null) {
                sb.append(line + RETCHAR);
            }
        }
        catch (Exception e) {
            logger.error("Exception: ", e);
            sb.append("Exception on exec (/sbin/mii-tool): " + e.toString() + RETCHAR);
        }
        finally {
            if (proc != null)
                proc.destroy();
        }

        return sb.toString();
    }

    /**
     * Append the first half of the activation key
     */
    private boolean appendActivationKey(StringBuilder sb)
    {
        boolean result;

        sb.append(SPACER);
        String key = LocalUvmContextFactory.context().getActivationKey();
        if ((key==null) || (key.length()==0)) {
            key = "unset";
            result = false;
        } else {
            /* get at most half of the key */
            key = key.substring(0,Math.min(key.length()/2,DEFAULT_HALF_KEY_LENGTH));
            result = true;
        }

        sb.append("activation key: " + key + "\n");

        return result;
    }

    private String _buildUVMStat ()
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
            NetworkSpacesInternalSettings netConf = ((NetworkManagerImpl) LocalUvmContextFactory.context().localNetworkManager()).getNetworkInternalSettings();
            sb.append(netConf.toString());
            sb.append(RETCHAR);

            /**
             * Node Config
             */
            sb.append(SPACER);
            LocalNodeManager tm = LocalUvmContextFactory.context().localNodeManager();
            for (Tid t : tm.nodeInstances()) {
                NodeContext tctx = tm.nodeContext(t);
                if (tctx == null) {
                    sb.append(t + "\tNULL Node Context\n");
                    continue;
                }
                Node node = tctx.node();
                if (node == null) {
                    sb.append(t + "\tNULL Node Context\n");
                    continue;
                }
                String name = pad(tctx.getNodeDesc().getName(), 25);
                sb.append(t.getName() + "\t" + name + "\t" + t.getPolicy()
                          + "\t" + node.getRunState() + RETCHAR);
            }

            /**
             * Session Count
             */
            sb.append(SPACER);
            sb.append("Estimated Sesssion Count: ");
            sb.append(LocalUvmContextFactory.context().argonManager().getSessionCount());
            sb.append(RETCHAR);
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
