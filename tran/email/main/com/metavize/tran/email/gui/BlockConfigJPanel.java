/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.email.gui;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.tran.email.*;

public class BlockConfigJPanel extends MEditTableJPanel {
    
    public BlockConfigJPanel(TransformContext transformContext) {

        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("email block rules");
        super.setDetailsTitle("rule notes");
        
        // create actual table model
        EmailTableModel emailTableModel = new EmailTableModel(transformContext);
        this.setTableModel( emailTableModel );
    }
}


class EmailTableModel extends MSortedTableModel{ 
    
    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C1_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C2_MW = 150; /* search for this */
    private static final int C3_MW = 150; /* in this part of the email */
    private static final int C4_MW = 65; /* block if found */
    private static final int C5_MW = 95; /* keep copy of blocked email */
    private static final int C6_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW + C5_MW), 120); /* description */

    private static final StringConstants sc = StringConstants.getInstance();

    private MLDefinition tempElem;
    ComboBoxModel keyModel;
    
    EmailTableModel(TransformContext transformContext){
        super(transformContext);
        
        tempElem = new MLDefinition();
        keyModel = super.generateComboBoxModel( tempElem.getFieldEnumeration(), tempElem.getField().toString() );
        refresh();
    }
    
    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min  rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, false, false, String.class,  null, "status");
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class,  "search string", "search for this");
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, ComboBoxModel.class,  keyModel, "<html><center>in this part<br>of the email</center></html>");
        addTableColumn( tableColumnModel,  4, C4_MW, false, true,  false, false, Boolean.class, "true", "<html><b><center>block if<br>found</center></b></html>");
        // addTableColumn( tableColumnModel,  5, C5_MW, false, true,  false, false, Boolean.class, "false", "<html><center>keep copy of<br>blocked email</center></html>");
        //        addTableColumn( tableColumnModel,  6,  55, false, true,  false, false, Boolean.class, "false", "alert");
        //        addTableColumn( tableColumnModel,  7,  55, false, true,  false, false, Boolean.class, "true", "log");
        addTableColumn( tableColumnModel,  5, C6_MW, true, true, false, true, String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        return tableColumnModel;
    }

    public EmailSettings generateSettings(Vector dataVector){
        Vector rowVector;
        
        EmailTransform tran = (EmailTransform) transformContext.transform();
        EmailSettings settings = tran.getEmailSettings();
        MLDefinition newElem;
        List elemList = new ArrayList();
        for(int i=0; i<dataVector.size(); i++){
            rowVector = (Vector) dataVector.elementAt(i);
            
            newElem = new MLDefinition();
            // Alerts newAlerts= (Alerts)NodeType.type(Alerts.class).instantiate();
            
            if( ((Boolean) rowVector.elementAt(4)).booleanValue() == true )
                newElem.setAction(MLDefinition.BLOCK);
            else
                newElem.setAction(MLDefinition.PASS);
            
            newElem.setValue( (String) rowVector.elementAt(2) );
            newElem.setField( MLDefinition.getFieldInstance((String) ((ComboBoxModel) rowVector.elementAt(3)).getSelectedItem()) );

	    if( (Boolean) rowVector.elementAt(4) )
		newElem.setAction( MLDefinition.BLOCK );
	    else
		newElem.setAction( MLDefinition.PASS );
            // newElem.setCopyOnBlock( ((Boolean) rowVector.elementAt(5)).booleanValue() );
            // newAlerts.generateCriticalAlerts( ((Boolean) rowVector.elementAt(6)).booleanValue() );
            // newAlerts.generateSummaryAlerts( ((Boolean) rowVector.elementAt(7)).booleanValue() );
            newElem.setNotes( (String) rowVector.elementAt(5) );
            
            // newElem.alerts(newAlerts);
            
            elemList.add(newElem);  
        }
        
        settings.setFilters( elemList );

        return settings;
    }
    
    public Vector generateRows(Object settin){
        EmailSettings settings = (EmailSettings) settin;
        Vector allRows = new Vector();
        int counter = 1;
        MLDefinition newElem;
        List elemList = settings.getFilters();

        for(Iterator iter = elemList.iterator(); iter.hasNext();) {
            newElem = (MLDefinition) iter.next();
            
            if( !(newElem.getAction().equals(MLDefinition.PASS) || newElem.getAction().equals(MLDefinition.BLOCK)) ) 
                continue;
            
            // alerts
            /*
            if(newElem.alerts() == null)
		newElem.alerts( (Alerts) NodeType.type(Alerts.class).instantiate()  );
            Alerts newAlerts = newElem.alerts();
            */

            Vector row = new Vector();
            row.add(new Integer(counter));
            row.add(super.ROW_SAVED);
            row.add(newElem.getValue());
            row.add(super.generateComboBoxModel(newElem.getFieldEnumeration(), newElem.getField().toString()));

            if( newElem.getAction().equals(MLDefinition.BLOCK) )
                row.add(new Boolean(true));
            else
                row.add(new Boolean(false));
            
            // row.add(new Boolean(newElem.isCopyOnBlock()));
            // row.add(new Boolean(newElem.alerts().generateCriticalAlerts()));
            // row.add(new Boolean(newElem.alerts().generateSummaryAlerts()));

            row.add(newElem.getNotes());
            
            allRows.add(row);
            
            counter++;
        }
        return allRows;
    }   
}
