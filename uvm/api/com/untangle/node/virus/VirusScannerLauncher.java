/*
 * $HeadURL$
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
package com.untangle.node.virus;

import java.io.File;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;

abstract public class VirusScannerLauncher implements Runnable
{
    protected final Logger logger = Logger.getLogger(getClass());

    protected String scanfilePath = null;

    // These next must be volatile since they are written and read by different threads.  bug948
    protected volatile Process scanProcess = null;
    protected volatile VirusScannerResult result = null;

    protected VirusScannerLauncher(File scanfile)
    {
        scanfilePath = scanfile.getAbsolutePath();
    }

    /**
     * Starts the scan and waits for timeout milliseconds for a result
     * If a result is reached, it is returned.
     * If the time expires VirusScannerResult.ERROR is returned
     */
    public VirusScannerResult doScan(long timeout)
    {
        Thread thread = UvmContextFactory.context().newThread(this);
        long startTime = System.currentTimeMillis();
        try {
            synchronized (this) {
                // Don't start the thread until we have the monitor held.
                thread.start();

                this.wait(timeout);

                // Argh! Java can return from wait() spuriously!
                if (this.result == null) {
                    long currentTime = System.currentTimeMillis();
                    while (this.result == null && (currentTime - startTime) < timeout) {
                        this.wait(timeout - (currentTime - startTime));
                        currentTime = System.currentTimeMillis();
                    }
                }
            }
        } catch (InterruptedException e) {
            logger.warn("Virus scan interrupted, killing process, assuming clean");
            this.scanProcess.destroy();
            return VirusScannerResult.ERROR;
        }

        if (this.result == null) {
            logger.warn("Timer expired, killing process, assuming clean");

            /**
             * This is debugging information for bug 948
             */
            if (this.scanProcess == null) {
                logger.warn("ScannerLauncher Thread Status: " + thread.getState());
                logger.warn("ScannerLauncher Thread isAlive: " + thread.isAlive());
                logger.error("Virus process (" + getClass() + ") failed to launch.");
            } else {
                this.scanProcess.destroy();
            }

            return VirusScannerResult.ERROR;
        } else {
            return this.result;
        }
    }

    abstract public void run();
}
