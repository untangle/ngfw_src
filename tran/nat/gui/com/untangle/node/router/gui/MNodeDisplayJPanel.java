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

package com.untangle.node.router.gui;

import com.untangle.gui.node.*;
import com.untangle.gui.pipeline.MPipelineJPanel;
import com.untangle.uvm.node.NodeContext;

public class MNodeDisplayJPanel extends com.untangle.gui.node.MNodeDisplayJPanel{
    
    
    public MNodeDisplayJPanel(MNodeJPanel mNodeJPanel) {
        super(mNodeJPanel);
        
        super.activity0JLabel.setText("BLOCK");
        super.activity1JLabel.setText("NAT");
        super.activity2JLabel.setText("REDIRECT");
        super.activity3JLabel.setText("DMZ");
    }

    final protected boolean getUpdateThroughput(){ return false; }
}
