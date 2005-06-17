/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.spyware.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;

import com.metavize.tran.spyware.*;

import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import javax.swing.event.*;

public class GeneralConfigJPanel extends MEditTableJPanel {
    
    
    public GeneralConfigJPanel() {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("General Settings");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(false);
        
        // create actual table model
        GeneralTableModel tableModel = new GeneralTableModel();
        this.setTableModel( tableModel );
    }
}



class GeneralTableModel extends MSortedTableModel{ 

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C2_MW = 200; /* setting name */
    private static final int C3_MW = 200; /* setting value */
    private static final int C4_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW), 120); /* description */

    private static final StringConstants sc = StringConstants.getInstance();

    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, true,  false, false, false, String.class,  null, "setting name");
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, false, Object.class,  null, sc.bold("setting value"));
        addTableColumn( tableColumnModel,  4, C4_MW, true,  false, true,  true, String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        return tableColumnModel;
    }
    
    
    public void generateSettings(Object settings, boolean validateOnly) throws Exception{

        Vector tempRowVector;
                
        // blockAllActiveX
        tempRowVector = (Vector) dataVector.elementAt(0);

	// SAVE SETTINGS ////////////
	if( !validateOnly ){
	    SpywareSettings spywareSettings = (SpywareSettings) settings;
	    spywareSettings.setBlockAllActiveX( ((Boolean)tempRowVector.elementAt(3)).booleanValue() );
	    spywareSettings.setBlockAllActiveXDetails( (String) tempRowVector.elementAt(4) );
        }
      
    }
    
    public Vector generateRows(Object settings){
        SpywareSettings spywareSettings = (SpywareSettings) settings;

        Vector allRows = new Vector(1);
        Vector tempRowVector;
                       
        // blockAllActiveX
        tempRowVector = new Vector(5);
        tempRowVector.add(super.ROW_SAVED);
        tempRowVector.add(new Integer(1));
        tempRowVector.add("block all ActiveX");
        tempRowVector.add( Boolean.valueOf( spywareSettings.getBlockAllActiveX()));
        tempRowVector.add( "This settings allows you to block ActiveX from being transferred, regardless of if the ActiveX is known to the ActiveX Block List or not." );//spywareSettings.getBlockAllActiveXDetails());
        allRows.add( tempRowVector );
        
        return allRows;
    }
}
