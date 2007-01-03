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

package com.untangle.gui.install;

import java.io.*;
import java.util.*;

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
	for (String disk : getAvailableDisks()) {
	    System.out.println(disk + " : " + getDiskGigs(disk) + "GB");
	}
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

    public static float getDiskGigs(String disk)
    {
        try {
            String[] args = {"/bin/sh","-c"," fdisk -l /dev/" + disk + " | awk '/Disk/ { print $3}'"};
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

    public static List<String> getAvailableDisks()
    {
        LinkedList<String> avail = new LinkedList();
        
        try {
            String[] args = {"/bin/sh","-c"," fdisk -l | awk '/Disk/ { gsub(/(\\/dev\\/|:)/, \"\", $2) ; print $2}'"};
            Process proc = Runtime.getRuntime().exec(args);
            BufferedReader input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line = null;
            while ((line = input.readLine()) != null) {
                avail.add(line);
            }
        }
        catch (IOException e) {}
        
        return avail;
    }

    public static int getNumNICs()
    {
        try {
            String[] args = {"/bin/sh","-c","/sbin/ifconfig -a | egrep '^eth' | wc -l "};
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
