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
    
    private static final String MESSAGE_DIALOG_TITLE = "Setup Wizard Warning";
    private static final String MESSAGE_NOT_REGISTERED = "You have not registered your EdgeGuard.  Please run the Setup Wizard again.";
    private static final String MESSAGE_NOT_CONFIGURED = "You have registered your EdgeGuard, but not configured its network settings.  You may do this by logging is with admin/passwd, and going to the Config Panel.";
    private static final String MESSAGE_NO_EMAIL = "You have registered and configured your EdgeGuard, but you have not configured your email server.  You may do this by logging in with admin/passwd, and going to the Config Panel.";
    private static final String MESSAGE_NO_ACCOUNT = "You have registered and configured your EdgeGuard, but you have not changed your password.  You may do this by logging in with admin/passwd, and going to the Config Panel.";
    
    public InitialSetupWizard() {
        setTitle("Metavize EdgeGuard Setup Wizard");
        addWizardPageJPanel(new InitialSetupWelcomeJPanel(), "1. Welcome", false, false);
        addWizardPageJPanel(new InitialSetupLicenseJPanel(), "2. License Agreement", false, false);
        addWizardPageJPanel(new InitialSetupContactJPanel(), "3. Contact Information", false, false);
        addWizardPageJPanel(new InitialSetupKeyJPanel(), "4. Activation Key", false, true);
        addWizardPageJPanel(new InitialSetupTimezoneJPanel(), "5. Timezone", true, true);
        addWizardPageJPanel(new InitialSetupNetworkJPanel(), "6. Network Settings", false, true);
        addWizardPageJPanel(new InitialSetupConnectivityJPanel(), "7. Connectivity Test", false, true);
        addWizardPageJPanel(new InitialSetupEmailJPanel(), "8. Email Server", false, true);
        addWizardPageJPanel(new InitialSetupPasswordJPanel(), "9. Admin Account", false, true);        
        addWizardPageJPanel(new InitialSetupCongratulationsJPanel(), "10. Finished!", true, true);
    }
    
    protected void wizardFinishedAbnormal(int currentPage){
        if( currentPage <= 3 ){ // NOT REGISTERED, MUST DO WIZARD AGAIN
            new MOneButtonJDialog(this, MESSAGE_DIALOG_TITLE, MESSAGE_NOT_REGISTERED);
        }
        else if( currentPage <= 5 ){ // REGISTERED, BUT NOT CONFIGURED, NO EMAIL, NO ACCOUNT
            new MOneButtonJDialog(this, MESSAGE_DIALOG_TITLE, MESSAGE_NOT_CONFIGURED);
        }
        else if( currentPage <= 7 ){ // REGISTERED, CONFIGURED, BUT NO EMAIL, NO ACCOUNT
            new MOneButtonJDialog(this, MESSAGE_DIALOG_TITLE, MESSAGE_NO_EMAIL);
        }
        else if( currentPage <= 8 ){ // REGISTERED, CONFIGURED, EMAIL SET, BUT NO ACCOUNT
            new MOneButtonJDialog(this, MESSAGE_DIALOG_TITLE, MESSAGE_NO_ACCOUNT);
        }
        cleanupConnection();
	super.wizardFinishedAbnormal(currentPage);
    }

    protected void wizardFinishedNormal(){
        cleanupConnection();
	super.wizardFinishedNormal();
    }
    private void cleanupConnection(){
	boolean wasLoggedIn = (Util.getMvvmContext() != null);
        Util.setMvvmContext(null);
	if( wasLoggedIn )
	    MvvmRemoteContextFactory.factory().logout();
    }
    
}


