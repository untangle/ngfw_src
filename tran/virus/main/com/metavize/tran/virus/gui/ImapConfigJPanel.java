/*
 *
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.virus.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.tran.virus.*;
import com.metavize.tran.mail.*;
import com.metavize.mvvm.tran.TransformContext;


import java.awt.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;


public class ImapConfigJPanel extends MEditTableJPanel {

    public ImapConfigJPanel() {

        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("spam filter rules");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(false);

        // create actual table model
        ImapTableModel imapTableModel = new ImapTableModel();
        this.setTableModel( imapTableModel );
    }
}


class ImapTableModel extends MSortedTableModel{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 100; /* source */
    private static final int C3_MW = 55; /* scan */
    private static final int C4_MW = 140; /* action if SPAM detected */
    private static final int C5_MW = Util.chooseMax(T_TW - (C1_MW + C2_MW + C3_MW + C4_MW), 120); /* description */

    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, false, false, false, false, String.class,  null, "source");
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, Boolean.class,  null, sc.bold("scan") );
        addTableColumn( tableColumnModel,  4, C4_MW, false, true,  false, false, ComboBoxModel.class,  null, sc.html("action if<br>SPAM detected"));
        addTableColumn( tableColumnModel,  5, C5_MW, true,  true,  false, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        return tableColumnModel;
    }

    private static final String VIRUS_INBOUND = "inbound SMTP";
    private static final String VIRUS_OUTBOUND = "outbound SMTP";

    public void generateSettings(Object settings, boolean validateOnly) throws Exception {
	VirusIMAPConfig virusIMAPConfigInbound = null;
	VirusIMAPConfig virusIMAPConfigOutbound = null;

	for( Vector rowVector : (Vector<Vector>) this.getDataVector() ){

            VirusIMAPConfig virusIMAPConfig = new VirusIMAPConfig();
            virusIMAPConfig.setScan( (Boolean) rowVector.elementAt(3) );
	    String actionString = (String) ((ComboBoxModel)rowVector.elementAt(4)).getSelectedItem();
	    VirusMessageAction messageAction = VirusMessageAction.getInstance( actionString );
            virusIMAPConfig.setMsgAction( messageAction );
            virusIMAPConfig.setNotes( (String) rowVector.elementAt(5) );
	    
	    if( ((String)rowVector.elementAt(2)).equals(VIRUS_INBOUND) ){
		virusIMAPConfigInbound = virusIMAPConfig;
	    }
	    else if( ((String)rowVector.elementAt(2)).equals(VIRUS_OUTBOUND) ){
		virusIMAPConfigOutbound = virusIMAPConfig;
	    }  
        }
	
	// SAVE SETTINGS ////////
	if( !validateOnly ){
	    VirusSettings virusSettings = (VirusSettings) settings;
	    virusSettings.setIMAPInbound( virusIMAPConfigInbound );
	    virusSettings.setIMAPOutbound( virusIMAPConfigOutbound );
	}


    }

    public Vector generateRows(Object settings) {
        VirusSettings virusSettings = (VirusSettings) settings;
        Vector allRows = new Vector();

	// INBOUND
	Vector inboundRow = new Vector();
        VirusIMAPConfig virusIMAPConfigInbound = virusSettings.getIMAPInbound();
        inboundRow.add( super.ROW_SAVED );
        inboundRow.add( new Integer(1) );
        inboundRow.add( VIRUS_INBOUND );
        inboundRow.add( virusIMAPConfigInbound.getScan() );
        ComboBoxModel inboundActionComboBoxModel =  super.generateComboBoxModel( VirusMessageAction.getValues(), virusIMAPConfigInbound.getMsgAction() );
        inboundRow.add( inboundActionComboBoxModel );
        inboundRow.add( virusIMAPConfigInbound.getNotes() );
	allRows.add(inboundRow);

	// OUTBOUND
	Vector outboundRow = new Vector();
        VirusIMAPConfig virusIMAPConfigOutbound = virusSettings.getIMAPOutbound();
        outboundRow.add( super.ROW_SAVED );
        outboundRow.add( new Integer(1) );
        outboundRow.add( VIRUS_OUTBOUND );
        outboundRow.add( virusIMAPConfigOutbound.getScan() );
        ComboBoxModel outboundActionComboBoxModel =  super.generateComboBoxModel( VirusMessageAction.getValues(), virusIMAPConfigOutbound.getMsgAction() );
        outboundRow.add( outboundActionComboBoxModel );
        outboundRow.add( virusIMAPConfigOutbound.getNotes() );
	allRows.add(outboundRow);

        return allRows;
    }
}
