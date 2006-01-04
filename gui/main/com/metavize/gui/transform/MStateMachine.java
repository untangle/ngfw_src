/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.transform;



import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.transform.BlinkJLabel;
import com.metavize.gui.util.*;

import com.metavize.mvvm.tran.*;
import com.metavize.mvvm.*;

import javax.swing.*;
import java.awt.Window;

public class MStateMachine implements java.awt.event.ActionListener {
    
    // references to components that can generate actions
    private JButton saveJButton;
    private JButton reloadJButton;
    private JButton removeJButton;
    private JToggleButton powerJToggleButton;
    private BlinkJLabel stateJLabel;
    private MTransformJPanel mTransformJPanel;
    private MTransformControlsJPanel mTransformControlsJPanel;
    private MTransformDisplayJPanel mTransformDisplayJPanel;
    private TransformContext transformContext;
    private MackageDesc mackageDesc;

    // helpers
    String transformName;
    String displayName;

    public MStateMachine( MTransformJPanel mTransformJPanel ) {                             
         this.mTransformJPanel = mTransformJPanel;
         this.mTransformControlsJPanel = mTransformJPanel.mTransformControlsJPanel();
         this.mTransformDisplayJPanel = mTransformJPanel.mTransformDisplayJPanel();
         this.powerJToggleButton = mTransformJPanel.powerJToggleButton();
         this.transformContext = mTransformJPanel.getTransformContext();
	 this.mackageDesc = mTransformJPanel.getMackageDesc();
         this.stateJLabel = mTransformJPanel.stateJLabel();
         this.saveJButton = mTransformControlsJPanel.saveJButton();
         this.reloadJButton = mTransformControlsJPanel.reloadJButton();
         this.removeJButton = mTransformControlsJPanel.removeJButton();
    
	 transformName = mackageDesc.getName();
	 displayName = mackageDesc.getDisplayName();
              
         new RefreshStateThread();
    }

 
    // ACTION MULTIPLEXER ///////////////////////////////
    public void actionPerformed(java.awt.event.ActionEvent evt) {              
        Object source = evt.getSource();

        if( Util.getIsDemo() && !source.equals(reloadJButton) )
            return;
        try{            
            if( source.equals(saveJButton) ){
                if( transformName.equals("nat-transform") ){
		    saveJButton.setEnabled(false);
                    if( (new SaveProceedDialog( displayName )).isProceeding() ){
			new SaveThread();
		    }
		    else{
			saveJButton.setEnabled(true);
		    }
                }
		else{
                    new SaveThread();
		}
            }
            else if( source.equals(reloadJButton) ){
		new RefreshThread();
	    }
            else if( source.equals(removeJButton) ){
		removeJButton.setEnabled(false);
                if( (new RemoveProceedDialog(displayName)).isProceeding() ){
                    Util.getPolicyStateMachine().moveFromRackToToolbox(mTransformJPanel.getPolicy(),mTransformJPanel);
                }
		removeJButton.setEnabled(true);
            }
            else if( source.equals(powerJToggleButton) ){ 
		int modifiers = evt.getModifiers();
		powerJToggleButton.setEnabled(false);
		// REMOVE
		if( (modifiers & java.awt.event.ActionEvent.SHIFT_MASK) > 0 ){
		    if( (modifiers & java.awt.event.ActionEvent.CTRL_MASK) == 0 ){
                        if( (new RemoveProceedDialog(displayName)).isProceeding() ){
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
	    super("MVCLIENT-MStateMachine.SaveThread: " + mackageDesc.getDisplayName());
	    saveJButton.setIcon(Util.getButtonSaving());
	    setProcessingView(false);
	    this.start();
	}
	public void run(){
	    try{
		mTransformControlsJPanel.saveAll();
		refreshState(true);
	    }
	    catch(Exception e){
		try{
		    Util.handleExceptionWithRestart("Error doing save", e);
		}
		catch(Exception f){
		    Util.handleExceptionNoRestart("Error doing save", f);
		    setProblemView(true);
		}
	    }
	    finally{
		SwingUtilities.invokeLater( new Runnable(){ public void run(){
		    saveJButton.setIcon(Util.getButtonSaveSettings());
		}});
	    }
	}
    }


    class RefreshThread extends Thread{
	public RefreshThread(){
	    super("MVCLIENT-MStateMachine.RefreshThread: " + mackageDesc.getDisplayName());
	    reloadJButton.setIcon(Util.getButtonReloading());
	    setProcessingView(false);
	    this.start();
	}
	public void run(){
	    try{
		mTransformControlsJPanel.refreshAll();
		refreshState(true);
	    }
	    catch(Exception e){
		try{
		    Util.handleExceptionWithRestart("Error doing refresh", e);
		}
		catch(Exception f){
		    Util.handleExceptionNoRestart("Error doing refresh", f);
		    setProblemView(true);
		}
	    }
	    finally{
		SwingUtilities.invokeLater( new Runnable(){ public void run(){
		    reloadJButton.setIcon(Util.getButtonReloadSettings());
		}});
	    }
	}
    }

    class PowerThread extends Thread{
	private final boolean powerOn;
	public PowerThread(){
	    super("MVCLIENT-MStateMachine.PowerThread: " + mackageDesc.getDisplayName());
	    powerOn = powerJToggleButton.isSelected();
	    if( powerOn )
		setStartingView(false);
	    else
		setStoppingView(false);
	    this.start();
	}

	public void run(){
	    try{
		if(powerOn){
		    transformContext.transform().start();
		}
		else
		    transformContext.transform().stop();
		
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
					      " through its Setup Wizard (in its settings).", displayName + " Confirmation", "Confirmation");
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
					      "  Please contact Metavize for further support.", displayName + " Warning", "Warning");
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
    private void setOnView(boolean doLater){ 	    setView( doLater, true, true, true, true, true, true, true,  BlinkJLabel.ON_STATE );
    mTransformJPanel.setPowerOnHintVisible(false); }
    private void setOffView(boolean doLater){ 	    setView( doLater, true, true, true, true, true, false, false, BlinkJLabel.OFF_STATE ); }
    
    private void setView(final boolean doLater, final boolean allControlsEnabled, final boolean saveEnabled,
			 final boolean refreshEnabled, final boolean removeEnabled, final boolean powerEnabled,
			 final Boolean powerOn, final boolean updateGraph, final int ledState){

	Runnable runnable = new Runnable(){
		public void run(){
		    mTransformControlsJPanel.setAllEnabled( allControlsEnabled );
		    saveJButton.setEnabled( saveEnabled );
		    reloadJButton.setEnabled( refreshEnabled );
		    removeJButton.setEnabled( removeEnabled );
		    if( Util.getIsDemo() )
			powerJToggleButton.setEnabled(false);
		    else
			powerJToggleButton.setEnabled( powerEnabled );
		    
		    if( powerOn != null){
			boolean wasEnabled = powerJToggleButton.isEnabled();
			powerJToggleButton.setEnabled(false);
			powerJToggleButton.setSelected( powerOn );
			powerJToggleButton.setEnabled(wasEnabled);
		    }
		    mTransformDisplayJPanel.setUpdateGraph( updateGraph );
		    stateJLabel.setViewState( ledState );
		}
	    };
	if( doLater )
	    SwingUtilities.invokeLater( runnable );
	else
	    runnable.run();
    }
    ///////////////////////////////////////////////


    // STATE REFRESHING //////////////////////////
    private void refreshState(boolean doLater){
	TransformState transformState = transformContext.transform().getRunState();
	if( TransformState.RUNNING.equals( transformState ) )
	    setOnView(doLater);
	else if( TransformState.INITIALIZED.equals( transformState ) )
	    setOffView(doLater);
	else
	    setProblemView(doLater);
    }
     
    class RefreshStateThread extends Thread{
	public RefreshStateThread(){
	    super("MVCLIENT-MStateMachine.RefreshStateThread: " + mackageDesc.getDisplayName());
	    this.start();
	}
	public void run(){
	    try{
		refreshState(true);
	    }
	    catch(Exception e){
		try{
		    Util.handleExceptionWithRestart("Error refreshing state", e);
		}
		catch(Exception f){
		    Util.handleExceptionNoRestart("Error refreshing state: ", f);
		    setProblemView(true);
		}
	    }
	}
    }
    ///////////////////////////////////
    
}
