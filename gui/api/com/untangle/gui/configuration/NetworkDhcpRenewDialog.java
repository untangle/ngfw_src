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

package com.untangle.gui.configuration;

import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.util.Util;

import com.untangle.mvvm.NetworkingConfiguration;

import javax.swing.*;
import java.awt.Dialog;

final public class NetworkDhcpRenewDialog extends MOneButtonProgressJDialog {
    
    private NetworkingConfiguration networkingConfiguration = null;
    
    public NetworkDhcpRenewDialog(Dialog parentDialog) {
	super(parentDialog);
        this.setTitle("Renewing DHCP Lease");
        this.labelJLabel.setText("Renewing DHCP Lease");
        this.messageJLabel.setText("<html><center>You have requested that the Untangle Server contact the network's DHCP<br>server in order to renew its lease on DHCP settings.</center></html>");
        new DhcpLeaseRenewThread();
        this.setVisible(true);
    }
    
    public NetworkingConfiguration getNetworkingConfiguration(){
        return networkingConfiguration;
    }

    public void windowClosing(java.awt.event.WindowEvent windowEvent) {}

    private void proceedJButtonActionPerformed(java.awt.event.ActionEvent evt) {
	setVisible(false);
	dispose();
    }
        
    private class DhcpLeaseRenewThread extends Thread {
        public DhcpLeaseRenewThread(){
	    super("MVCLIENT-DhcpLeaseRenewThread");
	    setDaemon(true);
            NetworkDhcpRenewDialog.this.jProgressBar.setValue(0);
            NetworkDhcpRenewDialog.this.jProgressBar.setIndeterminate(true);
            NetworkDhcpRenewDialog.this.jProgressBar.setString("Renewing DHCP lease...");
	    NetworkDhcpRenewDialog.this.proceedJButton.setEnabled(false);
            this.start();
        }
        public void run(){
            try{
                NetworkDhcpRenewDialog.this.networkingConfiguration = Util.getNetworkManager().renewDhcpLease();
                Thread.currentThread().sleep(2000l);
                SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
                    NetworkDhcpRenewDialog.this.jProgressBar.setIndeterminate(false);
                    NetworkDhcpRenewDialog.this.jProgressBar.setString("DHCP lease renewed.  Proceeding.");
                }});
                Thread.currentThread().sleep(2000l);
                NetworkDhcpRenewDialog.this.setVisible(false);
                NetworkDhcpRenewDialog.this.dispose();
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error: unable to renew DHCP lease", e);
                SwingUtilities.invokeLater( new Runnable(){ public void run(){
                    NetworkDhcpRenewDialog.this.jProgressBar.setIndeterminate(false);
                    NetworkDhcpRenewDialog.this.jProgressBar.setString("DHCP lease renewal failure.  Please try again later.");
		    NetworkDhcpRenewDialog.this.proceedJButton.setEnabled(true);
                }});
            }

        }
    }
    
    
}
