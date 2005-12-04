/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */


package com.metavize.tran.openvpn.gui;

import com.metavize.gui.util.*;

public class KeyButtonRunnable implements ButtonRunnable {
    private boolean enabled;
    private String recipient;
    public KeyButtonRunnable(String enabled){
	if( "true".equals(enabled) ) {
	    this.enabled = true;
	}
	else if( "false".equals(enabled) ){
	    this.enabled = false;
	}
    }
    public String getButtonText(){ return "Distribute Key"; }
    public boolean isEnabled(){ return enabled; }
    public void setRecipient(String recipient){ this.recipient = recipient; }
    public String getRecipient(){ return recipient; }
    public void run(){
	
    }
}
