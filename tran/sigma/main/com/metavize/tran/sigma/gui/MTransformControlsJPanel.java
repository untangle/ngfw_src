/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.sigma.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.tran.sigma.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import javax.swing.event.*;

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{
    
    private static final String NAME_BLOCK_LIST = "Protocol Block List";
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
    }

    protected void generateGui(){
	// BLOCK LIST /////
	ProtoConfigJPanel protoConfigJPanel = new ProtoConfigJPanel();
        this.mTabbedPane.insertTab(NAME_BLOCK_LIST, null, protoConfigJPanel, null, 0);
	super.savableMap.put(NAME_BLOCK_LIST, protoConfigJPanel);
	super.refreshableMap.put(NAME_BLOCK_LIST, protoConfigJPanel);
    }
    
}


