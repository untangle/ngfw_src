/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: ButtonKey.java 194 2005-04-06 19:13:55Z inieves $
 */

package com.metavize.gui.transform;

import com.metavize.mvvm.*;


public class ButtonKey implements Comparable<ButtonKey> {

    private String applianceName;
    private int rackPosition;
    private boolean isService;

    public ButtonKey(MTransformJButton mTransformJButton){
        init(mTransformJButton.getName(), mTransformJButton.getRackPosition(), mTransformJButton.getMackageDesc().isService());
    }
    
    public ButtonKey(MTransformJPanel mTransformJPanel){
        this(mTransformJPanel.getMackageDesc());
    }
    
    public ButtonKey(MackageDesc mackageDesc){
        init(mackageDesc.getName(), mackageDesc.getRackPosition(), mackageDesc.isService());
    }

    public ButtonKey(String applianceName, int rackPosition, boolean isService){
        init(applianceName, rackPosition, isService);
    }

        
    private void init(String applianceName, int rackPosition, boolean isService){
        this.applianceName = applianceName;
        this.rackPosition = rackPosition;
	this.isService = isService;
    }
    
    public int getRackPosition(){ return rackPosition; }
    public String getApplianceName(){ return applianceName; }
    public boolean isService(){ return isService; }
    
    public int compareTo(ButtonKey b){
	if( this.isService() == b.isService() ){
	    if( this.getRackPosition() < b.getRackPosition() )
		return -1;
	    else if ( this.getRackPosition() > b.getRackPosition() )
		return 1;
	    else
		return this.getApplianceName().compareToIgnoreCase( b.getApplianceName() );
	}
	else if( this.isService() )
	    return 1;
	else
	    return -1;
        
    }

     
}
