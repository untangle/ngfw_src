/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.email.gui;


import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;
import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;

import com.metavize.mvvm.tran.TransformContext;
import com.metavize.tran.email.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import javax.swing.event.*;

public class VirusConfigJPanel extends MEditTableJPanel {
    
    
    public VirusConfigJPanel() {

        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("virus filter rules");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(false);

        // create actual table model
        VirusTableModel virusTableModel = new VirusTableModel();
        this.setTableModel( virusTableModel );
    }
}


class VirusTableModel extends MSortedTableModel{ 

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 70; /* source */
    private static final int C3_MW = 55; /* scan */
    private static final int C4_MW = 200; /* action if virus detected */
    private static final int C5_MW = Util.chooseMax(T_TW - (C1_MW + C2_MW + C3_MW + C4_MW), 120); /* description */


    private VSCTLDefinition tempElem = new VSCTLDefinition();
    private ComboBoxModel actionModel = super.generateComboBoxModel( tempElem.getActionOnDetectEnumeration(), tempElem.getActionOnDetect().toString() );

    
    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, false, false, false, false, String.class,  null, "source");
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, Boolean.class,  null, sc.bold("scan"));
        addTableColumn( tableColumnModel,  4, C4_MW, false, true,  false, false, ComboBoxModel.class,  null, sc.html("action if<br>virus detected"));
        addTableColumn( tableColumnModel,  5, C5_MW, true, true, false, true, String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        return tableColumnModel;
    }
    
    
    public void generateSettings(Object settings, boolean validateOnly) throws Exception {

        VSCTLDefinition virusInboundCtl = null;
	VSCTLDefinition virusOutboundCtl = null;

	for( Vector rowVector : (Vector<Vector>) this.getDataVector() ){
            VSCTLDefinition newElem = new VSCTLDefinition();
            newElem.setScan( ((Boolean)rowVector.elementAt(3)).booleanValue() );
            newElem.setActionOnDetect( com.metavize.tran.email.Action.getInstance((String) ((ComboBoxModel)rowVector.elementAt(4)).getSelectedItem()) );
            newElem.setNotes( (String) rowVector.elementAt(5) );
            
	    if( ((String)rowVector.elementAt(2)).equals("inbound") ){
		virusInboundCtl = newElem;
	    }
	    else if( ((String)rowVector.elementAt(2)).equals("outbound") ){
		virusOutboundCtl = newElem;
	    }
	    

        }

	// SAVE SETTINGS /////
	if( !validateOnly ){
	    EmailSettings emailSettings = (EmailSettings) settings;
	    emailSettings.setVirusInboundCtl( virusInboundCtl );
	    emailSettings.setVirusOutboundCtl( virusOutboundCtl );
	}
        

    }
    
    public Vector generateRows(Object settings){
        EmailSettings emailSettings = (EmailSettings) settings;
	Vector allRows = new Vector();

        // INBOUND
	Vector inboundRow = new Vector();
        VSCTLDefinition virusInboundCtl = emailSettings.getVirusInboundCtl();
        if(virusInboundCtl == null)
            virusInboundCtl = new VSCTLDefinition();
        inboundRow.add( super.ROW_SAVED );
        inboundRow.add( new Integer(1) );
        inboundRow.add( "inbound" );
        inboundRow.add( new Boolean(virusInboundCtl.isScan()));
        ComboBoxModel inboundComboBoxModel = super.copyComboBoxModel(actionModel);
        inboundComboBoxModel.setSelectedItem( (String) virusInboundCtl.getActionOnDetect().toString() );
        inboundRow.add( inboundComboBoxModel );
        inboundRow.add( virusInboundCtl.getNotes() );
	allRows.add(inboundRow);

	// OUTBOUND
	Vector outboundRow = new Vector();
        VSCTLDefinition virusOutboundCtl = emailSettings.getVirusOutboundCtl(); 
        if(virusOutboundCtl == null)
            virusOutboundCtl = new VSCTLDefinition();
        outboundRow.add( super.ROW_SAVED );
        outboundRow.add( new Integer(2) );
        outboundRow.add( "outbound" );
        outboundRow.add( new Boolean(virusOutboundCtl.isScan()));
        ComboBoxModel outboundComboBoxModel = super.copyComboBoxModel(actionModel);
        outboundComboBoxModel.setSelectedItem( (String) virusOutboundCtl.getActionOnDetect().toString() );
        outboundRow.add( outboundComboBoxModel );
        outboundRow.add( virusOutboundCtl.getNotes() );
	allRows.add(outboundRow);

        return allRows;
    }
} 
