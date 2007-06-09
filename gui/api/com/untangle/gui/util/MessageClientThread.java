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

package com.untangle.gui.util;

import com.untangle.uvm.MessageQueue;
import com.untangle.uvm.client.*;
import com.untangle.uvm.toolbox.ToolboxMessage;
import com.untangle.uvm.toolbox.ToolboxMessageVisitor;


public class MessageClientThread extends Thread implements Shutdownable {

    private volatile ToolboxMessageVisitor toolboxMessageVisitor;
    private final UvmRemoteContext uvmContext;
    private volatile boolean stop = false;

    public MessageClientThread(UvmRemoteContext uvmContext, ToolboxMessageVisitor v){
        setDaemon(true);
        setName("MV-MessageClientThread");
        this.uvmContext = uvmContext;
        this.toolboxMessageVisitor = v;
        start();
    }

    public synchronized void doShutdown(){
        if(!stop){
            stop = true;
            interrupt();
        }
    }

    public void run(){

        MessageQueue<ToolboxMessage> toolQ = null;
        int failureCount = 0;
        while(!stop){
            if(failureCount>10)
                break;
            try{
                if( toolQ == null )
                    toolQ = uvmContext.toolboxManager().subscribe();

                for (ToolboxMessage msg : toolQ.getMessages()) {
                    msg.accept(toolboxMessageVisitor);
                }
                Thread.sleep(2000);
            }
            catch(InterruptedException e){ failureCount++; continue; }
            catch(Exception e){
                failureCount++;
                if( !isInterrupted() ){
                    try{ Util.handleExceptionWithRestart("Error handling messages", e); }
                    catch(Exception f){
                        Util.handleExceptionNoRestart("Error handling messages", f);
                        toolQ = null;
                    }
                }
            }
        }
        Util.printMessage("MessageClientThread Stopped");
    }
}
