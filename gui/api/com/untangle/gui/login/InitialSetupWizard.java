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

package com.untangle.gui.login;

import com.untangle.gui.widgets.wizard.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.util.*;
import com.untangle.gui.transform.*;

import com.untangle.mvvm.client.MvvmRemoteContextFactory;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Dimension;

public class InitialSetupWizard extends MWizardJDialog {

    private boolean isRegistered = false;
    
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

    public InitialSetupWizard() {
	setModal(true);
        setTitle("Untangle Server Setup Wizard");
        addWizardPageJPanel(new InitialSetupWelcomeJPanel(),         "1. Welcome", false, false);

        if( Util.getIsCD() ){
            addWizardPageJPanel(new InitialSetupContactJPanel(),         "2. Contact Information", true, true);
            //            addWizardPageJPanel(new InitialSetupKeyJPanel(),             "3. Activation Key", false, true);
            addWizardPageJPanel(new InitialSetupPasswordJPanel(),        "3. Admin Account & Time", true, true);        
            initialSetupInterfaceJPanel = new InitialSetupInterfaceJPanel();
            addWizardPageJPanel(initialSetupInterfaceJPanel,       "4. Interface Test", false, false);
            addWizardPageJPanel(new InitialSetupNetworkJPanel(),         "5. External Address", false, true);
            addWizardPageJPanel(new InitialSetupConnectivityJPanel(),    "6. Connectivity Test", false, true);
            addWizardPageJPanel(new InitialSetupRoutingJPanel(),         "7. Routing", false, true);
            addWizardPageJPanel(new InitialSetupEmailJPanel(),           "8. Email Settings", false, true);
            addWizardPageJPanel(new InitialSetupCongratulationsJPanel(), "9. Finished!", true, true);
        }
        else{
            addWizardPageJPanel(new InitialSetupLicenseJPanel(),         "2. License Agreement", false, false);
            addWizardPageJPanel(new InitialSetupContactJPanel(),         "3. Contact Information", false, false);
            addWizardPageJPanel(new InitialSetupKeyJPanel(),             "4. Activation Key", false, true);
            addWizardPageJPanel(new InitialSetupPasswordJPanel(),        "5. Admin Account & Time", true, true);        
            addWizardPageJPanel(new InitialSetupNetworkJPanel(),         "6. External Address", false, true);
            addWizardPageJPanel(new InitialSetupConnectivityJPanel(),    "7. Connectivity Test", false, true);
            addWizardPageJPanel(new InitialSetupRoutingJPanel(),         "8. Routing", false, true);
            addWizardPageJPanel(new InitialSetupEmailJPanel(),           "9. Email Settings", false, true);
            addWizardPageJPanel(new InitialSetupCongratulationsJPanel(), "10. Finished!", true, true);
        }
    }
    
    protected void wizardFinishedAbnormal(int currentPage){
        if( currentPage == 9 ){
            wizardFinishedNormal();
            return;
        }


        MTwoButtonJDialog dialog = MTwoButtonJDialog.factory(this, "Setup Wizard", "If you exit now, some of your settings may not be saved properly.<br>" +
                                                                                   "You should continue, if possible.  ", "Setup Wizard Warning", "Warning");
        dialog.setProceedText("Exit Wizard");
        dialog.setCancelText("Continue Wizard");
        dialog.setVisible(true);
        if( dialog.isProceeding() ){
            if ( ( currentPage >= 4 ) || ( Util.getIsCD() && ( currentPage >= 3 ) ) )
                isRegistered = true;
            if( currentPage <= 3 ){ // NOT REGISTERED, MUST DO WIZARD AGAIN
                MOneButtonJDialog.factory(this, "", MESSAGE_NOT_REGISTERED, MESSAGE_DIALOG_TITLE, "");
            }
            else if( currentPage == 4 ){ // PASSWORD NOT SET
                MOneButtonJDialog.factory(this, "", MESSAGE_NO_PASSWORD, MESSAGE_DIALOG_TITLE, "");
            }
            if( Util.getIsCD() )
                initialSetupInterfaceJPanel.finishedAbnormal();
            cleanupConnection();
            super.wizardFinishedAbnormal(currentPage);
        }
        else{
            return;
        }
    }
    
    protected void wizardFinishedNormal(){
        isRegistered = true;	
        if( (InitialSetupRoutingJPanel.getNatEnabled() && !InitialSetupRoutingJPanel.getNatChanged()) 
            || Util.isLocal() )
            cleanupConnection();	
        super.wizardFinishedNormal();
    }

    private void cleanupConnection(){
        if( Util.getMvvmContext() != null ){
            keepAliveThread.doShutdown();
            Util.setMvvmContext(null);	    
            try{
                MvvmRemoteContextFactory.factory().logout();
            }
            catch(Exception e){ Util.handleExceptionNoRestart("Error logging off", e); };
        }
    }
    
    public boolean isRegistered(){ return isRegistered; }    

}


