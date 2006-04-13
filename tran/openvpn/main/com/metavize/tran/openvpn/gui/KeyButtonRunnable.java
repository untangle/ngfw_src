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


package com.metavize.tran.openvpn.gui;
import com.metavize.tran.openvpn.*;
import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.util.*;
import java.awt.Window;
import java.awt.event.*;

public class KeyButtonRunnable implements ButtonRunnable {
    private boolean isEnabled;
    private VpnClient vpnClient;
    private static VpnTransform vpnTransform;
    private Window topLevelWindow;
    public KeyButtonRunnable(String isEnabled){
	if( "true".equals(isEnabled) ) {
	    this.isEnabled = true;
	}
	else if( "false".equals(isEnabled) ){
	    this.isEnabled = false;
	}
    }
    public String getButtonText(){ return "Distribute Client"; }
    public boolean isEnabled(){ return isEnabled; }
	public void setEnabled(boolean isEnabled){ this.isEnabled = isEnabled; }
    public void setVpnClient(VpnClient vpnClient){ this.vpnClient = vpnClient; }
    public static void setVpnTransform(VpnTransform vpnTransformX){ vpnTransform = vpnTransformX; }
    public void setTopLevelWindow(Window topLevelWindow){ this.topLevelWindow = topLevelWindow; }
	public void actionPerformed(ActionEvent evt){ run(); }
    public void run(){
	KeyJDialog keyJDialog = KeyJDialog.factory(topLevelWindow, vpnClient);
	keyJDialog.setVisible(true);
	if( keyJDialog.isProceeding() ){
	    if( keyJDialog.isUsbSelected() ){
		vpnClient.setDistributeUsb(true);
	    }
	    else if( keyJDialog.isEmailSelected() ){
		vpnClient.setDistributeUsb(false);
                vpnClient.setDistributionEmail( keyJDialog.getEmailAddress() );
	    }
	    try{
		vpnTransform.distributeClientConfig( vpnClient );
		String successString;
		if( vpnClient.getDistributeUsb() )
		    successString = "<html>OpenVpn successfully saved your digital key to your USB key.</html>";
		else
		    successString = "<html>OpenVpn successfully sent your digital key via email.</html>";

		MOneButtonJDialog.factory(topLevelWindow, "OpenVPN", successString, "OpenVPN Confirmation", "Confirmation" );
	    }
	    catch(Exception e){
		Util.handleExceptionNoRestart("Error saving/sending key:", e);
		String warningString;
		if( vpnClient.getDistributeUsb() )
		    warningString = "<html>OpenVpn was not able to save your digital key to your USB key.  Please try again.</html>";
		else
		    warningString = "<html>OpenVpn was not able to send your digital key via email.  Please try again.</html>";
		MOneButtonJDialog.factory(topLevelWindow, "OpenVPN", warningString, "OpenVPN Warning", "Warning" );
	    }
	}
    }
}
