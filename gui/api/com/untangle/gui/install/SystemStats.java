/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
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
            String[] args = {"/bin/sh","-c","cat /proc/cpuinfo | grep -E '^cpu MHz' | head -n 1 | awk '{print $4}'"};
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
            String[] args = {"/bin/sh","-c"," fdisk -l /dev/" + disk + " | awk '/Disk/ { gsub(/,/, \"\") ; print $3$4 }'"};
            Process proc = Runtime.getRuntime().exec(args);
            BufferedReader input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            String diskSize = input.readLine();

            int    len      = diskSize.length();
            String unit     = diskSize.substring(len - 2);
            float  number   = -1f;

            try {
                number = (float)Float.parseFloat(diskSize.substring(0, len - 2));
            }
            catch (java.lang.NumberFormatException e) {
                return -1f;
            }

            if (unit.equalsIgnoreCase("GB"))
                return number;
            else if (unit.equalsIgnoreCase("MB"))
                return number / 1000;
            else
                return -1f;

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
