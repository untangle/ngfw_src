/*
 * AdminConfigJDialog.java
 *
 * Created on December 12, 2004, 1:06 AM
 */

package com.metavize.gui.configuration;

import com.metavize.gui.widgets.configWindow.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;

import com.metavize.mvvm.security.*;
import com.metavize.mvvm.*;

import com.metavize.gui.util.StringConstants;


/**
 *
 * @author  inieves
 */
public class AdminConfigJDialog extends ConfigJDialog {
    
    /** Creates a new instance of AdminConfigJDialog */
    public AdminConfigJDialog( ) {
        super(Util.getMMainJFrame());
        
        // create graphical context
        MEditTableJPanel mEditTableJPanel = new MEditTableJPanel(true, false);
        mEditTableJPanel.setInsets(new Insets(4, 4, 2, 2));
        mEditTableJPanel.setTableTitle("Administrator Accounts");
        mEditTableJPanel.setDetailsTitle("rule notes");
        mEditTableJPanel.setAddRemoveEnabled(true);

        // create actual table model
        AdminConfigTableModel configTableModel = new AdminConfigTableModel( Util.getMvvmContext().adminManager() );
        configTableModel.setMTableChangeListener(this);
        mEditTableJPanel.setTableModel( configTableModel );

        this.contentJPanel.add(mEditTableJPanel);
        this.contentJTabbedPane.setTitleAt(0, "Administrator Accounts");
        this.setTitle("Administrator Accounts");
        this.setTable(mEditTableJPanel.getJTable());
    }
    

}



    
class AdminConfigTableModel extends MSortedTableModel {

    private static final int MIN_PASSWD_LENGTH = 3;
    private AdminManager adminManager;
    private Hashtable loginHashtable;

    private static final StringConstants sc = StringConstants.getInstance();
    
    AdminConfigTableModel(AdminManager adminManager){
        super(null);
        this.adminManager = adminManager;
        loginHashtable = new Hashtable();
        refresh();
    }


    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #   min  rsz    edit   remv   desc   typ               def
        addTableColumn( tableColumnModel,  0,  30, false, false, true,  false, Integer.class,    null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  1,  55, false, false, false, false, String.class,     null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  2,  85, true,  true,  false, false, String.class,     sc.EMPTY_NAME,  sc.TITLE_NAME );
        addTableColumn( tableColumnModel,  3,  85, true,  true,  false, false, String.class,     "[no login]", "login");
        addTableColumn( tableColumnModel,  4,  85, true,  false, true,  false, MPasswordField.class,     PasswordUtil.encrypt("12345678"),     "<html><center>original<br>password</center></html>");
        addTableColumn( tableColumnModel,  5, 260, true,  true,  false, false, MPasswordField.class, "",     "<html><center>set new password<br>(for new accounts or to change password)</center></html>");
        addTableColumn( tableColumnModel,  6, 260, true,  true,  false, false, MPasswordField.class, "",     "<html><center>confirm new password<br>(for new accounts or to change password)</center></html>");
        addTableColumn( tableColumnModel,  7, 250, true,  true,  false, false, MPasswordField.class, "12345678",     "<html><center><b>original user's password</b><br>(required to change or remove account)</center></html>");
        addTableColumn( tableColumnModel,  8,  85, true,  true,  false, false, String.class,     "[no email]", "<html><center>email<br>address</center></html>");
        addTableColumn( tableColumnModel,  9,  85, true,  true,  false, false, Boolean.class,    "false",    "<html><center>send critical<br>alerts</center></html>");
        return tableColumnModel;
    }
    


    public void commit()
    {
        
        if(Util.getIsDemo())
            return;
        
        Vector allUsers = this.getDataVector();
        Vector tempUser;
        boolean oneValidAccount = false;
        String loginName;
        
        loginHashtable.clear();
        
        // go through all the rows and perform some tests
	Util.printMessage("STATUS: STARTING VERIFICATION of " + allUsers.size() + " user(s)");	
        for(int i=0; i<allUsers.size(); i++){
            tempUser = (Vector) allUsers.elementAt(i);
            
            byte[] origPasswd = ((MPasswordField)tempUser.elementAt(4)).getByteArray();
            char[] newPasswd = ((MPasswordField)tempUser.elementAt(5)).getPassword();
            char[] newConfPasswd  = ((MPasswordField)tempUser.elementAt(6)).getPassword();
            char[] proceedPasswd  = ((MPasswordField)tempUser.elementAt(7)).getPassword();
            loginName = (String) tempUser.elementAt(3);
            
            // verify that the login name is not duplicated
            if( loginHashtable.containsKey(loginName) ){
                Util.printMessage("--> ERROR: login name already exists for: " + i);
                refresh();
                return;
            }
            loginHashtable.put(loginName, loginName);
            
            // verify that all of the new and new confirm passwords and match up (and they are of the proper size)
            if( !java.util.Arrays.equals(newPasswd, newConfPasswd) ){
                Util.printMessage("--> ERROR: new and new conf passwords are not the same for: " + i);
                refresh();
                return;
            }
            if( (newPasswd.length > 0) && (newPasswd.length < MIN_PASSWD_LENGTH) ){
                Util.printMessage("--> ERROR: new and/or new conf passwords are not long enough for: " + i);
                refresh();
                return;
            }            
            // verify that all of the original and proceed passwords are the same for any non saved row
            if(    !((String)tempUser.elementAt(1)).equals(super.ROW_SAVED)
                &&  !PasswordUtil.check( String.valueOf(proceedPasswd), origPasswd )){
                Util.printMessage("--> ERROR: orig and proceed passwords are not the same for: " + i);
                 
                refresh();
                return;
            }
            // verify that if this is a new row, a new password has been chosen
            if( ((String)tempUser.elementAt(1)).equals(super.ROW_ADD) && (newPasswd.length == 0) ){
                Util.printMessage("--> ERROR: a new password has not been chosen for: " + i );
                refresh();
                return;
            }

            // record if the row was not removed, that way we know we had at least one valid account
            if( !((String)tempUser.elementAt(1)).equals(super.ROW_REMOVE) ){
                oneValidAccount = true;
            }

        }

        // verify that there is at least one valid entry after all operations
        if(!oneValidAccount){
            Util.printMessage("--> ERROR: there must be at least one unchanged or changed (but not removed) password for someone to log in with");
            refresh();
            return;
        }
	else{
	    Util.printMessage("STATUS: FINISHED VERIFICATION of all accounts");
	}

        generateSettings(allUsers);
        refresh();

    }
    
    public Set generateSettings(Vector dataVector) {        
        
        User newElem = null;
        Vector rowVector;
	Set allRows = new LinkedHashSet();

	Util.printMessage("STATUS: STARTING SAVING of accounts");

        for(int i=0; i<dataVector.size(); i++){
            rowVector = (Vector) dataVector.elementAt(i);
            
            

            byte[] origPasswd = ((MPasswordField)rowVector.elementAt(4)).getByteArray();
            char[] newPasswd = ((MPasswordField)rowVector.elementAt(5)).getPassword();
            
	    Util.printMessage("> UPDATE: row status " + (String) rowVector.elementAt(1) );

            if( ((String)rowVector.elementAt(1)).equals(super.ROW_REMOVE) ){
                Util.printMessage("> UPDATE: removing row " + i);
                continue; // those removed and of a strange state are never even added            
            }
            else{
                try{
                if( newPasswd.length > 0 ){
                    Util.printMessage("> UPDATE: saving row (changed password) " + i);
		    newElem = new User( (String)rowVector.elementAt(3), new String(newPasswd), (String)rowVector.elementAt(2) );
                }
                else{
                    Util.printMessage("> UPDATE: saving row (unchanged password) " + i);
		    newElem = new User( (String)rowVector.elementAt(3), origPasswd, (String)rowVector.elementAt(2) );
                }   
                }catch(Exception e) {
		    Util.handleExceptionNoRestart("--> ERROR:  problem changing row", e);
		    continue;
                }
            }

            newElem.setEmail( (String) rowVector.elementAt(8) );
            newElem.setSendAlerts( ((Boolean) rowVector.elementAt(9)).booleanValue() );            
            allRows.add(newElem);
        }
        

        AdminSettings adminSettings = adminManager.getAdminSettings();
        adminSettings.setUsers(allRows);
        try{
	    adminManager.setAdminSettings(adminSettings);
	    Util.printMessage("STATUS: FINISHED SAVING of accounts");
	}
	catch(Exception e){ Util.handleExceptionNoRestart("--> ERROR:  problem updating accounts to server", e); }
  
        return null;
    }

    public Vector generateRows(Object configModel) {
        Vector configVector = new Vector();
        java.util.Set configSet = adminManager.getAdminSettings().getUsers();
        Iterator listIterator = configSet.iterator();
        Vector rowVector;
        User newElem;
        int count = 1;
        while(listIterator.hasNext()){
            newElem = (User) listIterator.next();
            rowVector = new Vector();
            rowVector.add( new Integer(count) );
            rowVector.add(super.ROW_SAVED);
            
            rowVector.add( newElem.getName() );
            rowVector.add( newElem.getLogin() );

            /* XXX What is this */
            MPasswordField tempMPasswordField = new MPasswordField( newElem.getPassword() );
            try{

            }catch(Exception e){}
            rowVector.add( tempMPasswordField );
            rowVector.add( new MPasswordField() );
            rowVector.add( new MPasswordField() );
	    MPasswordField originalMPasswordField = new MPasswordField();
	    originalMPasswordField.setGeneratesChangeEvent(false);
            rowVector.add( originalMPasswordField );
            rowVector.add( newElem.getEmail() );
            rowVector.add( new Boolean(newElem.getSendAlerts()) );
            
            configVector.add(rowVector);
            count++;
        }
        
        return configVector;
    }





}
