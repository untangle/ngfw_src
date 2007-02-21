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
package com.untangle.tran.firewall.gui;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.pipeline.MPipelineJPanel;
import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.mvvm.tran.*;
import com.untangle.mvvm.tran.firewall.*;
import com.untangle.mvvm.tran.firewall.ip.IPMatcherFactory;
import com.untangle.mvvm.tran.firewall.port.PortMatcherFactory;
import com.untangle.mvvm.tran.firewall.protocol.ProtocolMatcherFactory;
import com.untangle.tran.firewall.*;

public class BlockJPanel extends MEditTableJPanel{

    public BlockJPanel() {
        super(true, true);
        super.setFillJButtonEnabled( true );
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");
        super.setAddRemoveEnabled(true);

        // create actual table model
        BlockTableModel blockTableModel = new BlockTableModel();
        blockTableModel.setOrderModelIndex(0);
        this.setTableModel( blockTableModel );
        
    }




class BlockTableModel extends MSortedTableModel<Object>{

    private static final int  T_TW = Util.TABLE_TOTAL_WIDTH_LARGE;
    private static final int  C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int  C1_MW = Util.LINENO_EDIT_MIN_WIDTH; /* # */
    private static final int  C2_MW = 65;  /* enable action */
    private static final int  C3_MW = 75; /* action */
    private static final int  C4_MW = 55;  /* log */
    private static final int  C5_MW = 100; /* traffic type */
    private static final int  C6_MW = 160; /* direction */
    private static final int  C7_MW = 120;  /* source address */
    private static final int  C8_MW = 120;  /* destination address */
    private static final int  C9_MW = 110;  /* source port */
    private static final int  C10_MW = 110;  /* destination port */
    private static final int  C11_MW = 120;  /* category */

    private final int C12_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW + C5_MW + C6_MW + C7_MW + C8_MW + C9_MW + C10_MW + C11_MW), 120); /* description */


    private ComboBoxModel protocolModel = super.generateComboBoxModel( ProtocolMatcherFactory.getProtocolEnumeration(), ProtocolMatcherFactory.getProtocolDefault() );
    private ComboBoxModel directionModel = super.generateComboBoxModel( TrafficDirectionRule.getDirectionEnumeration(), TrafficDirectionRule.getDirectionDefault() );
    private ComboBoxModel actionModel = super.generateComboBoxModel( FirewallRule.getActionEnumeration(), FirewallRule.getActionDefault() );


    protected boolean getSortable(){ return false; }

    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #   min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0,  C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1,  C1_MW, false, true,  false, false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2,  C2_MW, false, true,  false, false, Boolean.class, "true", sc.bold("enable<br>rule") );
        addTableColumn( tableColumnModel,  3,  C3_MW, false, true,  false, false, ComboBoxModel.class, actionModel, sc.bold("action") );
        addTableColumn( tableColumnModel,  4,  C4_MW, false, true,  false, false, Boolean.class, "false", sc.bold("log") );
        addTableColumn( tableColumnModel,  5,  C5_MW, false, true,  false, false, ComboBoxModel.class, protocolModel, sc.html("traffic<br>type") );
        addTableColumn( tableColumnModel,  6,  C6_MW, false, true,  false, false, ComboBoxModel.class, directionModel, "direction" );
        addTableColumn( tableColumnModel,  7,  C7_MW, true,  true,  false, false, String.class, "1.2.3.4", sc.html("source<br>address") );
        addTableColumn( tableColumnModel,  8,  C8_MW, true,  true,  false, false, String.class, "1.2.3.4", sc.html("destination<br>address") );
        addTableColumn( tableColumnModel,  9,  C9_MW, true,  true,  false, false, String.class, "any", sc.html("source<br>port") );
        addTableColumn( tableColumnModel, 10, C10_MW, true,  true,  false, false, String.class, "2-5", sc.html("destination<br>port") );
        addTableColumn( tableColumnModel, 11, C11_MW, true,  true,  false, false, String.class, sc.EMPTY_CATEGORY, sc.TITLE_CATEGORY);
        addTableColumn( tableColumnModel, 12, C12_MW, true,  true,  false, true,  String.class, sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        addTableColumn( tableColumnModel, 13, 10,     false, false, true,  false, FirewallRule.class, null, "");
        return tableColumnModel;
    }


    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
        List elemList = new ArrayList(tableVector.size());
    FirewallRule newElem = null;
        int rowIndex = 0;

        for( Vector rowVector : tableVector ){
        rowIndex++;
            newElem = (FirewallRule) rowVector.elementAt(13);
            newElem.setLive( (Boolean) rowVector.elementAt(2) );
            newElem.setAction( ((ComboBoxModel) rowVector.elementAt(3)).getSelectedItem().toString() );
            newElem.setLog( (Boolean) rowVector.elementAt(4) );
        newElem.setProtocol( ProtocolMatcherFactory.parse(((ComboBoxModel) rowVector.elementAt(5)).getSelectedItem().toString()) );
            newElem.setDirection( ((ComboBoxModel) rowVector.elementAt(6)).getSelectedItem().toString() );
            try{ newElem.setSrcAddress( IPMatcherFactory.parse((String) rowVector.elementAt(7)) ); }
            catch(Exception e){ throw new Exception("Invalid \"source address\" in row: " + rowIndex); }
            try{ newElem.setDstAddress( IPMatcherFactory.parse((String) rowVector.elementAt(8)) ); }
            catch(Exception e){ throw new Exception("Invalid \"destination address\" in row: " + rowIndex); }
            try{ newElem.setSrcPort( PortMatcherFactory.parse((String) rowVector.elementAt(9)) ); }
            catch(Exception e){ throw new Exception("Invalid \"source port\" in row: " + rowIndex); }
            try{ newElem.setDstPort( PortMatcherFactory.parse((String) rowVector.elementAt(10)) ); }
            catch(Exception e){ throw new Exception("Invalid \"destination port\" in row: " + rowIndex); }
            newElem.setCategory( (String) rowVector.elementAt(11) );
            newElem.setDescription( (String) rowVector.elementAt(12) );
            elemList.add(newElem);
        }

    // SAVE SETTINGS //////////
    if( !validateOnly ){
        FirewallSettings firewallSettings = (FirewallSettings) settings;
        firewallSettings.setFirewallRuleList( elemList );
    }
    }



    public Vector<Vector> generateRows(Object settings) {
        FirewallSettings firewallSettings = (FirewallSettings) settings;
    List<FirewallRule> firewallRuleList = (List<FirewallRule>) firewallSettings.getFirewallRuleList();
        Vector<Vector> allRows = new Vector<Vector>(firewallRuleList.size());
    Vector tempRow = null;
        int rowIndex = 0;

        for( FirewallRule firewallRule : firewallRuleList ){
        rowIndex++;
        tempRow = new Vector(14);
        tempRow.add( super.ROW_SAVED );
        tempRow.add( rowIndex );
        tempRow.add( firewallRule.isLive() );
        tempRow.add( super.generateComboBoxModel( FirewallRule.getActionEnumeration(), firewallRule.getAction().toString() ) );
        tempRow.add( firewallRule.getLog() );
        tempRow.add( super.generateComboBoxModel( ProtocolMatcherFactory.getProtocolEnumeration(), firewallRule.getProtocol().toString() ) );
        tempRow.add( super.generateComboBoxModel( TrafficDirectionRule.getDirectionEnumeration(), firewallRule.getDirection().toString() ) );
        tempRow.add( firewallRule.getSrcAddress().toString() );
        tempRow.add( firewallRule.getDstAddress().toString() );
        tempRow.add( firewallRule.getSrcPort().toString() );
        tempRow.add( firewallRule.getDstPort().toString() );
        tempRow.add( firewallRule.getCategory() );
        tempRow.add( firewallRule.getDescription() );
        tempRow.add( firewallRule );
        allRows.add( tempRow );
        }
        return allRows;
    }


}

}
