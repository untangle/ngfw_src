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

package com.metavize.gui.install;

import com.incors.plaf.kunststoff.*;
import com.metavize.gui.widgets.wizard.*;
import com.metavize.gui.widgets.dialogs.*;
import java.awt.Dimension;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import java.awt.Color;

public class InstallWizard extends MWizardJDialog {

    protected Dimension getContentJPanelPreferredSize(){ return new Dimension(535,480); }
    
    public InstallWizard() {
	setModal(true);
        setTitle("Metavize EdgeGuard Hardware Wizard");
        addWizardPageJPanel(new InstallWelcomeJPanel(),   "1. Welcome", false, false);
        addWizardPageJPanel(new InstallBenchmarkJPanel(this), "2. Hardware Test", false, false);
        addWizardPageJPanel(new InstallWarningJPanel(),   "3. Warning", false, false);
    }

    public static boolean testsPassed(){
	return true;		
    }
    
    protected void wizardFinishedNormal(){
	super.wizardFinishedNormal();
	System.exit(0);
    }
    
    protected void wizardFinishedAbnormal(int currentPage){

	MTwoButtonJDialog dialog = MTwoButtonJDialog.factory(this, "Install Wizard", "If you exit now, " +
								 " you will not be able to continue installation.  " +
								 "You should continue, if possible.  ", "Install Wizard Warning", "Warning");
	dialog.setProceedText("<html><b>Exit</b> Wizard</html>");
	dialog.setCancelText("<html><b>Continue</b> Wizard</html>");
	dialog.setVisible(true);
	if( dialog.isProceeding() ){
	    super.wizardFinishedAbnormal(currentPage);
	    System.exit(1);
	}
	else{
	    return;
	}
    }

    public static void main(String[] args){

        try {
            KunststoffLookAndFeel kunststoffLaf = new KunststoffLookAndFeel();
            kunststoffLaf.setCurrentTheme(new KunststoffTheme());
            UIManager.setLookAndFeel(kunststoffLaf);
        }
        catch (Exception e) {
	    System.err.println("Error starting LAF:");
	    e.printStackTrace();
        }
	UIManager.put("ProgressBar.selectionForeground", new ColorUIResource(Color.BLACK));
	new InstallWizard().setVisible(true);
    }
}


