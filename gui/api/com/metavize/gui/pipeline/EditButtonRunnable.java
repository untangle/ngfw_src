/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */


package com.metavize.gui.pipeline;

import com.metavize.gui.transform.MTransformControlsJPanel;
import com.metavize.mvvm.portal.*;
import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.util.*;
import java.awt.Window;
import java.awt.Dialog;
import java.awt.event.*;
import java.util.List;
import java.util.Vector;
import javax.swing.CellEditor;
import javax.swing.SwingUtilities;

public class EditButtonRunnable implements ButtonRunnable {

    private Vector row;
    private Window topLevelWindow;
    private MConfigJDialog mConfigJDialog;
    private CellEditor cellEditor;
    private boolean valueChanged;

    public EditButtonRunnable(String isEnabled){
    }

    public String getButtonText(){ return "<html>edit</html>"; }
    public boolean isEnabled(){ return true; }
    public void setEnabled(boolean isEnabled){ }
    public boolean valueChanged(){ return valueChanged; }
    public void setRow(Vector row){ this.row = row; }
    public Vector getRow(){ return row; }

    public void setTopLevelWindow(Window topLevelWindow){ this.topLevelWindow = topLevelWindow; }
    public void setCellEditor(CellEditor cellEditor){ this.cellEditor = cellEditor; }

    public void actionPerformed(ActionEvent evt){ run(); }
    public void run(){

        PolicyWizardJDialog policyWizardJDialog = new PolicyWizardJDialog((Dialog)topLevelWindow, row);
        policyWizardJDialog.setVisible(true);
        if(policyWizardJDialog.isProceeding())
            valueChanged = true;
        else
            valueChanged = false;

	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    cellEditor.stopCellEditing();
	}});
    }
}
