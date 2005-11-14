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
import com.metavize.gui.util.*;
import com.metavize.gui.transform.*;

import com.metavize.mvvm.client.MvvmRemoteContextFactory;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Frame;

public class InitialSetupWizard extends MWizardJDialog {

    private String[] args;
    private InitialSetupSaveJPanel initialSetupSaveJPanel;
    
    public InitialSetupWizard(String[] args) {
	super((Frame)null, false);
	this.args = args;
        setTitle("Metavize EdgeGuard Setup Wizard");
        addSavableJPanel(new InitialSetupWelcomeJPanel(), "1. Welcome");
        addSavableJPanel(new InitialSetupLicenseJPanel(), "2. License Agreement");
        addSavableJPanel(new InitialSetupKeyJPanel(), "3. Activation Key");
        addSavableJPanel(new InitialSetupContactJPanel(), "4. Contact Information");
        addSavableJPanel(new InitialSetupPasswordJPanel(), "5. First Account");
        addSavableJPanel(new InitialSetupTimezoneJPanel(), "6. Timezone");
        addSavableJPanel(new InitialSetupNetworkJPanel(), "7. Network Settings");
        initialSetupSaveJPanel = new InitialSetupSaveJPanel();
        addSavableJPanel(initialSetupSaveJPanel, "8. Save Configuration");
        addSavableJPanel(new InitialSetupConnectivityJPanel(), "9. Connectivity Test");
        addSavableJPanel(new InitialSetupCongratulationsJPanel(), "10. Finished!");
        super.finishPage = 7;
    }
    
    protected void saveStarted(){
        initialSetupSaveJPanel.saveStarted();
    }
    
    protected void saveFinished(String message){
        initialSetupSaveJPanel.saveFinished(message);
    }
    
    protected void wizardFinished(){
        Util.setMvvmContext(null);
        MvvmRemoteContextFactory.factory().logout();
	new MLoginJFrame(args);
    }
    
    public void windowClosing(java.awt.event.WindowEvent evt){
	super.windowClosing(evt);
	System.exit(0);
    }
    
}


