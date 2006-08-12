/*
 * Copyright (c) 2003,2004, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.install;

import java.io.*;

public class SystemStats
{

    public static void main (String[] args)
    {
        SystemStats sys = new SystemStats();
        sys.test();
        System.exit(0);
    }
    
    public static void test()
    {
        System.out.println("Memory: " + getMemoryMegs() + "MB");
        System.out.println("Physical CPU(s): " + getPhysicalCPU());
        System.out.println("Logical  CPU(s): " + getLogicalCPU());
        System.out.println("Clock Speed: " + getClockSpeed() + " MHz" );
        System.out.println("BogoMIPS: " + getBogoMIPS());
        System.out.println("DiskSize: " + getDiskGigs() + "GB");
        System.out.println("Network Cards: " + getNumNICs());
    }

    public static int getMemoryMegs()
    {
        try {
            BufferedReader input = new BufferedReader(new FileReader("/proc/meminfo"));
            String[] tokens = input.readLine().split("[\t ]");
            for (int i=0; i<tokens.length; i++) {
                try {
                    int megs = Integer.parseInt(tokens[i])/1000;
                    return megs;
                }
                catch (java.lang.NumberFormatException e) {}
            }
        }
        catch (FileNotFoundException e) {
            return -1;
        }
        catch (IOException e) {
            return -1;
        }
        return -1;
    }

    public static int getPhysicalCPU()
    {
        try {
            String[] args = {"/bin/sh","-c","cat /proc/cpuinfo | grep physical | uniq | wc -l"};
            Process proc = Runtime.getRuntime().exec(args);
            BufferedReader input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            try {
                int num = Integer.parseInt(input.readLine());
                return num;
            }
            catch (java.lang.NumberFormatException e) {}
        }
        catch (IOException e) {}
        return -1;
    }

    public static int getLogicalCPU()
    {
        try {
            String[] args = {"/bin/sh","-c","cat /proc/cpuinfo | grep processor | uniq | wc -l"};
            Process proc = Runtime.getRuntime().exec(args);
            BufferedReader input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            try {
                int num = Integer.parseInt(input.readLine());
                return num;
            }
            catch (java.lang.NumberFormatException e) {}
        }
        catch (IOException e) {}
        return -1;
    }

    public static int getClockSpeed()
    {
        try {
            String[] args = {"/bin/sh","-c","cat /proc/cpuinfo | grep MHz | head -n 1 | awk '{print $4}'"};
            Process proc = Runtime.getRuntime().exec(args);
            BufferedReader input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            try {
                int num = (int)Float.parseFloat(input.readLine());
                return num;
            }
            catch (java.lang.NumberFormatException e) {}
        }
        catch (IOException e) {}
        return -1;
    }

    public static int getBogoMIPS()
    {
        try {
            String[] args = {"/bin/sh","-c","cat /proc/cpuinfo | grep bogomips | head -n 1 | awk '{print $3}'"};
            Process proc = Runtime.getRuntime().exec(args);
            BufferedReader input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            try {
                int num = (int)Float.parseFloat(input.readLine());
                return num;
            }
            catch (java.lang.NumberFormatException e) {}
        }
        catch (IOException e) {}
        return -1;
    }

    public static float getDiskGigs()
    {
        try {
            String[] args = {"/bin/sh","-c"," cat /proc/partitions | egrep 'sda|hda' | egrep -v 'sda[1-9]|hda[1-9]' | awk '{print $3*512/1000000000}'"};
            Process proc = Runtime.getRuntime().exec(args);
            BufferedReader input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            try {
		float num = (float)Float.parseFloat(input.readLine());
                return num;
            }
            catch (java.lang.NumberFormatException e) {}
        }
        catch (IOException e) {}
        return -1f;
    }

    public static int getNumNICs()
    {
        try {
            String[] args = {"/bin/sh","-c","/sbin/ifconfig | egrep '^eth' | wc -l "};
            Process proc = Runtime.getRuntime().exec(args);
            BufferedReader input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            try {
                int num = (int)Integer.parseInt(input.readLine());
                return num;
            }
            catch (java.lang.NumberFormatException e) {}
        }
        catch (IOException e) {}
        return -1;
    }

}
