/*
 *
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.email.gui;

import java.awt.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.tran.email.*;

public class SPAMConfigJPanel extends MEditTableJPanel {

    public SPAMConfigJPanel(TransformContext transformContext) {

        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("spam filter rules");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(false);

        // create actual table model
        SpamTableModel spamTableModel = new SpamTableModel(transformContext);
        this.setTableModel( spamTableModel );
    }
}


class SpamTableModel extends MSortedTableModel{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C1_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C2_MW = 70; /* source */
    private static final int C3_MW = 55; /* scan */
    private static final int C4_MW = 80; /* strength */
    private static final int C5_MW = 200; /* action if SPAM detected */
    private static final int C6_MW = Util.chooseMax(T_TW - (C1_MW + C2_MW + C3_MW + C4_MW + C5_MW), 120); /* description */

    private static final StringConstants sc = StringConstants.getInstance();


    private SSCTLDefinition tempElem;
    private ComboBoxModel actionModel;
    private ComboBoxModel strengthModel;

    SpamTableModel(TransformContext transformContext){
        super(transformContext);

        tempElem = new SSCTLDefinition();
        actionModel = super.generateComboBoxModel( tempElem.getActionOnDetectEnumeration(), tempElem.getActionOnDetect().toString() );
        strengthModel = super.generateComboBoxModel( tempElem.getScanStrengthEnumeration(), tempElem.getScanStrength() );

        refresh();
    }

    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, false, false, String.class,  null, "status");
        addTableColumn( tableColumnModel,  2, C2_MW, false, false, false, false, String.class,  null, "source");
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, Boolean.class,  null, sc.bold( "scan" ) );
        addTableColumn( tableColumnModel,  4, C4_MW, false, true,  false, false, ComboBoxModel.class,  null, "strength");
        addTableColumn( tableColumnModel,  5, C5_MW, false, true,  false, false, ComboBoxModel.class,  null, "<html><center>action if<br>SPAM detected</center></html>");
        // addTableColumn( tableColumnModel,  6,  95, false, true,  false, false, Boolean.class, null, "<html><center>keep copy of<br>SPAM emails</center></html>");
        //        addTableColumn( tableColumnModel,  7,  55, false, true,  false, false, Boolean.class, null, "alert");
        //        addTableColumn( tableColumnModel,  8,  55, false, true,  false, false, Boolean.class, null, "log");
        addTableColumn( tableColumnModel,  6, C6_MW, true, true, false, true, String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        return tableColumnModel;
    }

    public EmailSettings generateSettings(Vector dataVector){
        Vector rowVector;
        EmailTransform tran = (EmailTransform) transformContext.transform();
        EmailSettings settings = tran.getEmailSettings();
        SSCTLDefinition newElem;

        for(int i=0; i<dataVector.size(); i++){
            rowVector = (Vector) dataVector.elementAt(i);

            newElem = new SSCTLDefinition();
            // Alerts newAlerts= (Alerts)NodeType.type(Alerts.class).instantiate();

        if( ((String)rowVector.elementAt(2)).equals("inbound") ){
        settings.setSpamInboundCtl(newElem);
        }
        else if( ((String)rowVector.elementAt(2)).equals("outbound") ){
        settings.setSpamOutboundCtl(newElem);
        }
        else{
        System.err.println("unknown source");
        continue;
        }
            newElem.setScan( ((Boolean)rowVector.elementAt(3)).booleanValue() );
            newElem.setScanStrength( (String) ((ComboBoxModel)rowVector.elementAt(4)).getSelectedItem() );
            newElem.setActionOnDetect(com.metavize.tran.email.Action.getInstance( (String) ((ComboBoxModel)rowVector.elementAt(5)).getSelectedItem() ) );
            // newElem.setCopyOnBlock( ((Boolean)rowVector.elementAt(6)).booleanValue() );

            // newAlerts.generateCriticalAlerts( ((Boolean) rowVector.elementAt(7)).booleanValue() );
            // newAlerts.generateSummaryAlerts( ((Boolean) rowVector.elementAt(8)).booleanValue() );
            newElem.setNotes( (String) rowVector.elementAt(6) );

            // newElem.alerts(newAlerts);
        }

        return settings;
    }

    public Vector generateRows(Object settin)
    {
        EmailSettings settings = (EmailSettings) settin;
        Vector allRows = new Vector();
        Vector inboundRow, outboundRow;
        inboundRow = new Vector();
        outboundRow = new Vector();

        SSCTLDefinition spamInboundCtl  = settings.getSpamInboundCtl();
        if(spamInboundCtl == null)
            spamInboundCtl = new SSCTLDefinition();
        SSCTLDefinition spamOutboundCtl = settings.getSpamOutboundCtl();
        if(spamOutboundCtl == null)
            spamOutboundCtl = new SSCTLDefinition();

        inboundRow.add( new Integer(1) );
        inboundRow.add( super.ROW_SAVED );
        inboundRow.add( "inbound" );
        inboundRow.add( new Boolean(spamInboundCtl.isScan()) );
        ComboBoxModel inboundStrengthComboBoxModel = super.copyComboBoxModel(strengthModel);
        inboundStrengthComboBoxModel.setSelectedItem( (String) spamInboundCtl.getScanStrength() );
        inboundRow.add( inboundStrengthComboBoxModel );
        ComboBoxModel inboundActionComboBoxModel = super.copyComboBoxModel(actionModel);
        inboundActionComboBoxModel.setSelectedItem( (String) spamInboundCtl.getActionOnDetect().toString() );
        inboundRow.add( inboundActionComboBoxModel );
        // inboundRow.add( new Boolean(spamInboundCtl.isCopyOnBlock()) );
        /*
        Alerts inboundAlerts = spamInboundCtl.alerts();
        if(inboundAlerts == null)
            inboundAlerts = (Alerts) NodeType.type(Alerts.class).instantiate();
        inboundRow.add( new Boolean(inboundAlerts.generateCriticalAlerts()) );
        inboundRow.add( new Boolean(inboundAlerts.generateSummaryAlerts())  );
        */
        inboundRow.add( spamInboundCtl.getNotes() );
    allRows.add(inboundRow);

        outboundRow.add( new Integer(2) );
        outboundRow.add( super.ROW_SAVED );
        outboundRow.add( "outbound" );
        outboundRow.add( new Boolean(spamOutboundCtl.isScan()) );
        ComboBoxModel outboundStrengthComboBoxModel = super.copyComboBoxModel(strengthModel);
        outboundStrengthComboBoxModel.setSelectedItem( (String) spamOutboundCtl.getScanStrength() );
        outboundRow.add( outboundStrengthComboBoxModel );
        ComboBoxModel outboundActionComboBoxModel = super.copyComboBoxModel(actionModel);
        outboundActionComboBoxModel.setSelectedItem( (String) spamOutboundCtl.getActionOnDetect().toString() );
        outboundRow.add( outboundActionComboBoxModel );
        // outboundRow.add( new Boolean(spamOutboundCtl.isCopyOnBlock()) );
        /*
        Alerts outboundAlerts = spamOutboundCtl.alerts();
        if(outboundAlerts == null)
            outboundAlerts = (Alerts) NodeType.type(Alerts.class).instantiate();
        outboundRow.add( new Boolean(outboundAlerts.generateCriticalAlerts()) );
        outboundRow.add( new Boolean(outboundAlerts.generateSummaryAlerts())  );
        */
        outboundRow.add( spamOutboundCtl.getNotes() );
    allRows.add(outboundRow);

        return allRows;
    }
}
