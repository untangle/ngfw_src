package com.metavize.tran.ids.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;

public class MTransformDisplayJPanel extends com.metavize.gui.transform.MTransformDisplayJPanel{
    
    
    public MTransformDisplayJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
        
        super.activity0JLabel.setText("A1");
        super.activity1JLabel.setText("A2");
        super.activity2JLabel.setText("A3");
        super.activity3JLabel.setText("A4");
    }
    
}
