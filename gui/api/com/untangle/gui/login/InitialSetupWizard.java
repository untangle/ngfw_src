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

package com.untangle.gui.login;

import java.awt.Dimension;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.wizard.*;
import com.untangle.uvm.client.RemoteUvmContextFactory;

public class InitialSetupWizard extends MWizardJDialog {

    private static final String MESSAGE_DIALOG_TITLE   = "Setup Wizard Warning";
    private static final String MESSAGE_NOT_REGISTERED = "You have not registered your Untangle Server.<br>Please run the Setup Wizard again.";
    private static final String MESSAGE_NO_PASSWORD    = "You have not set the Admin account password.<br><b>The default login/password is: admin/passwd</b>";

    // SHARED DATA //
    private static Object sharedData;
    public static Object getSharedData(){ return sharedData; }
    public static void setSharedData(Object data){ sharedData = data; }
    private static KeepAliveThread keepAliveThread;
    public static void setKeepAliveThread(KeepAliveThread xKeepAliveThread){ keepAliveThread = xKeepAliveThread; }

    protected Dimension getContentJPanelPreferredSize(){ return new Dimension(535,480); }

    private InitialSetupInterfaceJPanel initialSetupInterfaceJPanel;
    private int lastPage = 0;

    public InitialSetupWizard() {
        setModal(true);
        setTitle("Untangle Server Setup Wizard");
        addWizardPageJPanel(new InitialSetupWelcomeJPanel(),         "1. Welcome", false, false);

        if( Util.isInsideVM()) {
            // VM Install needs to show license, needs interface test, auto key fetch
            addWizardPageJPanel(new InitialSetupLicenseJPanel(),         "2. License Agreement", false, false);
            addWizardPageJPanel(new InitialSetupContactJPanel(),         "3. Contact Information", true, true);
            addWizardPageJPanel(new InitialSetupPasswordJPanel(),        "4. Admin Account & Time", true, true);
            initialSetupInterfaceJPanel = new InitialSetupInterfaceJPanel();
            addWizardPageJPanel(initialSetupInterfaceJPanel,             "5. Interface Test", false, false);
            addWizardPageJPanel(new InitialSetupNetworkJPanel(),         "6. External Address", false, true);
            addWizardPageJPanel(new InitialSetupConnectivityJPanel(),    "7. Connectivity Test", false, true);
            addWizardPageJPanel(new InitialSetupRoutingJPanel(),         "8. Routing", false, true);
            addWizardPageJPanel(new InitialSetupEmailJPanel(),           "9. Email Settings", false, true);
            addWizardPageJPanel(new InitialSetupCongratulationsJPanel(), "10. Finished!", true, true);
            lastPage = 9;       // 0 based
        }
        else if ( !Util.isUntangleAppliance() ){
            // CD Install already showed license, needs interface test, auto key fetch
            addWizardPageJPanel(new InitialSetupContactJPanel(),         "2. Contact Information", true, true);
            addWizardPageJPanel(new InitialSetupPasswordJPanel(),        "3. Admin Account & Time", true, true);
            initialSetupInterfaceJPanel = new InitialSetupInterfaceJPanel();
            addWizardPageJPanel(initialSetupInterfaceJPanel,             "4. Interface Test", false, false);
            addWizardPageJPanel(new InitialSetupNetworkJPanel(),         "5. External Address", false, true);
            addWizardPageJPanel(new InitialSetupConnectivityJPanel(),    "6. Connectivity Test", false, true);
            addWizardPageJPanel(new InitialSetupRoutingJPanel(),         "7. Routing", false, true);
            addWizardPageJPanel(new InitialSetupEmailJPanel(),           "8. Email Settings", false, true);
            addWizardPageJPanel(new InitialSetupCongratulationsJPanel(), "9. Finished!", true, true);
            lastPage = 8;       // 0 based
        }
        else{
            // Appliance Install needs to show license, no interface test, manual key entry
            addWizardPageJPanel(new InitialSetupLicenseJPanel(),         "2. License Agreement", false, false);
            addWizardPageJPanel(new InitialSetupContactJPanel(),         "3. Contact Information", false, false);
            addWizardPageJPanel(new InitialSetupKeyJPanel(),             "4. Activation Key", false, true);
            addWizardPageJPanel(new InitialSetupPasswordJPanel(),        "5. Admin Account & Time", true, true);
            addWizardPageJPanel(new InitialSetupNetworkJPanel(),         "6. External Address", false, true);
            addWizardPageJPanel(new InitialSetupConnectivityJPanel(),    "7. Connectivity Test", false, true);
            addWizardPageJPanel(new InitialSetupRoutingJPanel(),         "8. Routing", false, true);
            addWizardPageJPanel(new InitialSetupEmailJPanel(),           "9. Email Settings", false, true);
            addWizardPageJPanel(new InitialSetupCongratulationsJPanel(), "10. Finished!", true, true);
            lastPage = 9;       // 0 based
        }
    }

    protected void wizardFinishedAbnormal(int currentPage){
        if( currentPage == lastPage ){
            wizardFinishedNormal();
            return;
        }

        MTwoButtonJDialog dialog = MTwoButtonJDialog.factory(this, "Setup Wizard", "If you exit now, some of your settings may not be saved properly.<br>" +
                                                             "You should continue, if possible.  ", "Setup Wizard Warning", "Warning");
        dialog.setProceedText("Exit Wizard");
        dialog.setCancelText("Continue Wizard");
        dialog.setVisible(true);
        if( dialog.isProceeding() ){
            if( Util.isInsideVM()) {
                if (currentPage <= 2) { // NOT REGISTERED, MUST DO WIZARD AGAIN
                    MOneButtonJDialog.factory(this, "", MESSAGE_NOT_REGISTERED, MESSAGE_DIALOG_TITLE, "");
                }
                else if (currentPage <= 3) { // PASSWORD NOT SET
                    MOneButtonJDialog.factory(this, "", MESSAGE_NO_PASSWORD, MESSAGE_DIALOG_TITLE, "");     
                }                
                initialSetupInterfaceJPanel.finishedAbnormal();
            }
            else if ( !Util.isUntangleAppliance() ) {
                if (currentPage <= 1) { // NOT REGISTERED, MUST DO WIZARD AGAIN
                    MOneButtonJDialog.factory(this, "", MESSAGE_NOT_REGISTERED, MESSAGE_DIALOG_TITLE, "");
                }
                else if (currentPage <= 2) { // PASSWORD NOT SET
                    MOneButtonJDialog.factory(this, "", MESSAGE_NO_PASSWORD, MESSAGE_DIALOG_TITLE, "");     
                }                
                initialSetupInterfaceJPanel.finishedAbnormal();
            }
            else {                
                if (currentPage <= 2) { // NOT REGISTERED, MUST DO WIZARD AGAIN
                    MOneButtonJDialog.factory(this, "", MESSAGE_NOT_REGISTERED, MESSAGE_DIALOG_TITLE, "");
                }
                else if (currentPage <= 4) { // PASSWORD NOT SET
                    MOneButtonJDialog.factory(this, "", MESSAGE_NO_PASSWORD, MESSAGE_DIALOG_TITLE, "");     
                }                                
            }
            
            cleanupConnection();
            super.wizardFinishedAbnormal(currentPage);
        }
        else{
            return;
        }
    }

    protected void wizardFinishedNormal(){
        if( (InitialSetupRoutingJPanel.getNatEnabled() && !InitialSetupRoutingJPanel.getNatChanged())
            || Util.isLocal() )
            cleanupConnection();
        super.wizardFinishedNormal();
    }

    private void cleanupConnection(){
        if( Util.getUvmContext() != null ){
            keepAliveThread.doShutdown();
            Util.setUvmContext(null);
            try{
                RemoteUvmContextFactory.factory().logout();
            }
            catch(Exception e){ Util.handleExceptionNoRestart("Error logging off", e); };
        }
    }
}


