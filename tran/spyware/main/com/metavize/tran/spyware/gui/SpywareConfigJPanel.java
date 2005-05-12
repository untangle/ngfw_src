/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.spyware.gui;

import com.metavize.mvvm.tran.TransformContext;

import com.metavize.mvvm.tran.IPMaddr;
import com.metavize.mvvm.tran.IPMaddrRule;
import com.metavize.tran.spyware.*;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import java.util.List;
import java.util.ArrayList;
import javax.swing.event.*;

public class SpywareConfigJPanel extends MEditTableJPanel{
    
    
    public SpywareConfigJPanel() {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("spyware sources");
        super.setDetailsTitle("source details");
        super.setAddRemoveEnabled(true);
        
        // create actual table model
        SpyTableModel spyTableModel = new SpyTableModel();
        this.setTableModel( spyTableModel );
    }
}


class SpyTableModel extends MSortedTableModel{ 

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C2_MW = 100; /* name */
    private static final int C3_MW = 150; /* subnet */
    private static final int C4_MW = 55; /* block */
    private static final int C5_MW = 55; /* log */
    private static final int C6_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW + C5_MW), 120); /* description */


    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min  rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS );
	addTableColumn( tableColumnModel,  1, C1_MW, false, false, false, false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class,  sc.EMPTY_NAME, sc.TITLE_NAME );
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, false, String.class,  "1.2.3.4/5", "subnet");
        addTableColumn( tableColumnModel,  4, C4_MW, false, true,  false, false, Boolean.class, "true", sc.bold("block"));
        addTableColumn( tableColumnModel,  5, C5_MW, false, true,  false, false, Boolean.class, "true", sc.bold("log"));
        addTableColumn( tableColumnModel,  6, C6_MW, true, true, false, true, String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        return tableColumnModel;
    }
    
    public void generateSettings(Object settings, boolean validateOnly) throws Exception{
        List elemList = new ArrayList();
	int rowIndex = 1;
	for( Vector rowVector : (Vector<Vector>) this.getDataVector() ){

            IPMaddrRule newElem = new IPMaddrRule();
            // newElem.setCategory( (String) rowVector.elementAt(2) );
            newElem.setName( (String) rowVector.elementAt(2) );
	    try{
		IPMaddr newIPMaddr = IPMaddr.parse( (String) rowVector.elementAt(3) );
		newElem.setIpMaddr( newIPMaddr );
	    }
            catch(Exception e){ throw new Exception("Invalid \"subnet\" specified at row: " + rowIndex); }
            newElem.setLive( ((Boolean) rowVector.elementAt(4)).booleanValue() );
            newElem.setLog( ((Boolean) rowVector.elementAt(5)).booleanValue() );
            newElem.setDescription( (String) rowVector.elementAt(6) );

            elemList.add(newElem);
	    rowIndex++;
        }

	// SAVE SETTINGS /////////
	if( !validateOnly ){
	    SpywareSettings spywareSettings = (SpywareSettings) settings;
	    spywareSettings.setSubnetRules(elemList);
	}

    }
    
    public Vector generateRows(Object settings){
	SpywareSettings spywareSettings = (SpywareSettings) settings;
        Vector allRows = new Vector();
	int count = 1;
	for( IPMaddrRule newElem : (List<IPMaddrRule>) spywareSettings.getSubnetRules() ){

            Vector row = new Vector();
            row.add(super.ROW_SAVED);
            row.add(new Integer(count));
            // row.add(newElem.getCategory());
            row.add(newElem.getName());
            row.add(newElem.getIpMaddr().toString());
            row.add(Boolean.valueOf( newElem.isLive()));
            row.add(Boolean.valueOf( newElem.getLog()));
            row.add(newElem.getDescription());
            
            allRows.add(row);
	    count++;
        }
        return allRows;
    } 
}
