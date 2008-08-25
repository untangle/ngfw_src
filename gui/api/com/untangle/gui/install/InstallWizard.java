/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.gui.install;

import java.awt.Color;
import java.awt.Dimension;
import java.io.FileWriter;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

import com.untangle.gui.util.Util;
import com.untangle.gui.widgets.dialogs.MTwoButtonJDialog;
import com.untangle.gui.widgets.wizard.MWizardJDialog;

public class InstallWizard extends MWizardJDialog {

    protected Dimension getContentJPanelPreferredSize(){ return new Dimension(535,480); }
    private String[] args;


    public InstallWizard(String[] args) {
        this.args = args;
        setModal(true);
        setTitle(Util.tr("Untangle Platform Install Wizard"));

        InstallWelcomeJPanel iwjp = new InstallWelcomeJPanel();
        Util.addLocalizable(iwjp);
        addWizardPageJPanel(iwjp, Util.marktr("1. Language"), false, false);
        InstallLicenseJPanel iljp = new InstallLicenseJPanel();
        Util.addLocalizable(iljp);
        addWizardPageJPanel(iljp, Util.marktr("2. License Agreement"), false, false);
        InstallDiskJPanel idjp = new InstallDiskJPanel(this);
        Util.addLocalizable(idjp);
        addWizardPageJPanel(idjp, Util.marktr("3. Choose Disk"), false, false);
        InstallBenchmarkJPanel ibjp = new InstallBenchmarkJPanel(this);
        Util.addLocalizable(ibjp);
        addWizardPageJPanel(ibjp, Util.marktr("4. Hardware Test"), false, false);
        InstallWarningJPanel iwajp = new InstallWarningJPanel();
        Util.addLocalizable(iwajp);
        addWizardPageJPanel(iwajp, Util.marktr("5. Final Warning"), false, false);

        Util.addLocalizable(this);
    }

    public void reloadStrings()
    {
        super.reloadStrings();
        setTitle(Util.tr("Untangle Platform Install Wizard"));
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
        MTwoButtonJDialog dialog = MTwoButtonJDialog.factory(this, Util.tr("Install Wizard"),
                                                             Util.tr("If you exit now, you will not be able to continue installation. You should continue, if possible."),
                                                             Util.tr("Install Wizard Warning"),
                                                             Util.tr("Warning"));
        dialog.setProceedText(Util.tr("<html><b>Exit</b> Wizard</html>"));
        dialog.setCancelText(Util.tr("<html><b>Continue</b> Wizard</html>"));
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
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        UIManager.put("ProgressBar.selectionForeground", new ColorUIResource(Color.BLACK));
        new InstallWizard(args).setVisible(true);
    }
}
