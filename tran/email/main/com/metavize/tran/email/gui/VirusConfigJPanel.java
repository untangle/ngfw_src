/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.email.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;

import com.metavize.tran.email.*;

import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import javax.swing.event.*;

public class VirusConfigJPanel extends MEditTableJPanel {
    
    
    public VirusConfigJPanel(TransformContext transformContext) {

        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("virus filter rules");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(false);

        // create actual table model
        VirusTableModel virusTableModel = new VirusTableModel(transformContext);
        this.setTableModel( virusTableModel );
    }
}


class VirusTableModel extends MSortedTableModel{ 

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C1_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C2_MW = 70; /* source */
    private static final int C3_MW = 55; /* scan */
    private static final int C4_MW = 200; /* action if virus detected */
    private static final int C5_MW = Util.chooseMax(T_TW - (C1_MW + C2_MW + C3_MW + C4_MW), 120); /* description */

    private static final StringConstants sc = StringConstants.getInstance();


    private VSCTLDefinition tempElem;
    private ComboBoxModel actionModel;

    
    VirusTableModel(TransformContext transformContext){
        super(transformContext);
        
        tempElem = new VSCTLDefinition();
        actionModel = super.generateComboBoxModel( tempElem.getActionOnDetectEnumeration(), tempElem.getActionOnDetect().toString() );
        
        refresh();
    }
    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min  rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, false, false, String.class,  null, "status");
        addTableColumn( tableColumnModel,  2, C2_MW, false, false, false, false, String.class,  null, "source");
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, Boolean.class,  null, "<html><b><center>scan</center></b></html>");
        addTableColumn( tableColumnModel,  4, C4_MW, false, true,  false, false, ComboBoxModel.class,  null, "<html><center>action if<br>virus detected</center></html>");
        // addTableColumn( tableColumnModel,  5,  95, false, true,  false, false, Boolean.class, null, "<html><center>keep copy of<br>infected emails</center></html>");
        // addTableColumn( tableColumnModel,  6,  55, false, true,  false, false, Boolean.class, null, "alert");
        // addTableColumn( tableColumnModel,  7,  55, false, true,  false, false, Boolean.class, null, "log");
        addTableColumn( tableColumnModel,  5, C5_MW, true, true, false, true, String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        return tableColumnModel;
    }
    
    
    public EmailSettings generateSettings(Vector dataVector){
        Vector rowVector;
        
        EmailTransform tran = (EmailTransform) transformContext.transform();
        EmailSettings settings = tran.getEmailSettings();
        VSCTLDefinition newElem;
        
        for(int i=0; i<dataVector.size(); i++){
            rowVector = (Vector) dataVector.elementAt(i);
            
            newElem = new VSCTLDefinition();
            // Alerts newAlerts = (Alerts)NodeType.type(Alerts.class).instantiate();
            
	    if( ((String)rowVector.elementAt(2)).equals("inbound") ){
		settings.setVirusInboundCtl(newElem);
	    }
	    else if( ((String)rowVector.elementAt(2)).equals("outbound") ){
		settings.setVirusOutboundCtl(newElem);
	    }
	    else{
		System.err.println("unknown source");
		continue;
	    }
	    
            newElem.setScan( ((Boolean)rowVector.elementAt(3)).booleanValue() );
            newElem.setActionOnDetect( com.metavize.tran.email.Action.getInstance((String) ((ComboBoxModel)rowVector.elementAt(4)).getSelectedItem()) );
            // newElem.setCopyOnBlock( ((Boolean)rowVector.elementAt(5)).booleanValue() );
            
            // newAlerts.generateCriticalAlerts( ((Boolean) rowVector.elementAt(6)).booleanValue() );
            // newAlerts.generateSummaryAlerts( ((Boolean) rowVector.elementAt(7)).booleanValue() );
            newElem.setNotes( (String) rowVector.elementAt(5) );

            // newElem.alerts(newAlerts);
        }
        
        return settings;
    }
    
    public Vector generateRows(Object settin){
        EmailSettings settings = (EmailSettings) settin;
        Vector allRows = new Vector();
        Vector inboundRow, outboundRow;
        inboundRow = new Vector();
        outboundRow = new Vector();
        
        VSCTLDefinition virusInboundCtl  = settings.getVirusInboundCtl();
        if(virusInboundCtl == null)
            virusInboundCtl = new VSCTLDefinition();
        VSCTLDefinition virusOutboundCtl = settings.getVirusOutboundCtl(); 
        if(virusOutboundCtl == null)
            virusOutboundCtl = new VSCTLDefinition();
        
        inboundRow.add( new Integer(1) );
        inboundRow.add( super.ROW_SAVED );
        inboundRow.add( "inbound" );
        inboundRow.add( new Boolean(virusInboundCtl.isScan()));
        ComboBoxModel inboundComboBoxModel = super.copyComboBoxModel(actionModel);
        inboundComboBoxModel.setSelectedItem( (String) virusInboundCtl.getActionOnDetect().toString() );
        inboundRow.add( inboundComboBoxModel );
        // inboundRow.add( new Boolean(virusInboundCtl.isCopyOnBlock()) );
        /*
        Alerts inboundAlerts = virusInboundCtl.alerts();
        if(inboundAlerts == null)
            inboundAlerts = (Alerts) NodeType.type(Alerts.class).instantiate();
        inboundRow.add( new Boolean(inboundAlerts.generateCriticalAlerts()) );
        inboundRow.add( new Boolean(inboundAlerts.generateSummaryAlerts())  );
        */
        inboundRow.add( virusInboundCtl.getNotes() );
	allRows.add(inboundRow);


        outboundRow.add( new Integer(2) );
        outboundRow.add( super.ROW_SAVED );
        outboundRow.add( "outbound" );
        outboundRow.add( new Boolean(virusOutboundCtl.isScan()));
        ComboBoxModel outboundComboBoxModel = super.copyComboBoxModel(actionModel);
        outboundComboBoxModel.setSelectedItem( (String) virusOutboundCtl.getActionOnDetect().toString() );
        outboundRow.add( outboundComboBoxModel );
        // outboundRow.add( new Boolean(virusOutboundCtl.isCopyOnBlock()) );
        /*1
        Alerts outboundAlerts = virusOutboundCtl.alerts();
        if(outboundAlerts == null)
            outboundAlerts = (Alerts) NodeType.type(Alerts.class).instantiate();
        outboundRow.add( new Boolean(outboundAlerts.generateCriticalAlerts()) );
        outboundRow.add( new Boolean(outboundAlerts.generateSummaryAlerts())  );
        */
        outboundRow.add( virusOutboundCtl.getNotes() );
	allRows.add(outboundRow);

        return allRows;
    }
} 
