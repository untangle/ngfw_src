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


package com.untangle.node.test.gui;

import com.untangle.gui.node.*;
import com.untangle.gui.pipeline.MPipelineJPanel;
import com.untangle.uvm.node.NodeContext;


public class MNodeDisplayJPanel extends com.untangle.gui.node.MNodeDisplayJPanel{
    

    
    public MNodeDisplayJPanel(MNodeJPanel mNodeJPanel) {
        super(mNodeJPanel);
        
        super.activity0JLabel.setText("ACT 1");
        super.activity1JLabel.setText("ACT 2");
        super.activity2JLabel.setText("ACT 3");
        super.activity3JLabel.setText("ACT 4");
    }
    
}
