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

package com.untangle.gui.configuration;

import java.awt.Dialog;
import javax.swing.*;

import com.untangle.gui.util.Util;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.uvm.networking.BasicNetworkSettings;

final public class NetworkDhcpRenewDialog extends MOneButtonProgressJDialog {

    private BasicNetworkSettings basicSettings = null;

    public NetworkDhcpRenewDialog(Dialog parentDialog) {
        super(parentDialog);
        this.setTitle("Renewing DHCP Lease");
        this.labelJLabel.setText("Renewing DHCP Lease");
        this.messageJLabel.setText("<html>You have requested that the Untangle Server contact the networks"
                                   +"<br>DHCP server in order to renew its lease on DHCP settings.</html>");
        new DhcpLeaseRenewThread();
        this.setVisible(true);
    }

    public BasicNetworkSettings getBasicSettings(){
        return basicSettings;
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
            NetworkDhcpRenewDialog.this.jProgressBar.setValue(1);
            NetworkDhcpRenewDialog.this.jProgressBar.setIndeterminate(true);
            NetworkDhcpRenewDialog.this.jProgressBar.setString("Renewing DHCP lease...");
            NetworkDhcpRenewDialog.this.proceedJButton.setEnabled(false);
            this.start();
        }
        public void run(){
            try{
                NetworkDhcpRenewDialog.this.basicSettings = Util.getNetworkManager().renewDhcpLease();
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
