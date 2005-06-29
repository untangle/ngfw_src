/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: ProtoConfigJPanel.java 194 2005-04-06 19:13:55Z rbscott $
 */
package com.metavize.tran.firewall.gui;


import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.tran.*;
import com.metavize.mvvm.tran.firewall.*;
import com.metavize.tran.firewall.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

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
        this.setTableModel( blockTableModel );

    }
    



class BlockTableModel extends MSortedTableModel{ 
    
    private static final int  T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int  C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int  C1_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int  C2_MW = 65;  /* enable action */
    private static final int  C3_MW = 100; /* action */
    private static final int  C4_MW = 100; /* traffic type */
    private static final int  C5_MW = 160; /* direction */
    private static final int  C6_MW = 120;  /* source address */
    private static final int  C7_MW = 120;  /* destination address */
    private static final int  C8_MW = 110;  /* source port */
    private static final int  C9_MW = 110;  /* destination port */
    private static final int C10_MW = 120;  /* category */
    
    private final int C11_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW + C5_MW + C6_MW + C7_MW + C8_MW + C9_MW + C10_MW), 120); /* description */

    
    private ComboBoxModel protocolModel = super.generateComboBoxModel( ProtocolMatcher.getProtocolEnumeration(), ProtocolMatcher.getProtocolDefault() );
    private ComboBoxModel directionModel = super.generateComboBoxModel( TrafficRule.getDirectionEnumeration(), TrafficRule.getDirectionDefault() );
    private ComboBoxModel actionModel = super.generateComboBoxModel( FirewallRule.getActionEnumeration(), FirewallRule.getActionDefault() );
    
    
    protected boolean getSortable(){ return false; }
    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #   min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0,  C0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1,  C1_MW, false, false, false, false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2,  C2_MW, false, true,  false, false, Boolean.class, "false", sc.bold("enable<br>action") );
        addTableColumn( tableColumnModel,  3,  C3_MW, false, true,  false, false, ComboBoxModel.class, actionModel, sc.bold("action") );
        addTableColumn( tableColumnModel,  4,  C4_MW, false, true,  false, false, ComboBoxModel.class, protocolModel, sc.html("traffic<br>type") );
        addTableColumn( tableColumnModel,  5,  C5_MW, false, true,  false, false, ComboBoxModel.class, directionModel, "direction" );
        addTableColumn( tableColumnModel,  6,  C6_MW, true,  true,  false, false, String.class, "1.2.3.4", sc.html("source<br>address") );
        addTableColumn( tableColumnModel,  7,  C7_MW, true,  true,  false, false, String.class, "1.2.3.4", sc.html("destination<br>address") );
        addTableColumn( tableColumnModel,  8,  C8_MW, true,  true,  false, false, String.class, "2-5", sc.html("source<br>port") );
        addTableColumn( tableColumnModel,  9,  C9_MW, true,  true,  false, false, String.class, "2-5", sc.html("destination<br>port") );
        addTableColumn( tableColumnModel, 10, C10_MW, true,  true,  false, false, String.class, sc.EMPTY_CATEGORY, sc.TITLE_CATEGORY);
        addTableColumn( tableColumnModel, 11, C11_MW, true,  true,  false, true,  String.class, sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        return tableColumnModel;
    }
    
    
    public void generateSettings(Object settings, boolean validateOnly) throws Exception {        
        List firewallRulesList = new ArrayList();
        int rowIndex = 1;
        for( Vector rowVector : (Vector<Vector>) this.getDataVector() ){

            FirewallRule firewallRule = new FirewallRule();
            firewallRule.setLive( (Boolean) rowVector.elementAt(2) );
            firewallRule.setAction( ((ComboBoxModel) rowVector.elementAt(3)).getSelectedItem().toString() );
	    firewallRule.setProtocol( ProtocolMatcher.parse(((ComboBoxModel) rowVector.elementAt(4)).getSelectedItem().toString()) );
            firewallRule.setDirection( ((ComboBoxModel) rowVector.elementAt(5)).getSelectedItem().toString() );
            try{ firewallRule.setSrcAddress( IPMatcher.parse((String) rowVector.elementAt(6)) ); }
            catch(Exception e){ throw new Exception("Invalid \"source address\" in row: " + rowIndex); }
            try{ firewallRule.setDstAddress( IPMatcher.parse((String) rowVector.elementAt(7)) ); }
            catch(Exception e){ throw new Exception("Invalid \"destination address\" in row: " + rowIndex); }
            try{ firewallRule.setSrcPort( PortMatcher.parse((String) rowVector.elementAt(8)) ); }
            catch(Exception e){ throw new Exception("Invalid \"source port\" in row: " + rowIndex); }
            try{ firewallRule.setDstPort( PortMatcher.parse((String) rowVector.elementAt(9)) ); }
            catch(Exception e){ throw new Exception("Invalid \"destination port\" in row: " + rowIndex); }
            firewallRule.setCategory( (String) rowVector.elementAt(10) );
            firewallRule.setDescription( (String) rowVector.elementAt(11) );

            firewallRulesList.add(firewallRule);
            rowIndex++;
        }
        
	// SAVE SETTINGS //////////
	if( !validateOnly ){
	    FirewallSettings firewallSettings = (FirewallSettings) settings;
	    firewallSettings.setFirewallRuleList(firewallRulesList);
	}
    }

    

    public Vector generateRows(Object settings) {
        FirewallSettings firewallSettings = (FirewallSettings) settings;
        Vector allRowsVector = new Vector();
        int index = 1;
        for( FirewallRule firewallRule : (List<FirewallRule>) firewallSettings.getFirewallRuleList() ){

	    Vector rowVector = new Vector();	    
	    rowVector.add(super.ROW_SAVED);
	    rowVector.add(new Integer(index));
	    rowVector.add(firewallRule.isLive());
	    rowVector.add( super.generateComboBoxModel( FirewallRule.getActionEnumeration(), firewallRule.getAction().toString() ));
	    rowVector.add( super.generateComboBoxModel( ProtocolMatcher.getProtocolEnumeration(), firewallRule.getProtocol().toString() ));
	    rowVector.add( super.generateComboBoxModel( TrafficRule.getDirectionEnumeration(), firewallRule.getDirection().toString() ));
	    rowVector.add(firewallRule.getSrcAddress().toString());
	    rowVector.add(firewallRule.getDstAddress().toString());
	    rowVector.add(firewallRule.getSrcPort().toString());
	    rowVector.add(firewallRule.getDstPort().toString());  
	    rowVector.add(firewallRule.getCategory());
	    rowVector.add(firewallRule.getDescription());
	    allRowsVector.add(rowVector);
	    index++;
        }
        return allRowsVector;
    }
    
    
}

}
