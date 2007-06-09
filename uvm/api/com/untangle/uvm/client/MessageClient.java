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

package com.untangle.uvm.client;

import com.untangle.uvm.MessageQueue;
import com.untangle.uvm.toolbox.ToolboxMessage;
import com.untangle.uvm.toolbox.ToolboxMessageVisitor;
import org.apache.log4j.Logger;

public class MessageClient
{
    private final UvmRemoteContext uvmContext;
    private final PollWorker pollWorker = new PollWorker();
    private final Logger logger = Logger.getLogger(getClass());

    private volatile ToolboxMessageVisitor toolboxMessageVisitor;

    // constructors -----------------------------------------------------------

    public MessageClient(UvmRemoteContext uvmContext)
    {
        this.uvmContext = uvmContext;
    }

    // accessors --------------------------------------------------------------

    public void setToolboxMessageVisitor(ToolboxMessageVisitor v)
    {
        this.toolboxMessageVisitor = v;
    }

    // lifecycle methods ------------------------------------------------------

    public void start()
    {
        pollWorker.start();
    }

    public void stop()
    {
        pollWorker.stop();
    }

    // inner classes ----------------------------------------------------------

    private class PollWorker implements Runnable
    {
        private volatile Thread thread;

        public void run()
        {
            Thread t = Thread.currentThread();
            MessageQueue<ToolboxMessage> toolQ = uvmContext.toolboxManager().subscribe();

            while (thread == t) {
                ToolboxMessageVisitor tmv = toolboxMessageVisitor;

                if (null != tmv) {
                    try {
                        for (ToolboxMessage msg : toolQ.getMessages()) {
                            msg.accept(tmv);
                        }
                    } catch (InvocationException exn1) {
                        System.err.println("invocation exn: " + exn1);
                        exn1.printStackTrace();
                        toolQ = uvmContext.toolboxManager().subscribe();
                        continue;
                    }
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException exn) {
                    /* reevaluate loop condition */
                }
            }
        }

        public synchronized void start()
        {
            if (null != thread) {
                logger.warn("MessageClient already running");
            } else {
                thread = new Thread(this, "MV-MessageClient");
                thread.setDaemon(true);
                thread.start();
            }
        }

        public synchronized void stop()
        {
            if (null != thread) {
                logger.warn(("MessageClient not running"));
            } else {
                Thread t = thread;
                thread = null;
                t.interrupt();
            }
        }
    }
}
