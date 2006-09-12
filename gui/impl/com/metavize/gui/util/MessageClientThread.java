/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.util;

import com.metavize.mvvm.client.*;
import com.metavize.mvvm.MessageQueue;
import com.metavize.mvvm.toolbox.ToolboxMessage;
import com.metavize.mvvm.toolbox.ToolboxMessageVisitor;


public class MessageClientThread extends Thread implements Shutdownable {

    private volatile ToolboxMessageVisitor toolboxMessageVisitor;
    private final MvvmRemoteContext mvvmContext;
    private volatile boolean stop = false;

    public MessageClientThread(MvvmRemoteContext mvvmContext, ToolboxMessageVisitor v){
	setDaemon(true);
	setName("MV-MessageClientThread");
        this.mvvmContext = mvvmContext;
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

	while(!stop){
	    try{
		if( toolQ == null )
		    toolQ = mvvmContext.toolboxManager().subscribe();
		
		for (ToolboxMessage msg : toolQ.getMessages()) {
		    msg.accept(toolboxMessageVisitor);
		}
		Thread.sleep(2000);
	    }
	    catch(InterruptedException e){ continue; }
	    catch(Exception e){
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
