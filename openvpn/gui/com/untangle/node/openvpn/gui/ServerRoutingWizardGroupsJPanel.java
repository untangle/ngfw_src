/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.openvpn.gui;

import java.util.*;
import javax.swing.SwingUtilities;

import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.widgets.wizard.*;
import com.untangle.uvm.security.*;
import com.untangle.uvm.node.*;
import com.untangle.node.openvpn.*;

public class ServerRoutingWizardGroupsJPanel extends MWizardPageJPanel {

    private static final String EXCEPTION_MINIMUM_COUNT = "You must create at least one group.";

    private VpnNode vpnNode;

    public ServerRoutingWizardGroupsJPanel(VpnNode vpnNode) {
        this.vpnNode = vpnNode;
        initComponents();
        ((MEditTableJPanel)configAddressGroupsJPanel).setShowDetailJPanelEnabled(false);
        ((MEditTableJPanel)configAddressGroupsJPanel).setInstantRemove(true);
        ((MEditTableJPanel)configAddressGroupsJPanel).setFillJButtonEnabled(false);
    }

    Vector<Vector> filteredDataVector;
    List<VpnGroup> elemList;
    Exception exception;

    public void doSave(Object settings, boolean validateOnly) throws Exception {

        SwingUtilities.invokeAndWait( new Runnable(){ public void run() {
            ((MEditTableJPanel)configAddressGroupsJPanel).getJTable().getCellEditor().stopCellEditing();
            ((MEditTableJPanel)configAddressGroupsJPanel).getJTable().clearSelection();
            filteredDataVector = ((MEditTableJPanel)configAddressGroupsJPanel).getTableModel().getFilteredDataVector();

            exception = null;

            if( filteredDataVector.size() < 1 ){
                exception = new Exception(EXCEPTION_MINIMUM_COUNT);
                return;
            }

            elemList = new ArrayList<VpnGroup>(filteredDataVector.size());
            VpnGroup newElem = null;
            int rowIndex = 0;

            for( Vector rowVector : filteredDataVector ){
                rowIndex++;
                newElem = new VpnGroup();
                newElem.setLive( (Boolean) rowVector.elementAt(2) );
                newElem.setUseDNS( (Boolean) rowVector.elementAt(3) );
                newElem.setName( (String) rowVector.elementAt(4) );
                try{ newElem.setAddress( IPaddr.parse( ((IPaddrString) rowVector.elementAt(5)).getString()) ); }
                catch(Exception e){ exception = new Exception("Invalid \"IP address\" in row: " + rowIndex); return; }
                try{ newElem.setNetmask( IPaddr.parse( ((IPaddrString) rowVector.elementAt(6)).getString()) ); }
                catch(Exception e){ exception = new Exception("Invalid \"netmask\" in row: " + rowIndex); return; }
                newElem.setDescription( (String) rowVector.elementAt(7) );
                elemList.add(newElem);
            }

        }});

        if( exception != null)
            throw exception;

        if( !validateOnly ){
            try{
                ServerRoutingWizard.getInfiniteProgressJComponent().startLater("Adding Address Pools...");
                GroupList groupList = new GroupList(elemList);
                vpnNode.setAddressGroups(groupList);
                ServerRoutingWizard.getInfiniteProgressJComponent().stopLater(1500l);
            }
            catch(Exception e){
                ServerRoutingWizard.getInfiniteProgressJComponent().stopLater(-1l);
                throw e;
            }
        }
    }


    private void initComponents() {//GEN-BEGIN:initComponents
        jLabel2 = new javax.swing.JLabel();
        configAddressGroupsJPanel = new ConfigAddressGroupsJPanel();
        jLabel3 = new javax.swing.JLabel();

        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        setOpaque(false);
        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("<html><b>Please add at least one Address Pool.</b><br>Connecting VPN clients and sites will be assigned IP addresses from these pools, and each pool can have a policy applied to it.</html>");
        add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 30, 410, -1));

        add(configAddressGroupsJPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(5, 90, 465, 210));

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/tran/openvpn/gui/ProductShot.png")));
        jLabel3.setEnabled(false);
        add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(-130, 230, -1, -1));

    }//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel configAddressGroupsJPanel;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    // End of variables declaration//GEN-END:variables

}
