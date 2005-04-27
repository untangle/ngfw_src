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
package com.metavize.tran.nat.gui;

import com.metavize.mvvm.tran.Transform;
import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.tran.*;
import com.metavize.mvvm.tran.firewall.*;
import com.metavize.tran.nat.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

public class RedirectJPanel extends MEditTableJPanel{

    private RedirectTableModel redirectTableModel;
            
    public RedirectJPanel() {
        super(true, true);
        super.setFillJButtonEnabled( true );
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");
        super.setAddRemoveEnabled(true);
        
        // create actual table model
        redirectTableModel = new RedirectTableModel();
        this.setTableModel( redirectTableModel );

    }
    
    public void refresh(Object settings) throws Exception {
        this.getJTable().getCellEditor().stopCellEditing();
        this.getJTable().clearSelection();
        redirectTableModel.refresh(settings);
    }
    
    public void save(Object settings) throws Exception {
        this.getJTable().getCellEditor().stopCellEditing();
        this.getJTable().clearSelection();
        redirectTableModel.save(settings);
    }
    



class RedirectTableModel extends MSortedTableModel{ 

    private Color INVALID_COLOR = Color.PINK;
    private Color BACKGROUND_COLOR = new Color(224, 224, 224);
    
    private static final int  T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int  C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int  C1_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int  C2_MW = 65;  /* redirect */
    private static final int  C3_MW = 100; /* protocol */
    private static final int  C4_MW = 160; /* direction */
    private static final int  C5_MW = 120;  /* source address */
    private static final int  C6_MW = 120;  /* destination address */
    private static final int  C7_MW = 80;  /* source port */
    private static final int  C8_MW = 80;  /* destination port */
    private static final int  C9_MW = 120;  /* redirect address */
    private static final int C10_MW = 65;  /* redirect port */
    private static final int C11_MW = 120;  /* category */
    
    private final int C12_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW + C5_MW + C6_MW + C7_MW + C8_MW + C9_MW + C10_MW + C11_MW), 120); /* description */


    private final StringConstants sc = StringConstants.getInstance();
    
    private ComboBoxModel protocolModel = super.generateComboBoxModel( ProtocolMatcher.getProtocolEnumeration(), ProtocolMatcher.getProtocolDefault() );
    private ComboBoxModel directionModel = super.generateComboBoxModel( TrafficRule.getDirectionEnumeration(), TrafficRule.getDirectionDefault() );
    
    RedirectTableModel(){
        super(null);
    }
    
    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #   min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0,  C0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1,  C1_MW, false, false, false, false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2,  C2_MW, false, true,  false, false, Boolean.class, "false", sc.bold("redirect") );
        addTableColumn( tableColumnModel,  3,  C3_MW, false, true,  false, false, ComboBoxModel.class, protocolModel, "protocol" );
        addTableColumn( tableColumnModel,  4,  C4_MW, false, true,  false, false, ComboBoxModel.class, directionModel, "direction" );
        addTableColumn( tableColumnModel,  5,  C5_MW, true,  true,  false, false, String.class, "1.2.3.4/5", sc.html("source<br>address") );
        addTableColumn( tableColumnModel,  6,  C6_MW, true,  true,  false, false, String.class, "1.2.3.4/5", sc.html("destination<br>address") );
        addTableColumn( tableColumnModel,  7,  C7_MW, true,  true,  false, false, String.class, "2-5", sc.html("source<br>port") );
        addTableColumn( tableColumnModel,  8,  C8_MW, true,  true,  false, false, String.class, "2-5", sc.html("destination<br>port") );
        addTableColumn( tableColumnModel,  9,  C9_MW, true,  true,  false, false, String.class, "1.2.3.4/5", sc.html("redirect<br>address") );
        addTableColumn( tableColumnModel, 10, C10_MW, true,  true,  false, false, String.class, "5", sc.html("redirect<br>port") );
        addTableColumn( tableColumnModel, 11, C11_MW, true,  true,  false, false, String.class, sc.EMPTY_CATEGORY, sc.TITLE_CATEGORY);
        addTableColumn( tableColumnModel, 12, C12_MW, true,  true,  false, true,  String.class, sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        return tableColumnModel;
    }
    
    public Vector generateRows(Object transformSettings){ return null; }
    public Object generateSettings(Vector dataVector){ return null; }
    
    public void refresh(Object settings) throws Exception {
        if(!(settings instanceof NatSettings)){
            RedirectJPanel.this.setBackground(INVALID_COLOR);
            return;
        }
        else{
            RedirectJPanel.this.setBackground(BACKGROUND_COLOR);
        }
    
        NatSettings natSettings = (NatSettings) settings;
        
        List<RedirectRule> redirectList = natSettings.getRedirectList();
        Vector allRowsVector = new Vector();
        Vector rowVector;
        int index = 1;
        for( RedirectRule redirectRule : redirectList ){
            try{
                rowVector = new Vector();
                rowVector.add(super.ROW_SAVED);
                rowVector.add(new Integer(index));
                rowVector.add(redirectRule.isLive());
                rowVector.add( super.generateComboBoxModel( ProtocolMatcher.getProtocolEnumeration(), redirectRule.getProtocol().toString() ));
                rowVector.add( super.generateComboBoxModel( TrafficRule.getDirectionEnumeration(), redirectRule.getDirection().toString() ));
                rowVector.add(redirectRule.getSrcAddress().toString());
                rowVector.add(redirectRule.getDstAddress().toString());
                rowVector.add(redirectRule.getSrcPort().toString());
                rowVector.add(redirectRule.getDstPort().toString());
                rowVector.add(redirectRule.getRedirectAddress().toString());
                rowVector.add( Integer.toString(redirectRule.getRedirectPort()) );      
                rowVector.add(redirectRule.getCategory());
                rowVector.add(redirectRule.getDescription());
                allRowsVector.add(rowVector);
                index++;
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error parsing for refresh", e);
            }
        }
        
        this.getDataVector().removeAllElements();
        this.getDataVector().addAll(allRowsVector);
        this.fireTableDataChanged();
    }
    
    public void save(Object settings) throws Exception {
        if(!(settings instanceof NatSettings)){
            RedirectJPanel.this.setBackground(INVALID_COLOR);
            return;
        }
        else{
            RedirectJPanel.this.setBackground(BACKGROUND_COLOR);
        }
        
        NatSettings natSettings = (NatSettings) settings;
        
        List redirectRulesList = new ArrayList();
        RedirectRule redirectRule;
        Vector<Vector> allRows = this.getDataVector();
        for( Vector rowVector : allRows ){
            try{
                if( ((String)rowVector.elementAt(0)).equals(this.ROW_REMOVE) )
                    continue;
                redirectRule = new RedirectRule();
                redirectRule.setLive( (Boolean) rowVector.elementAt(2) );
                redirectRule.setProtocol( ProtocolMatcher.parse(((ComboBoxModel) rowVector.elementAt(3)).getSelectedItem().toString()) );
                redirectRule.setDirection( ((ComboBoxModel) rowVector.elementAt(4)).getSelectedItem().toString() );
                redirectRule.setSrcAddress( IPMatcher.parse((String) rowVector.elementAt(5)) );
                redirectRule.setDstAddress( IPMatcher.parse((String) rowVector.elementAt(6)) );
                redirectRule.setSrcPort( PortMatcher.parse((String) rowVector.elementAt(7)) );
                redirectRule.setDstPort( PortMatcher.parse((String) rowVector.elementAt(8)) );
                redirectRule.setRedirectAddress( IPaddr.parse((String) rowVector.elementAt(9)) );
                redirectRule.setRedirectPort( Integer.parseInt((String) rowVector.elementAt(10)) );
                redirectRule.setCategory( (String) rowVector.elementAt(11) );
                redirectRule.setDescription( (String) rowVector.elementAt(12) );
                redirectRulesList.add(redirectRule);
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error parsing for save", e);
            }
        }
        
        natSettings.setRedirectList(redirectRulesList);
    }
    
    
}

}
