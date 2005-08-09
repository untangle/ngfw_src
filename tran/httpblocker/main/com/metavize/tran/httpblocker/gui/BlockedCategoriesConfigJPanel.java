/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.httpblocker.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.tran.TransformContext;
import com.metavize.tran.httpblocker.*;


import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import javax.swing.event.*;

public class BlockedCategoriesConfigJPanel extends MEditTableJPanel {
    
    
    public BlockedCategoriesConfigJPanel() {
        super(true, true);
        super.setInsets(new java.awt.Insets(4, 4, 2, 2));
        super.setTableTitle("Blocked Categories");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(false);
        
        // create actual table model
        CategoryTableModel categoryTableModel = new CategoryTableModel();
        super.setTableModel( categoryTableModel );
    }
}


class CategoryTableModel extends MSortedTableModel
{ 
    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C2_MW = 195; /* category */
    private static final int C3_MW = 100; /* block domains */
    private static final int C4_MW = 120; /* description */
    private static final int C5_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW), 55); /* original name */

    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, true,  false, false, false, String.class,  null, sc.TITLE_CATEGORY);
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, Boolean.class, null, sc.bold("block"));
        addTableColumn( tableColumnModel,  4, C4_MW, true,  true,  false, true,  String.class,  null, sc.TITLE_DESCRIPTION);
        addTableColumn( tableColumnModel,  5, C5_MW, false, false, true,  false, String.class,  null, "original name");
        addTableColumn( tableColumnModel,  6, 10,    false, false, true,  false, BlacklistCategory.class, null, "");
        return tableColumnModel;
    }
    
    
    public void generateSettings(Object settings, boolean validateOnly) throws Exception {
        List elemList = new ArrayList();
	for( Vector rowVector : (Vector<Vector>) this.getDataVector() ){
            
            BlacklistCategory newElem = (BlacklistCategory) rowVector.elementAt(6);
            newElem.setDisplayName( (String) rowVector.elementAt(2) );
	    if( ((Boolean)rowVector.elementAt(3)).booleanValue() ){
		newElem.setBlockDomains( true );
		newElem.setBlockUrls( true );
		newElem.setBlockExpressions( true );
	    }
	    else{
		newElem.setBlockDomains( false );
		newElem.setBlockUrls( false );
		newElem.setBlockExpressions( false );
	    }
            newElem.setDescription( (String) rowVector.elementAt(4) );
            newElem.setName( (String) rowVector.elementAt(5) );
            elemList.add(newElem);
        }
        
	// SAVE SETTINGS /////////
	if( !validateOnly ){
	    HttpBlockerSettings httpBlockerSettings = (HttpBlockerSettings) settings;
	    httpBlockerSettings.setBlacklistCategories( elemList );
	}

    }
    
    public Vector generateRows(Object settings){
	HttpBlockerSettings httpBlockerSettings = (HttpBlockerSettings) settings;
        Vector allRows = new Vector();
        int counter = 1;

	for( BlacklistCategory newElem : (List<BlacklistCategory>) httpBlockerSettings.getBlacklistCategories() ){

            Vector row = new Vector();
            row.add(super.ROW_SAVED);
            row.add(new Integer(counter));
            row.add(newElem.getDisplayName());
            row.add(Boolean.valueOf(newElem.getBlockDomains()));
            row.add(newElem.getDescription());
            row.add(newElem.getName());
	    row.add(newElem);
            allRows.add(row);
            counter++;
        }
        return allRows;
    }
}
