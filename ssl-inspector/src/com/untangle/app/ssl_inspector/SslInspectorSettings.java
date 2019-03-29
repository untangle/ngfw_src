/**
 * $Id: SslInspectorSettings.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.app.ssl_inspector;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;

import org.json.JSONString;
import org.json.JSONObject;

/**
 * This is the implementation of the ssl inspector settings.
 * 
 * @author mahotz
 * 
 */
@SuppressWarnings("serial")
public class SslInspectorSettings implements Serializable, JSONString
{
    private Integer version;

    private LinkedList<SslInspectorRule> ignoreList;
    private boolean processEncryptedMailTraffic;
    private boolean processEncryptedWebTraffic;
    private boolean blockInvalidTraffic;
    private boolean serverBlindTrust;
    private boolean javaxDebug;
    private boolean enabled;

    private boolean client_SSLv2Hello;
    private boolean client_SSLv3;
    private boolean client_TLSv10;
    private boolean client_TLSv11;
    private boolean client_TLSv12;

    private boolean server_SSLv2Hello;
    private boolean server_SSLv3;
    private boolean server_TLSv10;
    private boolean server_TLSv11;
    private boolean server_TLSv12;

    public SslInspectorSettings()
    {
        ignoreList = new LinkedList<>();
        processEncryptedMailTraffic = false;
        processEncryptedWebTraffic = true;
        blockInvalidTraffic = false;
        serverBlindTrust = false;
        javaxDebug = false;
        enabled = true;

        client_SSLv2Hello = false;
        client_SSLv3 = false;
        client_TLSv10 = true;
        client_TLSv11 = true;
        client_TLSv12 = true;

        server_SSLv2Hello = false;
        server_SSLv3 = false;
        server_TLSv10 = true;
        server_TLSv11 = true;
        server_TLSv12 = true;
    }

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

    // THIS IS FOR ECLIPSE - @formatter:off

    public Integer getVersion() { return (version); }
    public void setVersion(Integer value) { this.version = value; }

    public boolean getProcessEncryptedMailTraffic() { return (processEncryptedMailTraffic); }
    public void setProcessEncryptedMailTraffic(boolean flag) { this.processEncryptedMailTraffic = flag; }

    public boolean getProcessEncryptedWebTraffic() { return (processEncryptedWebTraffic); }
    public void setProcessEncryptedWebTraffic(boolean flag) { this.processEncryptedWebTraffic = flag; }

    public boolean getBlockInvalidTraffic() { return (blockInvalidTraffic); }
    public void setBlockInvalidTraffic(boolean flag) { this.blockInvalidTraffic = flag; }

    public boolean getServerBlindTrust() { return (serverBlindTrust); }
    public void setServerBlindTrust(boolean flag) { this.serverBlindTrust = flag; }

    public boolean getJavaxDebug() { return (javaxDebug); }
    public void setJavaxDebug(boolean flag) { this.javaxDebug = flag; }

    public boolean isEnabled() { return (enabled); }
    public void setEnabled(boolean flag) { this.enabled = flag; }

    // ----- client side protocols ----- client side protocols -----

    public boolean getClient_SSLv2Hello() { return (client_SSLv2Hello); }
    public void setClient_SSLv2Hello(boolean flag) { this.client_SSLv2Hello = flag; }

    public boolean getClient_SSLv3() { return (client_SSLv3); }
    public void setClient_SSLv3(boolean flag) { this.client_SSLv3 = flag; }

    public boolean getClient_TLSv10() { return (client_TLSv10); }
    public void setClient_TLSv10(boolean flag) { this.client_TLSv10 = flag; }

    public boolean getClient_TLSv11() { return (client_TLSv11); }
    public void setClient_TLSv11(boolean flag) { this.client_TLSv11 = flag; }

    public boolean getClient_TLSv12() { return (client_TLSv12); }
    public void setClient_TLSv12(boolean flag) { this.client_TLSv12 = flag; }

    // ----- server side protocols ----- server side protocols -----
    
    public boolean getServer_SSLv2Hello() { return (server_SSLv2Hello); }
    public void setServer_SSLv2Hello(boolean flag) { this.server_SSLv2Hello = flag; }

    public boolean getServer_SSLv3() { return (server_SSLv3); }
    public void setServer_SSLv3(boolean flag) { this.server_SSLv3 = flag; }

    public boolean getServer_TLSv10() { return (server_TLSv10); }
    public void setServer_TLSv10(boolean flag) { this.server_TLSv10 = flag; }

    public boolean getServer_TLSv11() { return (server_TLSv11); }
    public void setServer_TLSv11(boolean flag) { this.server_TLSv11 = flag; }

    public boolean getServer_TLSv12() { return (server_TLSv12); }
    public void setServer_TLSv12(boolean flag) { this.server_TLSv12 = flag; }

    // THIS IS FOR ECLIPSE - @formatter:on

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return (jO.toString());
    }
}
