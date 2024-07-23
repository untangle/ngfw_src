/**
 * $Id$
 */
package com.untangle.app.ftp;

import java.io.Serializable;
import org.json.JSONString;
import com.untangle.uvm.util.ValidSerializable;
import org.json.JSONObject;

/**
 * Ftp casing settings.
 */
@SuppressWarnings("serial")
@ValidSerializable
public class FtpSettings implements Serializable, JSONString
{
    private boolean enabled = true;

    public FtpSettings() { }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
    
}
