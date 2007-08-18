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


public class TableModelSiteToSite extends MSortedTableModel<Object>{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH_LARGE;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C2_MW = 65;  /* enabled */
    private static final int C3_MW = 85;  /* isUntanglePlatform */
    private static final int C4_MW = 150; /* name */
    private static final int C5_MW = 150; /* group */
    private static final int C6_MW = 120; /* network address */
    private static final int C7_MW = 120; /* netmask */
    private static final int C8_MW = 135; /* action */
    private static final int C9_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW + C5_MW + C6_MW + C7_MW  + C8_MW), 120); /* description */

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
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, Boolean.class, "true", sc.html("is Untangle<br>Server"));
        addTableColumn( tableColumnModel,  4, C4_MW, true,  true,  false, false, String.class,  sc.EMPTY_NAME, sc.html("site name") );
        addTableColumn( tableColumnModel,  5, C5_MW, true,  true,  false, false, ComboBoxModel.class,  groupModel, sc.html("address<br>pool"));
        addTableColumn( tableColumnModel,  6, C6_MW, false, true,  false, false, IPaddrString.class,  "1.2.3.4", sc.html("network<br>address"));
        addTableColumn( tableColumnModel,  7, C7_MW, false, true,  false, false, IPaddrString.class,  "255.255.255.0", sc.html("network<br>netmask"));
        addTableColumn( tableColumnModel,  8, C8_MW, false, true,  false, false, KeyButtonRunnable.class,  "false", sc.html("Secure Key<br>Distribution"));
        addTableColumn( tableColumnModel,  9, C9_MW, true,  true,  true, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        addTableColumn( tableColumnModel,  10, 10,   false, false, true,  false, VpnSite.class, null, "");
        return tableColumnModel;
    }

    public void handleDependencies(int modelCol, int modelRow){
        Vector rowVector = (Vector) getDataVector().elementAt(modelRow);
        String rowState = (String) rowVector.elementAt( getStateModelIndex() );
        KeyButtonRunnable keyButtonRunnable = (KeyButtonRunnable) rowVector.elementAt(8);
        if( !rowState.equals(ROW_SAVED) ){
            keyButtonRunnable.setEnabled(false);
        }
        else{
            keyButtonRunnable.setEnabled(true);
        }
        super.handleDependencies(modelCol,modelRow);
    }

    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
        List elemList = new ArrayList(tableVector.size());
        VpnSite newElem = null;
        int rowIndex = 0;

        for( Vector rowVector : tableVector ){
            rowIndex++;
            newElem = (VpnSite) rowVector.elementAt(10);
            newElem.setLive( (Boolean) rowVector.elementAt(2) );
            newElem.setUntanglePlatform( (Boolean) rowVector.elementAt(3) );
            newElem.setName( (String) rowVector.elementAt(4) );
            newElem.setGroup( (VpnGroup) ((ComboBoxModel) rowVector.elementAt(5)).getSelectedItem() );
            IPaddr network;
            try{ network = IPaddr.parse( ((IPaddrString) rowVector.elementAt(6)).getString() ); }
            catch(Exception e){ throw new Exception("Invalid \"network address\" in row: " + rowIndex);  }
            IPaddr netmask;
            try{ netmask = IPaddr.parse( ((IPaddrString) rowVector.elementAt(7)).getString() ); }
            catch(Exception e){ throw new Exception("Invalid \"network netmask\" in row: " + rowIndex);  }
            newElem.setSiteNetwork(network, netmask);
            newElem.setDescription( (String) rowVector.elementAt(9) );
            elemList.add(newElem);
        }

        // SAVE SETTINGS ////////
        if( !validateOnly ){
            VpnSettings vpnSettings = (VpnSettings) settings;
            vpnSettings.setSiteList(elemList);
        }

    }

    public Vector<Vector> generateRows(Object settings) {
        VpnSettings vpnSettings = (VpnSettings) settings;
        List<VpnSite> vpnSites = (List<VpnSite>) vpnSettings.getSiteList();
        Vector<Vector> allRows = new Vector<Vector>(vpnSites.size());
        Vector tempRow = null;
        int rowIndex = 0;

        updateGroupModel( (List<VpnGroup>) vpnSettings.getGroupList() );

        for( VpnSite vpnSite : vpnSites ){
            rowIndex++;
            tempRow = new Vector(10);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( vpnSite.isLive() );
            tempRow.add( vpnSite.isUntanglePlatform() );
            tempRow.add( vpnSite.getName() );
            ComboBoxModel groupComboBoxModel = super.copyComboBoxModel(groupModel);
            groupComboBoxModel.setSelectedItem( vpnSite.getGroup() );
            tempRow.add( groupComboBoxModel );
            ClientSiteNetwork siteNetwork = vpnSite.getSiteNetwork();
            tempRow.add( new IPaddrString( siteNetwork.getNetwork()));
            tempRow.add( new IPaddrString( siteNetwork.getNetmask()));
            KeyButtonRunnable keyButtonRunnable = new KeyButtonRunnable("true");
            keyButtonRunnable.setVpnClient( vpnSite );
            tempRow.add( keyButtonRunnable );
            tempRow.add( vpnSite.getDescription() );
            tempRow.add( vpnSite );
            allRows.add(tempRow);
        }

        return allRows;
    }
}
