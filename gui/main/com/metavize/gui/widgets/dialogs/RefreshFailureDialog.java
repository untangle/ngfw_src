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
final public class RefreshFailureDialog extends MOneButtonJDialog {
    
    public RefreshFailureDialog(String applianceName) {
        this.setTitle(applianceName + " Warning");
        messageJLabel.setText("<html><center>" + applianceName + " was unable to properly refresh all settings.<br>Please try again later.</center></html>");
        this.setVisible(true);
    }
    
}
