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

import com.untangle.mvvm.client.MvvmRemoteContext;

public class KeepAliveThread extends Thread implements Shutdownable {

    private static final long PING_DELAY = 60000l; // 1 minute
    private MvvmRemoteContext mvvmRemoteContext;
    private volatile boolean stop = false;

    public KeepAliveThread(MvvmRemoteContext mvvmRemoteContext){
        setName("MV-CLIENT: KeepAliveThread");
        this.mvvmRemoteContext = mvvmRemoteContext;
        start();
    }
    public void run(){
        while(!stop){
            try{
                mvvmRemoteContext.version();
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
