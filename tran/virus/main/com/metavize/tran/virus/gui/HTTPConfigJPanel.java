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
    
        
    public HTTPConfigJPanel(TransformContext transformContext) {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("HTTP virus scan rules");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(false);
        
        // create actual table model
        HTTPTableModel tableModel = new HTTPTableModel(transformContext);
        this.setTableModel( tableModel );    
    }
}


class HTTPTableModel extends MSortedTableModel{ 
    private static final StringConstants sc = StringConstants.getInstance();

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C1_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C2_MW = 65; /* protocol */
    private static final int C3_MW = 65; /* source */
    private static final int C4_MW = 55; /* block */
    private static final int C5_MW = Util.chooseMax(T_TW - (C1_MW + C2_MW + C3_MW + C4_MW), 120); /* description */

    HTTPTableModel(TransformContext transformContext){
        super(transformContext);
        
        refresh();
    }
    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min  rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  2, C2_MW, false, false, false, false, String.class,  null, "protocol");
        addTableColumn( tableColumnModel,  3, C3_MW, false, false, false, false, String.class,  null, "source");
        addTableColumn( tableColumnModel,  4, C4_MW, false, true,  false, false, Boolean.class, null, sc.bold( sc.TITLE_BLOCK ));
        // addTableColumn( tableColumnModel,  5,  55, false, true,  false, false, Boolean.class, null, "alert");
        // addTableColumn( tableColumnModel,  6,  55, false, true,  false, false, Boolean.class, null, "log");
        addTableColumn( tableColumnModel,  5, C5_MW, true, true, false, true, String.class,  
                        sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION );
        return tableColumnModel;
    }

    
    public Object generateSettings(Vector dataVector){
        Vector rowVector;
        
        VirusSettings virusSettings = ((VirusTransform)transformContext.transform()).getVirusSettings();
        VirusConfig newElem;
        
        for(int i=0; i<dataVector.size(); i++){
            rowVector = (Vector) dataVector.elementAt(i);
            
            newElem = new VirusConfig();
            //Alerts newAlerts = (Alerts)NodeType.type(Alerts.class).instantiate();
            
	    if( ((String)rowVector.elementAt(3)).equals("inbound") ){
		virusSettings.setHttpInbound(newElem);
	    }
	    else if( ((String)rowVector.elementAt(3)).equals("outbound") ){
		virusSettings.setHttpOutbound(newElem);
	    }
	    else{
		System.err.println("unknown source");
		continue;
	    }
	    
            newElem.setScan( (Boolean) rowVector.elementAt(4) );
            // newAlerts.generateCriticalAlerts( ((Boolean) rowVector.elementAt(5)).booleanValue() );
            // newAlerts.generateSummaryAlerts( ((Boolean) rowVector.elementAt(6)).booleanValue() );
            // newElem.alerts(newAlerts);
            newElem.setNotes( (String) rowVector.elementAt(5) );
        }
        
        return virusSettings;
    }
    
    public Vector generateRows(Object transformSettings){
	VirusSettings virusSettings = (VirusSettings) transformSettings;
	Vector allRows = new Vector();
        Vector inboundRow, outboundRow;
        inboundRow = new Vector();
        outboundRow = new Vector();
        
        VirusConfig virusInboundCtl  = virusSettings.getHttpInbound();
        VirusConfig virusOutboundCtl = virusSettings.getHttpOutbound();

        inboundRow.add( new Integer(1) );
        inboundRow.add( super.ROW_SAVED );
        inboundRow.add( "HTTP" );
        inboundRow.add( "inbound" );
        
        inboundRow.add( virusInboundCtl.getScan() );
        // Alerts inboundAlerts = virusInboundCtl.alerts();
        // if(inboundAlerts == null)
        //    inboundAlerts = (Alerts) NodeType.type(Alerts.class).instantiate();
        // inboundRow.add( new Boolean(inboundAlerts.generateCriticalAlerts()) );
        // inboundRow.add( new Boolean(inboundAlerts.generateSummaryAlerts())  );
        inboundRow.add( virusInboundCtl.getNotes() );
	allRows.add(inboundRow);


        outboundRow.add( new Integer(2) );
        outboundRow.add( super.ROW_SAVED );
        outboundRow.add( "HTTP" );
        outboundRow.add( "outbound" );
        
        outboundRow.add( virusOutboundCtl.getScan() );
        // Alerts outboundAlerts = virusOutboundCtl.alerts();
        // if(outboundAlerts == null)
        //     outboundAlerts = (Alerts) NodeType.type(Alerts.class).instantiate();
        // outboundRow.add( new Boolean(outboundAlerts.generateCriticalAlerts()) );
        // outboundRow.add( new Boolean(outboundAlerts.generateSummaryAlerts())  );
        outboundRow.add( virusOutboundCtl.getNotes() );
	allRows.add(outboundRow);

        return allRows;
    }
}
