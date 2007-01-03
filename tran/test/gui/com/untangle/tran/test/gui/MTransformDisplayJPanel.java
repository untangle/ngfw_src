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


package com.untangle.tran.test.gui;

import com.untangle.gui.transform.*;
import com.untangle.gui.pipeline.MPipelineJPanel;
import com.untangle.mvvm.tran.TransformContext;


public class MTransformDisplayJPanel extends com.untangle.gui.transform.MTransformDisplayJPanel{
    

    
    public MTransformDisplayJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
        
        super.activity0JLabel.setText("ACT 1");
        super.activity1JLabel.setText("ACT 2");
        super.activity2JLabel.setText("ACT 3");
        super.activity3JLabel.setText("ACT 4");
    }
    
}
