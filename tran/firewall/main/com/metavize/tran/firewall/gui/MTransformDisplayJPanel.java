/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.firewall.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;

public class MTransformDisplayJPanel extends com.metavize.gui.transform.MTransformDisplayJPanel{

    private boolean updateThroughput = false;
    
    public MTransformDisplayJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
        
        super.activity0JLabel.setText("BLOCK");
        super.activity1JLabel.setText("PASS");
        
    }
    
}
