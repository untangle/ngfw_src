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

package com.untangle.gui.transform;

import java.awt.Window;
import javax.swing.*;

import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.mvvm.*;
import com.untangle.mvvm.tran.*;

public class MStateMachine implements java.awt.event.ActionListener {

    // references to components that can generate actions
    private JButton saveJButton;
    private JButton reloadJButton;
    private JButton removeJButton;
    private JToggleButton powerJToggleButton;
    private BlinkJLabel stateJLabel;
    private JLabel messageJLabel;
    private MTransformJPanel mTransformJPanel;
    private MTransformControlsJPanel mTransformControlsJPanel;
    private MTransformDisplayJPanel mTransformDisplayJPanel;
    private Transform transform;

    // helpers
    String transformName;
    String displayName;

    public MStateMachine( MTransformJPanel mTransformJPanel ) {
        this.mTransformJPanel = mTransformJPanel;
        this.mTransformControlsJPanel = mTransformJPanel.mTransformControlsJPanel();
        this.mTransformDisplayJPanel = mTransformJPanel.mTransformDisplayJPanel();
        this.powerJToggleButton = mTransformJPanel.powerJToggleButton();
        this.transform = mTransformJPanel.getTransform();
        this.stateJLabel = mTransformJPanel.stateJLabel();
	this.messageJLabel = mTransformJPanel.messageTextJLabel();
        this.saveJButton = mTransformControlsJPanel.saveJButton();
        this.reloadJButton = mTransformControlsJPanel.reloadJButton();
        this.removeJButton = mTransformControlsJPanel.removeJButton();

        transformName = mTransformJPanel.getTransformDesc().getName();
        displayName = mTransformJPanel.getTransformDesc().getDisplayName();

	doRefreshState();
    }


    Boolean readOnlyPowerState = null;
    // ACTION MULTIPLEXER ///////////////////////////////
    public void actionPerformed(java.awt.event.ActionEvent evt) {
        Object source = evt.getSource();

        if( Util.getIsDemo() && !source.equals(reloadJButton) ){
	    if( source.equals(powerJToggleButton) ){
		if( readOnlyPowerState == null ){
		    readOnlyPowerState = new Boolean(!powerJToggleButton.isSelected());
		}
		if( powerJToggleButton.isSelected() != readOnlyPowerState )
		    powerJToggleButton.setSelected(readOnlyPowerState);
	    }
            return;
	}
        try{
            if( source.equals(saveJButton) ){
		if( !mTransformControlsJPanel.shouldSave() )
		    return;
		new SaveThread();
            }
            else if( source.equals(reloadJButton) ){
                new RefreshThread();
            }
            else if( source.equals(removeJButton) ){
                /* removeJButton.setEnabled(false); */
		RemoveProceedDialog dialog = RemoveProceedDialog.factory((Window)mTransformControlsJPanel.getContentJPanel().getTopLevelAncestor(),
									 displayName);
                if( dialog.isProceeding() ){
                    Util.getPolicyStateMachine().moveFromRackToToolbox(mTransformJPanel.getPolicy(),mTransformJPanel);
                }
                /* removeJButton.setEnabled(true); */
            }
            else if( source.equals(powerJToggleButton) ){
                int modifiers = evt.getModifiers();
                powerJToggleButton.setEnabled(false);
                // REMOVE
                if( (modifiers & java.awt.event.ActionEvent.SHIFT_MASK) > 0 ){
                    if( (modifiers & java.awt.event.ActionEvent.CTRL_MASK) == 0 ){
			RemoveProceedDialog dialog = RemoveProceedDialog.factory((Window)mTransformControlsJPanel.getContentJPanel().getTopLevelAncestor(),
										 displayName);
                        if( dialog.isProceeding() ){
                            Util.getPolicyStateMachine().moveFromRackToToolbox(mTransformJPanel.getPolicy(),mTransformJPanel);
                        }
                        else{
                            powerJToggleButton.setSelected( !powerJToggleButton.isSelected() );
                            powerJToggleButton.setEnabled(true);
                        }
                    }
                    else{
                        powerJToggleButton.setEnabled(true);
                    }
                }
                else{
                    if( transformName.equals("nat-transform") ){
                        if( (new PowerProceedDialog(displayName, powerJToggleButton.isSelected())).isProceeding() ){
                            new PowerThread();
                        }
                        else{
                            powerJToggleButton.setSelected( !powerJToggleButton.isSelected() );
                            powerJToggleButton.setEnabled(true);
                        }
                    }
                    else{
                        new PowerThread();
                    }
                }
            }
            else{ Util.printMessage("error: unknown action source: " + source); }
        }
        catch(Exception e){
            try{
                Util.handleExceptionWithRestart("Error handling action", e);
            }
            catch(Exception f){
                Util.handleExceptionNoRestart("Error getting last state", f);
                setProblemView(true);
            }
        }
    }
    ////////////////////////////////////////////


    // ACTION THREADS //////////////////////////
    class SaveThread extends Thread{
        public SaveThread(){
            super("MVCLIENT-StateMachineSaveThread: " + displayName);
	    setDaemon(true);
            setProcessingView(false);
	    mTransformControlsJPanel.getInfiniteProgressJComponent().start("Saving...");
            start();
        }
        public void run(){
            try{
                mTransformControlsJPanel.saveAll();
                mTransformControlsJPanel.refreshAll();
                mTransformControlsJPanel.populateAll();
            }
	    catch(ValidationException v){
		// this was handled with a dialog at a lower level
	    }
            catch(Exception e){
                try{ Util.handleExceptionWithRestart("Error doing save", e); }
                catch(Exception f){
                    Util.handleExceptionNoRestart("Error doing save", f);
                    setProblemView(true);
		    SaveFailureDialog.factory( (Window) mTransformControlsJPanel.getContentJPanel().getTopLevelAncestor(), displayName );
                }
            }

	    mTransformControlsJPanel.getInfiniteProgressJComponent().stopLater(MTransformControlsJPanel.MIN_PROGRESS_MILLIS);

            try{ refreshState(true); }
            catch(Exception e){
                try{ Util.handleExceptionWithRestart("Error doing save", e); }
                catch(Exception f){
                    Util.handleExceptionNoRestart("Error doing save", f);
                    setProblemView(true);
		    SaveFailureDialog.factory( (Window) mTransformControlsJPanel.getContentJPanel().getTopLevelAncestor(), displayName );
                }
            }
        }
    }

    

    class RefreshThread extends Thread{
        public RefreshThread(){
            super("MVCLIENT-StateMachineRefreshThread: " + displayName );
	    setDaemon(true);
            setProcessingView(false);
	    mTransformControlsJPanel.getInfiniteProgressJComponent().start("Refreshing...");
            start();
        }
        public void run(){
            try{
                mTransformControlsJPanel.refreshAll();
                mTransformControlsJPanel.populateAll();
            }
            catch(Exception e){
                try{ Util.handleExceptionWithRestart("Error doing refresh", e); }
                catch(Exception f){
                    Util.handleExceptionNoRestart("Error doing refresh", f);
                    setProblemView(true);		
		    RefreshFailureDialog.factory( (Window) mTransformControlsJPanel.getContentJPanel().getTopLevelAncestor(), displayName );
                }
            }

	    mTransformControlsJPanel.getInfiniteProgressJComponent().stopLater(MTransformControlsJPanel.MIN_PROGRESS_MILLIS);

	    try{ refreshState(true); }
	    catch(Exception e){
		try{ Util.handleExceptionWithRestart("Error doing refresh", e); }
                catch(Exception f){
                    Util.handleExceptionNoRestart("Error doing refresh", f);
                    setProblemView(true);		
		    RefreshFailureDialog.factory( (Window) mTransformControlsJPanel.getContentJPanel().getTopLevelAncestor(), displayName );
                }
	    }
		
        }
    }

    class PowerThread extends Thread{
        private final boolean powerOn;
        public PowerThread(){
            super("MVCLIENT-StateMachinePowerThread: " + displayName );
	    setDaemon(true);
            powerOn = powerJToggleButton.isSelected();
            if( powerOn )
                setStartingView(false);
            else
                setStoppingView(false);
            start();
        }

        public void run(){
            try{
                if(powerOn){
                    transform.start();
                }
                else
                    transform.stop();

                if( powerOn )
                    setOnView(true);
                else
                    setOffView(true);
            }
            catch(UnconfiguredException e){
                if( transformName.equals("openvpn-transform") ){
                    MOneButtonJDialog.factory((Window)mTransformJPanel.getTopLevelAncestor(), displayName,
                                              "You must configure OpenVPN as either a VPN Routing Server" +
                                              " or a VPN Client before you can turn it on.  You may do this" +
                                              " through its Setup Wizard (in its settings).", displayName + " Warning", "Warning");
                }
                setOffView(true);
            }
            catch(Exception e){
                try{
                    Util.handleExceptionWithRestart("Error doing power", e);
                }
                catch(Exception f){
                    Util.handleExceptionNoRestart("Error doing power", f);
                    setProblemView(true);
                    String action;
                    if( powerOn )
                        action = "turned on";
                    else
                        action = "turned off";
                    MOneButtonJDialog.factory((Window)mTransformJPanel.getTopLevelAncestor(), displayName,
                                              displayName + " could not be " + action + "." +
                                              "  Please contact Untangle Support.", displayName + " Warning", "Warning");
                }
            }
        }
    }
    ///////////////////////////////////////////////


    // VIEW SETTING ////////////////////////////////
    private void setProcessingView(boolean doLater){ setView( doLater, false, false, false, false, false, null, true, BlinkJLabel.PROCESSING_STATE ); }
    void setProblemView(boolean doLater){    setView( doLater, false, false, false, true,  false, null, true, BlinkJLabel.PROBLEM_STATE ); }
    private void setStartingView(boolean doLater){ setView( doLater, false, false, false, false, false, null, false, BlinkJLabel.STARTING_STATE ); }
    private void setStoppingView(boolean doLater){ setView( doLater, false, false, false, false, false, null, false, BlinkJLabel.STOPPING_STATE ); }
    void setRemovingView(boolean doLater){ setStoppingView(doLater); }
    private void setOnView(boolean doLater){
        setView( doLater, true, true, true, true, true, true, true,  BlinkJLabel.ON_STATE );
	mTransformJPanel.setPowerOnHintVisible(false);
    }
    private void setOffView(boolean doLater){       setView( doLater, true, true, true, true, true, false, false, BlinkJLabel.OFF_STATE ); }
    void setDisabledView(boolean doLater){    setView( doLater, false, false, false, true,  false, null, false, BlinkJLabel.DISABLED_STATE ); }

    private void setView(final boolean doLater, final boolean allControlsEnabled, final boolean saveEnabled,
                         final boolean refreshEnabled, final boolean removeEnabled, final boolean powerEnabled,
                         final Boolean powerOn, final boolean doVizUpdates, final int ledState){

        Runnable runnable = new Runnable(){
                public void run(){
                    if( powerOn != null){
                        powerJToggleButton.setSelected( powerOn );
                        powerJToggleButton.setEnabled(true);
                    }
                    stateJLabel.setViewState( ledState );
                }
            };
        if( doLater )
            SwingUtilities.invokeLater( runnable );
        else
            runnable.run();
	mTransformDisplayJPanel.setDoVizUpdates( doVizUpdates );
    }
    ///////////////////////////////////////////////


    // STATE REFRESHING //////////////////////////
    public void doRefreshState(){
        new RefreshStateThread();	
    }

    private void refreshState(boolean doLater) throws Exception {
        TransformState transformState = transform.getRunState();
        if( TransformState.RUNNING.equals( transformState ) )
            setOnView(doLater);
        else if( TransformState.INITIALIZED.equals( transformState ) )
            setOffView(doLater);
        else if( TransformState.DISABLED.equals( transformState ) )
            setDisabledView(doLater);
        else
            setProblemView(doLater);
	String extraText = mTransformJPanel.getNewMackageDesc().getExtraName();
	if( (extraText!=null) && (extraText.length()>0) )
	    messageJLabel.setText(extraText);
	else
	    messageJLabel.setText("");
    }

    class RefreshStateThread extends Thread{
        public RefreshStateThread(){
            super("MVCLIENT-StateMachineRefreshStateThread: " + displayName );
	    setDaemon(true);
            start();
        }
        public void run(){
            try{ refreshState(true); }
            catch(Exception e){
                try{ Util.handleExceptionWithRestart("Error refreshing state", e); }
                catch(Exception f){
                    Util.handleExceptionNoRestart("Error refreshing state", f);
                    setProblemView(true);
                }
            }
        }
    }
    ///////////////////////////////////

}
