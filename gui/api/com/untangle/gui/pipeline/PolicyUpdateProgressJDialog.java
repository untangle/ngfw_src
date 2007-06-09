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

package com.untangle.gui.pipeline;

import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.uvm.policy.*;
import com.untangle.gui.configuration.*;

import java.awt.Frame;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;



public class PolicyUpdateProgressJDialog extends MProgressJDialog {

    private List<Policy> policies;

    public PolicyUpdateProgressJDialog( Frame parentFrame ) {
        super("Updating Racks", "Please wait while your racks are updated.", parentFrame);
    }

    public List<Policy> getPolicies(){ return policies; }

    public void setVisible(boolean isVisible){
        if(isVisible){
            new UpdatePolicyThread();
        }
        super.setVisible(isVisible);
    }

    private class UpdatePolicyThread extends Thread {
        public UpdatePolicyThread(){
            setDaemon(true);
            setName("MV-PolicyUpdateThread");
            PolicyUpdateProgressJDialog.this.getJProgressBar().setValue(1);
            PolicyUpdateProgressJDialog.this.getJProgressBar().setIndeterminate(true);
            PolicyUpdateProgressJDialog.this.getJProgressBar().setString("Updating...");
            start();
        }
        public void run(){
            try{
                policies = Util.getPolicyManager().getPolicyConfiguration().getPolicies();
                sleep(1500l);
            }
            catch(Exception e){
                try{ Util.handleExceptionWithRestart("Error handling policy manager action", e); }
                catch(Exception f){ Util.handleExceptionNoRestart("Error handling policy manager action", f); }
            }
            finally{
                SwingUtilities.invokeLater( new Runnable(){ public void run(){
                    PolicyUpdateProgressJDialog.this.setVisible(false);
                }});
            }
        }
    }
}
