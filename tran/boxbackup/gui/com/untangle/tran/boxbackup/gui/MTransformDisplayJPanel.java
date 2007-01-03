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


package com.untangle.tran.boxbackup.gui;

import com.untangle.gui.transform.*;
import com.untangle.gui.pipeline.MPipelineJPanel;
import com.untangle.mvvm.tran.TransformContext;


public class MTransformDisplayJPanel extends com.untangle.gui.transform.MTransformDisplayJPanel{
        
    public MTransformDisplayJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);       
    }
    
    protected boolean getUpdateActivity(){ return false; }
    protected boolean getUpdateSessions(){ return false; }
    protected boolean getUpdateThroughput(){ return false; }
}
