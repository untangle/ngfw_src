/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */


package com.untangle.gui.pipeline;

import com.untangle.gui.transform.MTransformControlsJPanel;
import com.untangle.mvvm.portal.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.util.*;
import java.awt.Window;
import java.awt.event.*;
import java.util.List;
import javax.swing.CellEditor;
import javax.swing.SwingUtilities;

public class UidButtonRunnable implements ButtonRunnable, Comparable<UidButtonRunnable> {

    private String uid;
    private Window topLevelWindow;
    private MConfigJDialog mConfigJDialog;
    private CellEditor cellEditor;
    private boolean valueChanged;

    public UidButtonRunnable(String isEnabled){
    }

    public String getButtonText(){ return (uid==null?"any":uid); }
    public boolean isEnabled(){ return true; }
    public void setEnabled(boolean isEnabled){ }
    public boolean valueChanged(){ return valueChanged; }
    public void setUid(String uid){ this.uid = uid; }
    public String getUid(){ return (uid==null?"any":uid); }

    public void setTopLevelWindow(Window topLevelWindow){ this.topLevelWindow = topLevelWindow; }
    public void setCellEditor(CellEditor cellEditor){ this.cellEditor = cellEditor; }

    public void actionPerformed(ActionEvent evt){ run(); }
    public int compareTo(UidButtonRunnable uidButtonRunnable){
	if( (uid==null) && (uidButtonRunnable.uid==null))
	    return 0;
	else if( (uid!=null) && (uidButtonRunnable.uid==null))
	    return 1;
	else if( (uid==null) && (uidButtonRunnable.uid!=null))
	    return -1;
	else		
	    return uid.compareTo(uidButtonRunnable.uid);
    }
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
