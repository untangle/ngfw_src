/*
 * TransformGUI.java
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.airgap.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;

public class MTransformDisplayJPanel extends com.metavize.gui.transform.MTransformDisplayJPanel{

    public MTransformDisplayJPanel(MTransformJPanel mTransformJPanel){
        super(mTransformJPanel);
        
        super.activity0JLabel.setText("TCP OK");
        super.activity1JLabel.setText("UDP OK");
        
    }
    
    final protected boolean getUpdateActivity(){ return false; }
    final protected boolean getUpdateSessions(){ return false; }
    
}
