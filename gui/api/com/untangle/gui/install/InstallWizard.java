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

package com.untangle.gui.install;

import java.awt.Color;
import java.awt.Dimension;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import java.io.FileWriter;


import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.wizard.*;

public class InstallWizard extends MWizardJDialog {

    protected Dimension getContentJPanelPreferredSize(){ return new Dimension(535,480); }
    private String[] args;
    

    public InstallWizard(String[] args) {
	this.args = args;
	setModal(true);
        setTitle("Untangle Platform Install Wizard");
        addWizardPageJPanel(new InstallWelcomeJPanel(),       "1. Welcome", false, false);
        addWizardPageJPanel(new InstallLicenseJPanel(),       "2. License Agreement", false, false);
        addWizardPageJPanel(new InstallDiskJPanel(this),      "3. Choose Disk", false, false);
        addWizardPageJPanel(new InstallBenchmarkJPanel(this), "4. Hardware Test", false, false);
        addWizardPageJPanel(new InstallWarningJPanel(),       "5. Final Warning", false, false);
    }

    private static String targetDisk;
    public static void setTargetDisk(String xTargetDisk){ targetDisk = xTargetDisk; }
    public static String getTargetDisk(){ return targetDisk; }

    protected void wizardFinishedNormal(){
	super.wizardFinishedNormal();

	if(args.length>0){
	    String path = args[0];
	    String output = "HD=\"" + getTargetDisk() + "\"" + "\n";
	    try{
            FileWriter fw = new FileWriter(path);
            fw.write(output);
            fw.close();
	    }
	    catch(Exception e){
            e.printStackTrace();
            System.exit(1);
	    }
	}
    
	System.exit(0);
    }

    protected void wizardFinishedAbnormal(int currentPage){
	MTwoButtonJDialog dialog = MTwoButtonJDialog.factory(this, "Install Wizard", "If you exit now, " +
							     " you will not be able to continue installation.  " +
							     "You should continue, if possible.  ", "Install Wizard Warning",
							     "Warning");
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
	    com.incors.plaf.alloy.AlloyLookAndFeel.setProperty("alloy.licenseCode", "7#Metavize_Inc.#1f75cs6#2n7ryw");
	    //com.incors.plaf.alloy.AlloyTheme theme = new com.incors.plaf.alloy.themes.glass.GlassTheme();
	    com.incors.plaf.alloy.AlloyTheme theme =
		com.incors.plaf.alloy.themes.custom.CustomThemeFactory.createTheme(new Color(152,152,171), // PROGRESS & SCROLL
										   new Color(215,215,215), // BACKGROUND
										   Color.ORANGE, // NO IDEA
										   new Color(50,50,50), // RADIO / CHECKBOX
										   //new Color(160,160,160), // MOUSEOVER
										   new Color(68,91,255), // MOUSEOVER
										   new Color(215,215,215)); // POPUPS
	    javax.swing.LookAndFeel alloyLnF = new com.incors.plaf.alloy.AlloyLookAndFeel(theme);
	    javax.swing.UIManager.setLookAndFeel(alloyLnF);
        /*
            KunststoffLookAndFeel kunststoffLaf = new KunststoffLookAndFeel();
            kunststoffLaf.setCurrentTheme(new KunststoffTheme());
            UIManager.setLookAndFeel(kunststoffLaf);
        */
        }
        catch (Exception e) {
	    e.printStackTrace();
        }
        UIManager.put("ProgressBar.selectionForeground", new ColorUIResource(Color.BLACK));
        new InstallWizard(args).setVisible(true);
    }
}


