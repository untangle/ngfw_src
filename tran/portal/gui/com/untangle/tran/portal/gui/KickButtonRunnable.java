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
        if(!Util.getIsDemo()){
            new KickUserThread();
        }
    }

    class KickUserThread extends Thread {
        MProgressJDialog mProgressJDialog;
        public KickUserThread(){
            setDaemon(true);
            setName("MV-CLIENT: KickUserThread");
            start();
            mProgressJDialog = MProgressJDialog.factory("Logging out...",
                                                        "The user: " + portalLogin.getUser()
                                                        + " from group: "
                                                        + (portalLogin.getGroup()==null?"(no group)":portalLogin.getGroup())
                                                        + " is being logged out.",
                                                        topLevelWindow);
            mProgressJDialog.getJProgressBar().setIndeterminate(true);
            mProgressJDialog.getJProgressBar().setString("Logging out...");
            mProgressJDialog.setVisible(true);
        }
        public void run(){

            try{
                Util.getRemotePortalManager().forceLogout(portalLogin);
                Thread.sleep(2000l);
                SwingUtilities.invokeLater( new Runnable(){ public void run(){
                    mProgressJDialog.getJProgressBar().setIndeterminate(false);
                    mProgressJDialog.getJProgressBar().setString("Success");
                    mProgressJDialog.getJProgressBar().setValue(100);
                }});
            }
            catch(Exception e){
                try{ Util.handleExceptionWithRestart("Error forcing logout", e); }
                catch(Exception f){ Util.handleExceptionNoRestart("Error forcing logout", f); }
            }
            finally{
                isEnabled = false;
                SwingUtilities.invokeLater( new Runnable(){ public void run(){
                    mProgressJDialog.setVisible(false);
                    cellEditor.stopCellEditing();
                }});
            }

        }
    }
}
