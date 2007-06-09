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

import com.untangle.uvm.client.UvmRemoteContext;

public class KeepAliveThread extends Thread implements Shutdownable {

    private static final long PING_DELAY = 60000l; // 1 minute
    private UvmRemoteContext uvmRemoteContext;
    private volatile boolean stop = false;

    public KeepAliveThread(UvmRemoteContext uvmRemoteContext){
        setName("MV-CLIENT: KeepAliveThread");
        this.uvmRemoteContext = uvmRemoteContext;
        start();
    }
    public void run(){
        while(!stop){
            try{
                uvmRemoteContext.version();
                sleep(PING_DELAY);
            }
            catch(Exception e){
                try{ sleep(PING_DELAY); }
                catch(InterruptedException exn){}
                e.printStackTrace();
            }
        }
    }
    public void doShutdown(){
        stop = true;
    }
}
