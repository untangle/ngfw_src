 /*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */



package com.metavize.tran.reporting.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;
import javax.swing.*;
import com.metavize.gui.util.*;


public class MTransformJPanel extends com.metavize.gui.transform.MTransformJPanel{
    

    
    public MTransformJPanel(TransformContext transformContext) throws Exception {
        super(transformContext);
                
    }
    
    // Reference this since we need it and we don't have a Log panel to reference it. XXX
    public void fake(com.metavize.tran.reporting.ReportingTransform transform) {
        transform.getReportingSettings();
    }
}
