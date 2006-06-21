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

package com.metavize.gui.login;

import com.metavize.gui.widgets.wizard.*;
import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.util.*;
import com.metavize.gui.transform.*;

import com.metavize.mvvm.client.MvvmRemoteContextFactory;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Dimension;

public class InitialSetupWizard extends MWizardJDialog {

    private boolean isRegistered = false;
    
    private static final String MESSAGE_DIALOG_TITLE   = "Setup Wizard Warning";
    private static final String MESSAGE_NOT_REGISTERED = "You have not registered your EdgeGuard.  Please run the Setup Wizard again.";
    private static final String MESSAGE_NO_PASSWORD    = "You have not set the Admin account password.  <b>The default login/password is: admin/passwd</b>";

    // SHARED DATA //
    private static Object sharedData;
    public static Object getSharedData(){ return sharedData; }
    public static void setSharedData(Object data){ sharedData = data; }

    protected Dimension getContentJPanelPreferredSize(){ return new Dimension(535,480); }
    
    public InitialSetupWizard() {
	setModal(true);
        setTitle("Metavize EdgeGuard Setup Wizard");
        addWizardPageJPanel(new InitialSetupWelcomeJPanel(),         "1. Welcome", false, false);
        addWizardPageJPanel(new InitialSetupLicenseJPanel(),         "2. License Agreement", false, false);
        addWizardPageJPanel(new InitialSetupContactJPanel(),         "3. Contact Information", false, false);
        addWizardPageJPanel(new InitialSetupKeyJPanel(),             "4. Activation Key", false, true);
        addWizardPageJPanel(new InitialSetupPasswordJPanel(),        "5. Admin Account & Time", false, true);        
        addWizardPageJPanel(new InitialSetupNetworkJPanel(),         "6. External Address", true, true);
        addWizardPageJPanel(new InitialSetupRoutingJPanel(),         "7. Routing", false, true);
        addWizardPageJPanel(new InitialSetupConnectivityJPanel(),    "8. Connectivity Test", false, true);
        addWizardPageJPanel(new InitialSetupEmailJPanel(),           "9. Email Settings", false, true);
        addWizardPageJPanel(new InitialSetupCongratulationsJPanel(), "10. Finished!", true, true);
    }
    
    protected void wizardFinishedAbnormal(int currentPage){	
	MTwoButtonJDialog dialog = MTwoButtonJDialog.factory(this, "Setup Wizard", "If you exit now, " +
								 "some of your settings may not be saved properly.  " +
								 "You should continue, if possible.  ", "Setup Wizard Warning", "Warning");
	dialog.setProceedText("<html><b>Exit</b> Wizard</html>");
	dialog.setCancelText("<html><b>Continue</b> Wizard</html>");
	dialog.setVisible(true);
	if( dialog.isProceeding() ){
	    if( currentPage >= 4 )
		isRegistered = true;
	    if( currentPage <= 3 ){ // NOT REGISTERED, MUST DO WIZARD AGAIN
		MOneButtonJDialog.factory(this, "", MESSAGE_NOT_REGISTERED, MESSAGE_DIALOG_TITLE, "");
	    }
	    else if( currentPage == 4 ){ // PASSWORD NOT SET
		MOneButtonJDialog.factory(this, "", MESSAGE_NO_PASSWORD, MESSAGE_DIALOG_TITLE, "");
	    }
	    if(InitialSetupRoutingJPanel.getNatEnabled() && !InitialSetupRoutingJPanel.getNatChanged())
		cleanupConnection();
	    super.wizardFinishedAbnormal(currentPage);
	}
	else{
	    return;
	}
    }

    protected void wizardFinishedNormal(){
	isRegistered = true;
	
	if(InitialSetupRoutingJPanel.getNatEnabled() && !InitialSetupRoutingJPanel.getNatChanged())
	    cleanupConnection();
	
	super.wizardFinishedNormal();
    }
    private void cleanupConnection(){
	boolean wasLoggedIn = (Util.getMvvmContext() != null);
        Util.setMvvmContext(null);
	if( wasLoggedIn )
	    MvvmRemoteContextFactory.factory().logout();
    }
    
    public boolean isRegistered(){ return isRegistered; }    

}


