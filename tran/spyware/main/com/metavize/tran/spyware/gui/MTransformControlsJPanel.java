/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.spyware.gui;

import com.metavize.mvvm.tran.TransformContext;

import com.metavize.mvvm.tran.IPMaddr;
import com.metavize.tran.spyware.*;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
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
        
        
        this.mTabbedPane.insertTab("General Settings", null, new GeneralConfigJPanel(mTransformJPanel.getTransformContext()), null, 0);  
        this.mTabbedPane.insertTab("ActiveX Block List", null, new ActiveXConfigJPanel(mTransformJPanel.getTransformContext()), null, 0);          
        this.mTabbedPane.insertTab("Spyware Block List", null, new SpywareConfigJPanel(mTransformJPanel.getTransformContext()), null, 0);
        this.mTabbedPane.insertTab("Cookie Block List", null, new CookieConfigJPanel(mTransformJPanel.getTransformContext()), null, 0);
        
        // this.eventTabbedPane.insertTab("Cookie + Spyware + ActiveX Combined", null, new CombinedEventJPanel(mTransformJPanel.getTransformContext()), null, 0);          
    }
}


