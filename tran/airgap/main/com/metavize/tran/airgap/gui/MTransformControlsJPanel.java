/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.airgap.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{
    
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
        
        super.contentJPanel.remove(super.saveJButton);
        super.contentJPanel.remove(super.reloadJButton);
        super.contentJPanel.remove(super.expandJButton);
        super.contentJPanel.remove(super.readoutJLabel);
        super.contentJPanel.remove(super.mTabbedPane);
    }
     
    
}
