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


package com.untangle.tran.portal.gui;

import java.awt.Window;
import java.awt.event.*;
import javax.swing.CellEditor;
import javax.swing.SwingUtilities;

import com.untangle.gui.transform.MTransformControlsJPanel;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.mvvm.portal.*;

public class SettingsButtonRunnable implements ButtonRunnable {
    private boolean isUserType;
    private boolean isEnabled;
    private PortalUser portalUser;
    private PortalGroup portalGroup;
    private Window topLevelWindow;
    private MTransformControlsJPanel mTransformControlsJPanel;
    private boolean valueChanged;
    private CellEditor cellEditor;

    public SettingsButtonRunnable(String isEnabled){
        if( "true".equals(isEnabled) ) {
            this.isEnabled = true;
        }
        else if( "false".equals(isEnabled) ){
            this.isEnabled = false;
        }
    }
    public String getButtonText(){ return "Edit"; }
    public boolean isEnabled(){ return isEnabled; }
    public boolean valueChanged(){ return valueChanged; }
    public void setCellEditor(CellEditor cellEditor){ this.cellEditor = cellEditor; }
    public void setEnabled(boolean isEnabled){ this.isEnabled = isEnabled; }
    public void setUserType(boolean isUserType){ this.isUserType = isUserType; }
    public void setPortalUser(PortalUser portalUser){ this.portalUser = portalUser; }
    public void setPortalGroup(PortalGroup portalGroup){ this.portalGroup = portalGroup; }
    public void setTopLevelWindow(Window topLevelWindow){ this.topLevelWindow = topLevelWindow; }
    public void setMTransformControlsJPanel(MTransformControlsJPanel mTransformControlsJPanel){ this.mTransformControlsJPanel = mTransformControlsJPanel; }
    public void actionPerformed(ActionEvent evt){ run(); }
    public void run(){
        if( isUserType ){
            UserSettingsJDialog userSettingsJDialog = UserSettingsJDialog.factory(topLevelWindow, portalUser, mTransformControlsJPanel);
            userSettingsJDialog.setVisible(true);
            //valueChanged = userSettingsJDialog.getSettingsSaved();
        }
        else{
            GroupSettingsJDialog groupSettingsJDialog = GroupSettingsJDialog.factory(topLevelWindow, portalGroup, mTransformControlsJPanel);
            groupSettingsJDialog.setVisible(true);
            //valueChanged = groupSettingsJDialog.getSettingsSaved();
        }

        /*
          SwingUtilities.invokeLater( new Runnable(){ public void run(){
          cellEditor.stopCellEditing();
          }});
        */
    }
}
