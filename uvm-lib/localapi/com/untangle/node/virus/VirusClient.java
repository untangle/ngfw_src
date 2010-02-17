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

import org.apache.log4j.Logger;

abstract public class VirusClient implements Runnable {
    protected final Logger logger = Logger.getLogger(getClass());

    protected final static int READ_SZ = 1024;

    protected final VirusClientContext cContext;

    protected Thread cThread;
    protected String dbgName; // thread name and socket host
    protected volatile boolean stop = false;

    public VirusClient(VirusClientContext cContext) {
        this.cContext = cContext;
    }

    public void setThread(Thread cThread) {
        this.cThread = cThread;
        dbgName = new StringBuilder("<").append(cThread.getName()).append(">").append(cContext.getHost()).append(":").append(cContext.getPort()).toString();
        return;
    }

    public void startScan() {
        //logger.debug("start, thread: " + cThread + ", this: " + this);
        cThread.start(); // execute run() now
        return;
    }

    // timeout > 0
    public void checkProgress(long timeout) {
        //logger.debug("check, thread: " + cThread + ", this: " + this);
        if (false == cThread.isAlive()) {
            logger.debug(dbgName + ", is not alive; not waiting");
            return;
        }

        try {
            synchronized (this) {
                long startTime = System.currentTimeMillis();
                this.wait(timeout); // wait for run() to finish/timeout

                // retry when no result yet and time remains before timeout
                long elapsedTime = System.currentTimeMillis() - startTime;
                while (null == cContext.getResult() && elapsedTime < timeout) {
                    this.wait(timeout - elapsedTime);
                    elapsedTime = System.currentTimeMillis() - startTime;
                }
            }
        } catch (InterruptedException e) {
            logger.warn(dbgName + ", virusc interrupted", e);
        } catch (Exception e) {
            logger.warn(dbgName + ", virusc failed", e);
        }

        if (null == cContext.getResult()) {
            logger.warn(dbgName + ", virusc timer expired");
            stopScan();
        }

        return;
    }

    public void stopScan() {
        //logger.debug("stop, thread: " + cThread + ", this: " + this);
        if (false == cThread.isAlive()) {
            logger.debug(dbgName + ", is not alive; no need to stop");
            return;
        }

        this.stop = true;
        cThread.interrupt(); // stop run() now
        return;
    }

    public String toString() {
        return dbgName;
    }

    abstract public void run();

    public void cleanExit() {
        synchronized (this) {
            this.notifyAll(); // notify waiting thread and finish run()
            return;
        }
    }
}
