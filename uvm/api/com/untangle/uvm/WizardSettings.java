/**
 * $Id: WizardSettings.java,v 1.00 2014/12/11 13:59:41 dmorris Exp $
 */
package com.untangle.uvm;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Wizard settings.
 */
@SuppressWarnings("serial")
public class WizardSettings implements Serializable, JSONString
{
    private boolean wizardComplete = false;
    /**
     * if passwordRequired == true, then a password will be required
     * on the *even* if the wizard has not been completed.
     * if passwordRequired == false, then the user will be automatically logged
     * into the setup wizard the first time
     */
    private boolean passwordRequired = false;
    private String completedStep;

    /**
     * XXX DEPRECATED
     * Can be removed after 14.0
     */
    private String[] steps = new String[] {
        "Welcome",
        "ServerSettings",
        "Interfaces",
        "Internet",
        "InternalNetwork",
        "AutoUpgrades",
        "Complete"
    };

    public WizardSettings() { }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public String[] getSteps() { return steps; }
    public void setSteps( String[] newValue ) { this.steps = newValue; }

    public boolean getWizardComplete() { return wizardComplete; }
    public void setWizardComplete( boolean newValue ) { this.wizardComplete = newValue; }

    public boolean getPasswordRequired() { return passwordRequired; }
    public void setPasswordRequired( boolean newValue ) { this.passwordRequired = newValue; }
    
    public String getCompletedStep() { return this.completedStep; }
    public void setCompletedStep( String newValue ) { this.completedStep = newValue; }

}
