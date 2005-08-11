/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.virus.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;
import com.metavize.tran.virus.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import javax.swing.event.*;

public class HTTPConfigJPanel extends MEditTableJPanel {
    
        
    public HTTPConfigJPanel() {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("HTTP virus scan rules");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(false);
        
        // create actual table model
        HTTPTableModel tableModel = new HTTPTableModel();
        this.setTableModel( tableModel );    
    }
}


class HTTPTableModel extends MSortedTableModel{ 


    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 100; /* source */
    private static final int C3_MW = 55;  /* block */
    private static final int C4_MW = Util.chooseMax(T_TW - (C0_MW + C2_MW + C3_MW), 120); /* description */

    protected boolean getSortable(){ return false; }
    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, false, false, false, false, String.class,  null, "source");
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, Boolean.class, null, sc.bold("scan"));
        addTableColumn( tableColumnModel,  4, C4_MW, true,  true,  false, true,  String.class, sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION );
        return tableColumnModel;
    }

    private static final String INBOUND_SOURCE = "inbound HTTP";
    private static final String OUTBOUND_SOURCE = "outbound HTTP";
    
    public void generateSettings(Object settings, boolean validateOnly) throws Exception {
	for( Vector rowVector : (Vector<Vector>) this.getDataVector() ){
            
            VirusConfig virusConfig = new VirusConfig();
            virusConfig.setScan( (Boolean) rowVector.elementAt(3) );
            virusConfig.setNotes( (String) rowVector.elementAt(4) );
            
	    // SAVE SETTINGS
	    if( !validateOnly ){
		VirusSettings virusSettings = (VirusSettings) settings;
		if( ((String)rowVector.elementAt(2)).equals(INBOUND_SOURCE) ){
		    virusSettings.setHttpInbound(virusConfig);
		}
		else if( ((String)rowVector.elementAt(2)).equals(OUTBOUND_SOURCE) ){
		    virusSettings.setHttpOutbound(virusConfig);
		}

	    }
	    
        }
        
    }
    
    public Vector generateRows(Object settings){
	VirusSettings virusSettings = (VirusSettings) settings;
	Vector allRows = new Vector();

	// INBOUND
	Vector inboundRow = new Vector();
        VirusConfig virusInboundCtl  = virusSettings.getHttpInbound();
        inboundRow.add( super.ROW_SAVED );
        inboundRow.add( new Integer(1) );
        inboundRow.add( INBOUND_SOURCE );
        inboundRow.add( virusInboundCtl.getScan() );
        inboundRow.add( virusInboundCtl.getNotes() );
	allRows.add(inboundRow);

	// OUTBOUND
	Vector outboundRow = new Vector();
        VirusConfig virusOutboundCtl = virusSettings.getHttpOutbound();
        outboundRow.add( super.ROW_SAVED );
        outboundRow.add( new Integer(2) );
        outboundRow.add( OUTBOUND_SOURCE );
        outboundRow.add( virusOutboundCtl.getScan() );
        outboundRow.add( virusOutboundCtl.getNotes() );
	allRows.add(outboundRow);

        return allRows;
    }
}
