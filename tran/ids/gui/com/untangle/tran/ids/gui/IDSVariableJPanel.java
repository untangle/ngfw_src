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



package com.untangle.tran.ids.gui;

import com.untangle.gui.transform.*;
import com.untangle.gui.pipeline.MPipelineJPanel;
import com.untangle.mvvm.tran.TransformContext;

import com.untangle.tran.ids.*;

import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.util.*;

import java.awt.Insets;
//import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
//import javax.swing.event.*;

public class IDSVariableJPanel extends MEditTableJPanel {
        
    public IDSVariableJPanel() {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("General Settings");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(true);
        
        // create actual table model
        IDSVariableTableModel tableModel = new IDSVariableTableModel();
        this.setTableModel( tableModel );
    }
}



class IDSVariableTableModel extends MSortedTableModel<Object>{ 

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C2_MW = 160; /* variable name */
    private static final int C3_MW = 160; /* variable value */
    private static final int C4_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW), 120); /* description */

    private static final StringConstants sc = StringConstants.getInstance();

    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class,  "$SOME_VARIABLE", "variable name");
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, false, String.class,  "some_value", sc.bold("variable value"));
        addTableColumn( tableColumnModel,  4, C4_MW, true,  true,  false, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        addTableColumn( tableColumnModel,  5, 10,    false, false, true,  false, IDSVariable.class,  null, "");
        return tableColumnModel;
    }
    
    
    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception{
        List elemList = new ArrayList(tableVector.size());
	IDSVariable newElem = null;

	for( Vector rowVector : tableVector ){
	    newElem = (IDSVariable) rowVector.elementAt(5);
	    newElem.setVariable( (String) rowVector.elementAt(2) );
	    newElem.setDefinition( (String) rowVector.elementAt(3) );
	    newElem.setDescription( (String) rowVector.elementAt(4) );
	    elemList.add(newElem);
	}

	// SAVE SETTINGS ////////////
	if( !validateOnly ){
	    IDSSettings idsSettings = (IDSSettings) settings;
	    idsSettings.setVariables( elemList );
        }
      
    }
    
    public Vector<Vector> generateRows(Object settings){
	IDSSettings idsSettings = (IDSSettings) settings;
	List<IDSVariable> variables = (List<IDSVariable>) idsSettings.getVariables();
        Vector<Vector> allRows = new Vector<Vector>(variables.size());
	Vector tempRow = null;
	int rowIndex = 0;

	for( IDSVariable idsVariable : variables ){
	    rowIndex++;
	    tempRow = new Vector(6);
	    tempRow.add( super.ROW_SAVED );
	    tempRow.add( rowIndex );
	    tempRow.add( idsVariable.getVariable() );
	    tempRow.add( idsVariable.getDefinition() );
	    tempRow.add( idsVariable.getDescription() );
	    tempRow.add( idsVariable );
	    allRows.add( tempRow );
	}
        
        return allRows;
    }
}
