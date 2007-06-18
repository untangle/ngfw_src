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
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.node.*;
import com.untangle.uvm.node.firewall.*;
import com.untangle.node.openvpn.*;


public class TableModelClientToSite extends MSortedTableModel<Object>{

    private static final String EXCEPTION_CANNOT_CHANGE_NAME = "You cannot change an account name after its key has been distributed.";

    private static final String UNSET_STRING = "unassigned";


    private static final int T_TW = Util.TABLE_TOTAL_WIDTH_LARGE;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C2_MW = 65; /* enabled */
    private static final int C3_MW = 150;  /* name */
    private static final int C4_MW = 150; /* group */
    private static final int C5_MW = 150; /* action */
    private static final int C6_MW = 150; /* virtual address */
    private static final int C7_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW + C5_MW + C6_MW), 120); /* description */

    private DefaultComboBoxModel groupModel = new DefaultComboBoxModel();

    public void updateGroupModel(List<VpnGroup> vpnGroups){
        groupModel.removeAllElements();
        for( VpnGroup vpnGroup : vpnGroups )
            groupModel.addElement(vpnGroup);
    }

    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true, false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, false, true,  false, false, Boolean.class, "true", sc.bold("enabled"));
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, false, String.class,  sc.EMPTY_NAME, sc.html("client name") );
        addTableColumn( tableColumnModel,  4, C4_MW, true,  true,  false, false, ComboBoxModel.class,  groupModel, sc.html("address<br>pool"));
        addTableColumn( tableColumnModel,  5, C5_MW, false, true,  false, false, KeyButtonRunnable.class,  "false", sc.html("secure key<br>distribution"));
        addTableColumn( tableColumnModel,  6, C6_MW, false, false, false, false, IPaddrString.class, UNSET_STRING, sc.html("virtual address"));
        addTableColumn( tableColumnModel,  7, C7_MW, true,  true,  true, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        addTableColumn( tableColumnModel,  8, 10,    false, false, true,  false, VpnClient.class, null, "");
        return tableColumnModel;
    }

    public void handleDependencies(int viewCol, int viewRow){
        Vector rowVector = (Vector) getDataVector().elementAt(viewRow);
        String rowState = (String) rowVector.elementAt( getStateModelIndex() );
        KeyButtonRunnable keyButtonRunnable = (KeyButtonRunnable) rowVector.elementAt(5);
        if( !rowState.equals(ROW_SAVED) ){
            keyButtonRunnable.setEnabled(false);
        }
        else{
            keyButtonRunnable.setEnabled(true);
        }
        super.handleDependencies(viewCol,viewRow);
    }

    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
        List elemList = new ArrayList(tableVector.size());
        VpnClient newElem = null;
        int rowIndex = 0;

        for( Vector rowVector : tableVector ){
            rowIndex++;
            newElem = (VpnClient) rowVector.elementAt(8);
            newElem.setLive( (Boolean) rowVector.elementAt(2) );

            String rowState = (String) rowVector.elementAt( getStateModelIndex() );
            String proposedName = (String) rowVector.elementAt(3);
            if( rowState.equals(ROW_CHANGED) && !proposedName.equals(newElem.getName()) ){
                throw new Exception(EXCEPTION_CANNOT_CHANGE_NAME);
            }
            newElem.setName( (String) rowVector.elementAt(3) );

            newElem.setGroup( (VpnGroup) ((ComboBoxModel) rowVector.elementAt(4)).getSelectedItem() );
            /* rbs: do not modify the client address */
            newElem.setDescription( (String) rowVector.elementAt(7) );
            elemList.add(newElem);
        }

        // SAVE SETTINGS ////////
        if( !validateOnly ){
            VpnSettings vpnSettings = (VpnSettings) settings;
            vpnSettings.setClientList(elemList);
        }

    }

    public Vector<Vector> generateRows(Object settings) {
        VpnSettings vpnSettings = (VpnSettings) settings;
        List<VpnClient> vpnClients = (List<VpnClient>) vpnSettings.getClientList();
        Vector<Vector> allRows = new Vector<Vector>(vpnClients.size());
        Vector tempRow = null;
        int rowIndex = 0;

        updateGroupModel( (List<VpnGroup>) vpnSettings.getGroupList() );

        for( VpnClient vpnClient : vpnClients ){
            rowIndex++;
            tempRow = new Vector(8);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( vpnClient.isLive() );
            tempRow.add( vpnClient.getName() );
            ComboBoxModel groupComboBoxModel = super.copyComboBoxModel(groupModel);
            groupComboBoxModel.setSelectedItem( vpnClient.getGroup() );
            tempRow.add( groupComboBoxModel );
            KeyButtonRunnable keyButtonRunnable = new KeyButtonRunnable("true");
            keyButtonRunnable.setVpnClient( vpnClient );
            tempRow.add( keyButtonRunnable  );
            tempRow.add( getClientAddress( vpnClient ));
            tempRow.add( vpnClient.getDescription() );
            tempRow.add( vpnClient );
            allRows.add(tempRow);
        }

        return allRows;
    }

    IPaddrString getClientAddress( VpnClient vpnClient )
    {
        IPaddr clientAddress = vpnClient.getAddress();
        IPaddrString ipaddrString = new IPaddrString( UNSET_STRING );
        if ( clientAddress != null && !clientAddress.isEmpty()) {
            ipaddrString = new IPaddrString( clientAddress );
        }

        return ipaddrString;
    }
}

