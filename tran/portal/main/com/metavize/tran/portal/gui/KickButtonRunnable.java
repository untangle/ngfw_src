/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */


package com.metavize.tran.portal.gui;

import com.metavize.gui.transform.MTransformControlsJPanel;
import com.metavize.mvvm.portal.*;
import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.util.*;
import java.awt.Window;
import java.awt.event.*;
import javax.swing.CellEditor;
import javax.swing.SwingUtilities;

public class KickButtonRunnable implements ButtonRunnable {
    private boolean isEnabled;
    private Window topLevelWindow;    
    private MTransformControlsJPanel mTransformControlsJPanel;
    private boolean valueChanged;
    private CellEditor cellEditor;

    private PortalLogin portalLogin;

    public KickButtonRunnable(PortalLogin portalLogin, String isEnabled){
	if( "true".equals(isEnabled) ) {
	    this.isEnabled = true;
	}
	else if( "false".equals(isEnabled) ){
	    this.isEnabled = false;
	}
	this.portalLogin = portalLogin;
    }
    public String getButtonText(){ return "Logout"; }
    public boolean isEnabled(){ return isEnabled; }
    public boolean valueChanged(){ return false; }
    public void setCellEditor(CellEditor cellEditor){ this.cellEditor = cellEditor; }
    public void setEnabled(boolean isEnabled){ this.isEnabled = isEnabled; }
    public void setTopLevelWindow(Window topLevelWindow){ this.topLevelWindow = topLevelWindow; }
    public void setMTransformControlsJPanel(MTransformControlsJPanel mTransformControlsJPanel){ this.mTransformControlsJPanel = mTransformControlsJPanel; }
    public void actionPerformed(ActionEvent evt){ run(); }
    public void run(){

	isEnabled = false;
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    cellEditor.stopCellEditing();
	}});
    }
}
