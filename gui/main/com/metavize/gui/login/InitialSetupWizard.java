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

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Frame;

public class InitialSetupWizard extends MWizardJDialog {

    private String[] args;
    
    public InitialSetupWizard(String[] args) {
	super((Frame)null, true);
	this.args = args;
        setTitle("Metavize EdgeGuard Setup Wizard");
        addSavableJPanel(new InitialSetupWelcomeJPanel(), "1. Welcome");
        addSavableJPanel(new InitialSetupLicenseJPanel(), "2. License Agreement");
        addSavableJPanel(new InitialSetupKeyJPanel(), "3. Activation Key");
        addSavableJPanel(new InitialSetupContactJPanel(), "4. Contact Info");
        addSavableJPanel(new InitialSetupPasswordJPanel(), "5. First Account");
        addSavableJPanel(new InitialSetupTimezoneJPanel(), "6. Timezone");
        addSavableJPanel(new InitialSetupNetworkJPanel(), "7. Network Settings");
        addSavableJPanel(new InitialSetupCongratulationsJPanel(), "8. Finished!");
    }

    protected void wizardFinished(){
	new MLoginJFrame(args);
    }
    
    public void windowClosing(java.awt.event.WindowEvent evt){
	super.windowClosing(evt);
	System.exit(0);
    }
    
}


