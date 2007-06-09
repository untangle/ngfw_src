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

package com.untangle.gui.node;

import com.untangle.uvm.*;
import com.untangle.uvm.toolbox.MackageDesc;

public class ButtonKey implements Comparable<ButtonKey> {

    private String applianceName;
    private int viewPosition;
    private int rackType;

    public ButtonKey(MNodeJButton mNodeJButton){
        init(mNodeJButton.getName(), mNodeJButton.getViewPosition(), mNodeJButton.getMackageDesc().getRackType());
    }

    public ButtonKey(MNodeJPanel mNodeJPanel){
        this(mNodeJPanel.getMackageDesc());
    }

    public ButtonKey(MackageDesc mackageDesc){
        init(mackageDesc.getName(), mackageDesc.getViewPosition(), mackageDesc.getRackType());
    }

    public ButtonKey(String applianceName, int viewPosition, int rackType){
        init(applianceName, viewPosition, rackType);
    }

    private void init(String applianceName, int viewPosition, int rackType){
        this.applianceName = applianceName;
        this.viewPosition = viewPosition;
        this.rackType = rackType;
    }

    public int getViewPosition(){ return viewPosition; }
    public String getApplianceName(){ return applianceName; }
    public int getRackType(){ return rackType; }

    public int compareTo(ButtonKey b){
        if( getRackType() < b.getRackType() )
            return -1;
        else if( getRackType() > b.getRackType() )
            return 1;
        else{
            if( getViewPosition() < b.getViewPosition() )
                return -1;
            else if ( getViewPosition() > b.getViewPosition() )
                return 1;
            else
                return getApplianceName().compareToIgnoreCase( b.getApplianceName() );
        }

    }


}
