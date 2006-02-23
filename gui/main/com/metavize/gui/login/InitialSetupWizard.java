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

public class InitialSetupWizard extends MWizardJDialog {

    private boolean isRegistered = false;
    
    private static final String MESSAGE_DIALOG_TITLE   = "Setup Wizard Warning";
	private static final String MESSAGE_NOT_FINISHED = "You must complete the Setup Wizard before exiting.";
	
    private static final String MESSAGE_NOT_REGISTERED = "You have not registered your EdgeGuard.  Please run the Setup Wizard again.";
    private static final String MESSAGE_NOT_CONFIGURED = "You have registered your EdgeGuard, but you have not configured other " +
	"necessary settings.  You may do this in the Config Panel after logging in.  Your default login/password is: admin/passwd";

    // SHARED DATA //
    private static Object sharedData;
    public static Object getSharedData(){ return sharedData; }
    public static void setSharedData(Object data){ sharedData = data; }
    
    public InitialSetupWizard() {
	setModal(true);
        setTitle("Metavize EdgeGuard Setup Wizard");
        addWizardPageJPanel(new InitialSetupWelcomeJPanel(),         "1. Welcome", false, false);
        addWizardPageJPanel(new InitialSetupLicenseJPanel(),         "2. License Agreement", false, false);
        addWizardPageJPanel(new InitialSetupContactJPanel(),         "3. Contact Information", false, false);
        addWizardPageJPanel(new InitialSetupKeyJPanel(),             "4. Activation Key", false, true);
        addWizardPageJPanel(new InitialSetupRoutingJPanel(),         "5. Routing", true, true);
        addWizardPageJPanel(new InitialSetupNetworkJPanel(),         "6. External Address", false, true);
        addWizardPageJPanel(new InitialSetupConnectivityJPanel(),    "7. Connectivity Test", false, true);
        addWizardPageJPanel(new InitialSetupEmailJPanel(),           "8. Email Settings", false, true);
        addWizardPageJPanel(new InitialSetupPasswordJPanel(),        "9. Admin Account & Time", false, true);        
        addWizardPageJPanel(new InitialSetupCongratulationsJPanel(), "10. Finished!", true, true);
    }
    
    protected void wizardFinishedAbnormal(int currentPage){
	
	MTwoButtonJDialog dialog = MTwoButtonJDialog.factory(this, "Setup Wizard", "If you exit now, some of your settings may not be saved properly.  You should continue, if possible.", "Setup Wizard Warning", "Warning");
	
	if( dialog.isProceeding() )
	    super.wizardFinishedAbnormal(currentPage);
			/*
        if( currentPage <= 3 ){ // NOT REGISTERED, MUST DO WIZARD AGAIN
            new MOneButtonJDialog(this, MESSAGE_DIALOG_TITLE, MESSAGE_NOT_REGISTERED);
        }
        else if( currentPage <= 8 ){ // REGISTERED
            new MOneButtonJDialog(this, MESSAGE_DIALOG_TITLE, MESSAGE_NOT_CONFIGURED);
        }
	if( currentPage >= 4 )
	    isRegistered = true;
        cleanupConnection();
	super.wizardFinishedAbnormal(currentPage);
			 **/
    }

    protected void wizardFinishedNormal(){
	isRegistered = true;
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


