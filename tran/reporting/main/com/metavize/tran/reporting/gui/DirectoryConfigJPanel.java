/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.reporting.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.*;

import com.metavize.tran.reporting.*;

import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import javax.swing.event.*;

public class DirectoryConfigJPanel extends MEditTableJPanel {
    
    
    public DirectoryConfigJPanel(TransformContext transformContext) {

        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("IP Address <==> User Directory");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(true);

        // create actual table model
        DirectoryTableModel directoryTableModel = new DirectoryTableModel(transformContext);
        this.setTableModel( directoryTableModel );
    }
}


class DirectoryTableModel extends MSortedTableModel{ 

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C1_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C2_MW = 150; /* user name */
    private static final int C3_MW = 150; /* IPMaddr */
    private static final int C4_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW), 120); /* description */

    private static final StringConstants sc = StringConstants.getInstance();


    
    DirectoryTableModel(TransformContext transformContext){
        super(transformContext);
	refresh();
    }
    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false,  true, false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, false, false, String.class,  null, "status");
        addTableColumn( tableColumnModel,  2, C3_MW, true,   true, false, false, String.class,  "0.0.0.0/32", "IP Address");
	addTableColumn( tableColumnModel,  3, C2_MW, true,   true, false, false, String.class,  "no name", "user name");
	addTableColumn( tableColumnModel,  4, C4_MW, true,   true, false,  true, String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        return tableColumnModel;
    }
    
    
    
    public Object generateSettings(Vector dataVector){
        Vector rowVector;

	IPMaddrRule newElem;
        java.util.List elemList = new LinkedList();
        for(int i=0; i<dataVector.size(); i++){
	    try{
		rowVector = (Vector) dataVector.elementAt(i);
		newElem = new IPMaddrRule();
		IPMaddr newIPMaddr;
		try{ newIPMaddr = IPMaddr.parse( (String) rowVector.elementAt(2) ); }
		catch(IllegalArgumentException e){ continue; }
		newElem.setIpMaddr( newIPMaddr );
		newElem.setName( (String) rowVector.elementAt(3) );
		newElem.setDescription( (String) rowVector.elementAt(4) );
		elemList.add(newElem);
	    }
	    catch(Exception e){
		elemList = null;
	    }
        }

	ReportingSettings transformSettings = (ReportingSettings) transformContext.transform().getSettings();
        if( elemList != null )
	    transformSettings.getNetworkDirectory().setEntries(elemList); 
        return transformSettings;
    }
    
    public Vector generateRows(Object transformSettings){
        Vector allRows = new Vector();
        Vector row;
	IPMaddrRule newElem;
        java.util.List elemList = ((ReportingSettings)transformSettings).getNetworkDirectory().getEntries();
        int count = 0;
        for (Iterator i = elemList.iterator() ; i.hasNext() ; count++) {
            newElem = (IPMaddrRule) i.next();
            row = new Vector();
            row.add(new Integer(count+1));
            row.add(super.ROW_SAVED);
            row.add(newElem.getIpMaddr().toString());
	    row.add(newElem.getName());
            row.add(newElem.getDescription());
            allRows.add(row);
        }
        return allRows;
    }


} 
