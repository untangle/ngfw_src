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
import java.util.List;
import javax.swing.CellEditor;
import javax.swing.SwingUtilities;

public class UidButtonRunnable implements ButtonRunnable {

    private String uid;
    private Window topLevelWindow;
    private MTransformControlsJPanel mTransformControlsJPanel;
    private CellEditor cellEditor;
    private boolean valueChanged;

    public UidButtonRunnable(String isEnabled){
    }

    public String getButtonText(){ return (uid==null?"[no user id/login]":uid); }
    public boolean isEnabled(){ return true; }
    public void setEnabled(boolean isEnabled){ }
    public boolean valueChanged(){ return valueChanged; }
    public void setUid(String uid){ this.uid = uid; }
    public String getUid(){ return uid; }

    public void setTopLevelWindow(Window topLevelWindow){ this.topLevelWindow = topLevelWindow; }
    public void setCellEditor(CellEditor cellEditor){ this.cellEditor = cellEditor; }
    public void setMTransformControlsJPanel(MTransformControlsJPanel mTransformControlsJPanel){ this.mTransformControlsJPanel = mTransformControlsJPanel; }
    public void actionPerformed(ActionEvent evt){ run(); }
    public void run(){
	UidSelectJDialog uidSelectJDialog = UidSelectJDialog.factory(topLevelWindow);
	uidSelectJDialog.setVisible(true);
	String newUid = uidSelectJDialog.getUid();
	if( (uid == null) && (newUid == null) ){
	    valueChanged = false;
	}
	else if( (uid != null) && (newUid == null) ){
	    valueChanged = false;
	}
	else if( (uid == null) && (newUid != null) ){
	    valueChanged = true;
	}
	else{
	    valueChanged = !uid.equals(newUid);
	}
	if(valueChanged)
	    uid = newUid;
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    cellEditor.stopCellEditing();
	}});
    }
}
