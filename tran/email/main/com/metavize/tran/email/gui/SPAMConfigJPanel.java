/*
 *
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.email.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;
import com.metavize.gui.widgets.editTable.*;

import com.metavize.mvvm.tran.TransformContext;
import com.metavize.tran.email.*;

import java.awt.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;


public class SPAMConfigJPanel extends MEditTableJPanel {

    public SPAMConfigJPanel() {

        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("spam filter rules");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(false);

        // create actual table model
        SpamTableModel spamTableModel = new SpamTableModel();
        this.setTableModel( spamTableModel );
    }
}


class SpamTableModel extends MSortedTableModel{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 70; /* source */
    private static final int C3_MW = 55; /* scan */
    private static final int C4_MW = 80; /* strength */
    private static final int C5_MW = 200; /* action if SPAM detected */
    private static final int C6_MW = Util.chooseMax(T_TW - (C1_MW + C2_MW + C3_MW + C4_MW + C5_MW), 120); /* description */


    private SSCTLDefinition tempElem = new SSCTLDefinition();;
    private ComboBoxModel actionModel = super.generateComboBoxModel( tempElem.getActionOnDetectEnumeration(), tempElem.getActionOnDetect().toString() );
    private ComboBoxModel strengthModel = super.generateComboBoxModel( tempElem.getScanStrengthEnumeration(), tempElem.getScanStrength() );

    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, false, false, false, false, String.class,  null, "source");
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, Boolean.class,  null, sc.bold("scan") );
        addTableColumn( tableColumnModel,  4, C4_MW, false, true,  false, false, ComboBoxModel.class,  null, "strength");
        addTableColumn( tableColumnModel,  5, C5_MW, false, true,  false, false, ComboBoxModel.class,  null, sc.html("action if<br>SPAM detected"));
        addTableColumn( tableColumnModel,  6, C6_MW, true, true, false, true, String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        return tableColumnModel;
    }

    public void generateSettings(Object settings, boolean validateOnly) throws Exception {
	SSCTLDefinition spamInboundCtl = null;
	SSCTLDefinition spamOutboundCtl = null;

	for( Vector rowVector : (Vector<Vector>) this.getDataVector() ){

            SSCTLDefinition newElem = new SSCTLDefinition();
            newElem.setScan( ((Boolean)rowVector.elementAt(3)).booleanValue() );
            newElem.setScanStrength( (String) ((ComboBoxModel)rowVector.elementAt(4)).getSelectedItem() );
            newElem.setActionOnDetect(com.metavize.tran.email.Action.getInstance( (String) ((ComboBoxModel)rowVector.elementAt(5)).getSelectedItem() ) );
            newElem.setNotes( (String) rowVector.elementAt(6) );
	    
	    if( ((String)rowVector.elementAt(2)).equals("inbound") ){
		spamInboundCtl = newElem;
	    }
	    else if( ((String)rowVector.elementAt(2)).equals("outbound") ){
		spamOutboundCtl = newElem;
	    }
	    else{
		System.err.println("unknown source");
		continue;
	    }
	    
        }
	
	// SAVE SETTINGS ////////
	if( !validateOnly ){
	    EmailSettings emailSettings = (EmailSettings) settings;
	    emailSettings.setSpamInboundCtl( spamInboundCtl );
	    emailSettings.setSpamOutboundCtl( spamOutboundCtl );
	}


    }

    public Vector generateRows(Object settings) {
        EmailSettings emailSettings = (EmailSettings) settings;
        Vector allRows = new Vector();

	// INBOUND
	Vector inboundRow = new Vector();
        SSCTLDefinition spamInboundCtl  = emailSettings.getSpamInboundCtl();
        if(spamInboundCtl == null)
            spamInboundCtl = new SSCTLDefinition();
        inboundRow.add( super.ROW_SAVED );
        inboundRow.add( new Integer(1) );
        inboundRow.add( "inbound" );
        inboundRow.add( new Boolean(spamInboundCtl.isScan()) );
        ComboBoxModel inboundStrengthComboBoxModel = super.copyComboBoxModel(strengthModel);
        inboundStrengthComboBoxModel.setSelectedItem( (String) spamInboundCtl.getScanStrength() );
        inboundRow.add( inboundStrengthComboBoxModel );
        ComboBoxModel inboundActionComboBoxModel = super.copyComboBoxModel(actionModel);
        inboundActionComboBoxModel.setSelectedItem( (String) spamInboundCtl.getActionOnDetect().toString() );
        inboundRow.add( inboundActionComboBoxModel );
        inboundRow.add( spamInboundCtl.getNotes() );
	allRows.add(inboundRow);

	// OUTBOUND
	Vector outboundRow = new Vector();
        SSCTLDefinition spamOutboundCtl = emailSettings.getSpamOutboundCtl();
        if(spamOutboundCtl == null)
            spamOutboundCtl = new SSCTLDefinition();
        outboundRow.add( super.ROW_SAVED );
        outboundRow.add( new Integer(2) );
        outboundRow.add( "outbound" );
        outboundRow.add( new Boolean(spamOutboundCtl.isScan()) );
        ComboBoxModel outboundStrengthComboBoxModel = super.copyComboBoxModel(strengthModel);
        outboundStrengthComboBoxModel.setSelectedItem( (String) spamOutboundCtl.getScanStrength() );
        outboundRow.add( outboundStrengthComboBoxModel );
        ComboBoxModel outboundActionComboBoxModel = super.copyComboBoxModel(actionModel);
        outboundActionComboBoxModel.setSelectedItem( (String) spamOutboundCtl.getActionOnDetect().toString() );
        outboundRow.add( outboundActionComboBoxModel );
        outboundRow.add( spamOutboundCtl.getNotes() );
	allRows.add(outboundRow);

        return allRows;
    }
}
