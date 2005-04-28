/*
 * ValidateFailureDialog.java
 *
 * Created on April 27, 2005, 2:03 PM
 */

package com.metavize.gui.widgets.dialogs;


/**
 *
 * @author inieves
 */
final public class SaveFailureDialog extends MOneButtonJDialog {
    
    public SaveFailureDialog(String applianceName) {
        this.setTitle(applianceName + " Warning");
        messageJLabel.setText("<html><center>" + applianceName + " was unable to save settings.<br>Please try again later.</center></html>");
        this.setVisible(true);
        this.dispose();
    }
    
}
