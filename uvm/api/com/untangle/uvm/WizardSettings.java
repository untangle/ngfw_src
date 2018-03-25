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
    private String completedStep;
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
    
    public String getCompletedStep() { return this.completedStep; }
    public void setCompletedStep( String newValue ) { this.completedStep = newValue; }

}
