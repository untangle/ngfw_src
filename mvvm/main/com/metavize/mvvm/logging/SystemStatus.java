/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.logging;

import java.io.*;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.NetworkManager;
import com.metavize.mvvm.networking.NetworkManagerImpl;
import com.metavize.mvvm.networking.internal.NetworkSpacesInternalSettings;
import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.tran.Transform;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.TransformDesc;
import com.metavize.mvvm.tran.TransformManager;
import org.apache.log4j.Logger;
    
public class SystemStatus
{
    private static final Logger logger = Logger.getLogger(LogMailer.class);

    
    public String staticConf = null;
    
    public SystemStatus()
    {
        staticConf = _buildStaticConf();
    }

    public void test ()
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
            sb.append("========================================================\n");
            proc = MvvmContextFactory.context().exec("/bin/uname -a");
            input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ((line = input.readLine()) != null) {
                sb.append(line+"\n");
            }
            proc.destroy();
            
            /**
             * lspci
             */
            sb.append("========================================================\n");
            proc = MvvmContextFactory.context().exec("/usr/bin/lspci");
            input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ((line = input.readLine()) != null) {
                sb.append(line+"\n");
            }
            proc.destroy();
            
            /**
             * /proc/cpuinfo
             */
            sb.append("========================================================\n");
            input = new BufferedReader(new FileReader("/proc/cpuinfo"));
            for ( i=0 ; i<8 && ((line = input.readLine()) != null) ; i++ ) {
                sb.append(line+"\n");
            }

        }
        catch (Exception e) {
            logger.error("Exception: ", e);
            return null;
        }

        return sb.toString();
    }

    private static String _buildDynamicStat ()
    {
        StringBuilder sb = new StringBuilder();
        String line;
        Process proc;
        BufferedReader input;
        int i = 0;
        try {
            /**
             * /proc/loadavg
             */
            sb.append("========================================================\n");
            input = new BufferedReader(new FileReader("/proc/loadavg"));
            while ((line = input.readLine()) != null) {
                sb.append("LOAD: "+line+"\n");
            }

            /**
             * /proc/meminfo
             */
            sb.append("========================================================\n");
            input = new BufferedReader(new FileReader("/proc/meminfo"));
            while ((line = input.readLine()) != null) {
                sb.append("MEM: "+line+"\n");
            }

            /**
             * df
             */
            sb.append("========================================================\n");
            proc = MvvmContextFactory.context().exec("/bin/df -h");
            input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ((line = input.readLine()) != null) {
                sb.append(line+"\n");
            }
            proc.destroy();
            
            /**
             * ps aux
             */
            sb.append("========================================================\n");
            proc = MvvmContextFactory.context().exec("/bin/ps --sort -rss aux");
            input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            for ( i=0 ; (line = input.readLine()) != null ; i++ ) {
                if (i<25) sb.append(line+"\n");
            }
            proc.destroy();
            sb.append((i-25) + " lines truncated\n");
        }
        catch (Exception e) {
            logger.error("Exception: ", e);
            return null;
        }

        return sb.toString();
    }

    private static String _buildMVVMStat ()
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
            sb.append("========================================================\n");
            NetworkSpacesInternalSettings netConf = ((NetworkManagerImpl) MvvmContextFactory.context().networkManager()).getNetworkInternalSettings();
            sb.append(netConf.toString());
            sb.append("\n");

            /**
             * Transform Config
             */
            sb.append("========================================================\n");
            TransformManager tm = MvvmContextFactory.context().transformManager();
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
            sb.append("========================================================\n");
            sb.append("Session Count: Unimplemented\n");
            /* FIXME */

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

}
