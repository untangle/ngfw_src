/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.spyware.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;

public class MTransformDisplayJPanel extends com.metavize.gui.transform.MTransformDisplayJPanel{
    
    
    public MTransformDisplayJPanel(MTransformJPanel mTransformJPanel){
        super(mTransformJPanel);
        
        super.activity0JLabel.setText("SCAN");
        super.activity1JLabel.setText("DETECT");
        super.activity2JLabel.setText("BLOCK");

    }
    
}
