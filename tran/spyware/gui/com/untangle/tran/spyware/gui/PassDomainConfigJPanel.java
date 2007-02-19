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



package com.untangle.tran.spyware.gui;

import com.untangle.mvvm.tran.TransformContext;

import com.untangle.mvvm.tran.StringRule;
import com.untangle.tran.spyware.*;

import com.untangle.gui.transform.*;
import com.untangle.gui.pipeline.MPipelineJPanel;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.util.*;

//import java.awt.*;
//import javax.swing.*;

import java.net.URL;
import java.awt.Insets;
import javax.swing.table.*;
import java.util.*;
//import javax.swing.event.*;

public class PassDomainConfigJPanel extends MEditTableJPanel{
    
    
    public PassDomainConfigJPanel() {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("spyware sources");
        super.setDetailsTitle("source details");
        super.setAddRemoveEnabled(true);
        
        // create actual table model
        PassDomainTableModel passDomainTableModel = new PassDomainTableModel();
        this.setTableModel( passDomainTableModel );
    }
}


class PassDomainTableModel extends MSortedTableModel<Object>{ 

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C2_MW = 55;  /* pass */
    private static final int C3_MW = 100; /* category */
    private static final int C4_MW = 200; /* domain */
    private static final int C5_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW), 120); /* description */


    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS );
	addTableColumn( tableColumnModel,  1, C1_MW, false, false, true, false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, false, true,  false, false, Boolean.class, "true", sc.bold("pass"));
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  true, false, String.class,  sc.EMPTY_CATEGORY, "category");
        addTableColumn( tableColumnModel,  4, C4_MW, true,  true,  false, false, String.class,  "", "domain");
        addTableColumn( tableColumnModel,  5, C5_MW, true,  true,  true, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        addTableColumn( tableColumnModel,  6, 10,    false, false, true,  false, StringRule.class, null, "");
        return tableColumnModel;
    }
    
    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception{
        List elemList = new ArrayList(tableVector.size());
	StringRule newElem = null;
	int rowIndex = 0;

	for( Vector rowVector : tableVector ){
	    rowIndex++;
            newElem = (StringRule) rowVector.elementAt(6);
            newElem.setLive( (Boolean) rowVector.elementAt(2) );
            newElem.setCategory( (String) rowVector.elementAt(3) );
            String newURL = (String) rowVector.elementAt(4);
            if( newURL.startsWith("https://") )
                throw new Exception("https \"URL\" specified at row: " + rowIndex + "cannot be passed.");
            if( newURL.startsWith("http://") )
                newURL = newURL.substring(7,newURL.length());
            if( newURL.startsWith("www.") )                
                newURL = newURL.substring(4, newURL.length());
            if( newURL.indexOf("/") >= 0 )
                newURL = newURL.substring(0, newURL.indexOf("/"));
            if( newURL.trim().length() == 0 )
                throw new Exception("Invalid \"domain\" specified at row: " + rowIndex); 
            newElem.setString( newURL  );
            newElem.setDescription( (String) rowVector.elementAt(5) );

            elemList.add(newElem);
        }

	// SAVE SETTINGS /////////
	if( !validateOnly ){
	    SpywareSettings spywareSettings = (SpywareSettings) settings;
	    spywareSettings.setDomainWhitelist( elemList );
	}

    }
    
    public Vector<Vector> generateRows(Object settings){
	SpywareSettings spywareSettings = (SpywareSettings) settings;
	List<StringRule> domainWhitelist = (List<StringRule>) spywareSettings.getDomainWhitelist();
        Vector<Vector> allRows = new Vector<Vector>(domainWhitelist.size());
	Vector tempRow = null;
	int rowIndex = 0;

	for( StringRule newElem : domainWhitelist ){
	    rowIndex++;
            tempRow = new Vector(7);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( newElem.isLive() );
            tempRow.add( newElem.getCategory() );
            tempRow.add( newElem.getString() );
            tempRow.add( newElem.getDescription() );
            tempRow.add( newElem );
            allRows.add( tempRow );
        }
        return allRows;
    } 
}
