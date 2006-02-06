/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: DnsAddressJPanel.java 194 2005-04-06 19:13:55Z rbscott $
 */

package com.metavize.tran.nat.gui;


import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.tran.*;
import com.metavize.mvvm.tran.firewall.*;
import com.metavize.tran.nat.*;


import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

public class DnsAddressJPanel extends MEditTableJPanel{

            
    public DnsAddressJPanel() {
        super(true, true);
        super.setFillJButtonEnabled( false );
        super.setInsets(new java.awt.Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");
        super.setAddRemoveEnabled(true);
        
        // create actual table model
        DnsAddressTableModel dnsAddressTableModel = new DnsAddressTableModel();
        this.setTableModel( dnsAddressTableModel );

    }
    
    



class DnsAddressTableModel extends MSortedTableModel{ 

    
    private static final int  T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int  C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int  C1_MW = Util.LINENO_MIN_WIDTH; /* line number */
    private static final int  C2_MW = 130;  /* host name */
    private static final int  C3_MW = 130;  /* IP address */
    private static final int  C4_MW = 120;  /* category */    
    private final int C5_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW), 120); /* description */

        
    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #   min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0,  C0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1,  C1_MW, false, false, false, false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2,  C2_MW, true,  true,  false, false, String.class, sc.empty("no hostname"), sc.html("translate this<br><b>hostname</b>") );
        addTableColumn( tableColumnModel,  3,  C3_MW, true,  true,  false, false, String.class, "1.2.3.4", sc.html("into this<br><b>IP address</b>") );
        addTableColumn( tableColumnModel,  4,  C4_MW, true,  true,  false, false, String.class, sc.EMPTY_CATEGORY, sc.TITLE_CATEGORY);
        addTableColumn( tableColumnModel,  5,  C5_MW, true,  true,  false, true,  String.class, sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        addTableColumn( tableColumnModel,  6,  10,    false, false, true,  false, DnsStaticHostRule.class, null, "");
        return tableColumnModel;
    }
    
    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
        NatSettingsWrapper natSettings = (NatSettingsWrapper) settings;        
        List elemList = new ArrayList(tableVector.size());
	DnsStaticHostRule newElem = null;
        int rowIndex = 0;

        for( Vector rowVector : tableVector ){
	    rowIndex++;
            newElem = (DnsStaticHostRule) rowVector.elementAt(6);
            try{ newElem.setHostNameList( HostNameList.parse( (String)rowVector.elementAt(2)) ); }
            catch(Exception e){ throw new Exception("Invalid \"hostname\" in row: " + rowIndex); }
            try{ newElem.setStaticAddress( IPaddr.parse( (String)rowVector.elementAt(3)) ); }
            catch(Exception e){ throw new Exception("Invalid \"IP address\" in row: " + rowIndex); }
            newElem.setCategory( (String) rowVector.elementAt(4) );
            newElem.setDescription( (String) rowVector.elementAt(5) );
            elemList.add(newElem);
        }
        
	// SAVE SETTINGS ////////
	if( !validateOnly ){
	    natSettings.setDnsStaticHostList( elemList );
	}
    }
    
    
    public Vector<Vector> generateRows(Object settings) {    
        NatSettingsWrapper natSettings = (NatSettingsWrapper) settings;
	List<DnsStaticHostRule> dnsStaticHostList = (List<DnsStaticHostRule>) natSettings.getDnsStaticHostList();
        Vector<Vector> allRows = new Vector<Vector>(dnsStaticHostList.size());
	Vector tempRow = null;
	int rowIndex = 0;

        for( DnsStaticHostRule hostRule : dnsStaticHostList ){
	    rowIndex++;
	    tempRow = new Vector(7);
	    tempRow.add( super.ROW_SAVED );
	    tempRow.add( rowIndex );
	    tempRow.add( hostRule.getHostNameList().toString() );
	    tempRow.add( hostRule.getStaticAddress().toString() );
	    tempRow.add( hostRule.getCategory() );
	    tempRow.add( hostRule.getDescription() );
	    tempRow.add( hostRule );
	    allRows.add( tempRow);
        }
        return allRows;
    }
    
}

}
