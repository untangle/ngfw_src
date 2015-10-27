/*
 * $Id: SslInspectorSettings.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.node.ssl_inspector;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import org.json.JSONString;
import org.json.JSONObject;

@SuppressWarnings("serial")
public class SslInspectorSettings implements Serializable
{
    private LinkedList<SslInspectorRule> ignoreList;
    private boolean processEncryptedMailTraffic;
    private boolean processEncryptedWebTraffic;
    private boolean blockInvalidTraffic;
    private boolean serverFakeHostname;
    private boolean serverBlindTrust;
    private boolean javaxDebug;
    private boolean enabled;

    // constructors -----------------------------------------------------------

    public SslInspectorSettings()
    {
        ignoreList = new LinkedList<SslInspectorRule>();
        processEncryptedMailTraffic = true;
        processEncryptedWebTraffic = true;
        blockInvalidTraffic = false;
        serverFakeHostname = true;
        serverBlindTrust = false;
        javaxDebug = false;
        enabled = true;
    }

    // accessors --------------------------------------------------------------

    public LinkedList<SslInspectorRule> getIgnoreRules()
    {
        return (ignoreList);
    }

    public void setIgnoreRules(LinkedList<SslInspectorRule> ignoreList)
    {
        this.ignoreList = ignoreList;
        int count = 1;

        Iterator<SslInspectorRule> iterator = ignoreList.iterator();
        while (iterator.hasNext()) {
            SslInspectorRule entry = iterator.next();
            entry.setRuleId(count);
            count += 1;
        }
    }

    public boolean getProcessEncryptedMailTraffic()
    {
        return processEncryptedMailTraffic;
    }

    public void setProcessEncryptedMailTraffic(boolean flag)
    {
        this.processEncryptedMailTraffic = flag;
    }

    public boolean getProcessEncryptedWebTraffic()
    {
        return processEncryptedWebTraffic;
    }

    public void setProcessEncryptedWebTraffic(boolean flag)
    {
        this.processEncryptedWebTraffic = flag;
    }

    public boolean getBlockInvalidTraffic()
    {
        return blockInvalidTraffic;
    }

    public void setBlockInvalidTraffic(boolean flag)
    {
        this.blockInvalidTraffic = flag;
    }

    public boolean getServerFakeHostname()
    {
        return serverFakeHostname;
    }

    public void setServerFakeHostname(boolean flag)
    {
        this.serverFakeHostname = flag;
    }

    public boolean getServerBlindTrust()
    {
        return serverBlindTrust;
    }

    public void setServerBlindTrust(boolean flag)
    {
        this.serverBlindTrust = flag;
    }

    public boolean getJavaxDebug()
    {
        return javaxDebug;
    }

    public void setJavaxDebug(boolean flag)
    {
        this.javaxDebug = flag;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean flag)
    {
        this.enabled = flag;
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
