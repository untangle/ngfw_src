/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.protofilter.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.tran.protofilter.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import javax.swing.event.*;

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{
    
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);

        this.mTabbedPane.insertTab("Protocol Block List", null, new ProtoConfigJPanel(mTransformJPanel.getTransformContext()), null, 0);
        //this.eventTabbedPane.insertTab("Protocol Block List", null, new ProtoEventJPanel(mTransformJPanel.getTransformContext()), null, 0);
    }
    
}


