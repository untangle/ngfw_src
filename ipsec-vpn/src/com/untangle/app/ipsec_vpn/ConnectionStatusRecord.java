/**
 * $Id: ConnectionStatusRecord.java 37267 2014-02-26 23:42:19Z dmorris $
 */

package com.untangle.app.ipsec_vpn;

import java.io.Serializable;
import org.json.JSONObject;
import org.json.JSONString;

/**
 * This record is used in several different ways, and for each use only some of
 * the fields are populated. The ipsec-status script returns ip xfrm state and
 * policy records. The Java code previously parsed that data to build the list
 * of tunnel status records displayed in the interface. The code has since been
 * updated to use the new ipsec-tunnel-status script which parses the output of
 * "ipsec statusall tunnel_name" to return the tunnel state, traffic counts, and
 * local and remote endpoints.
 */

// THIS IS FOR ECLIPSE - @formatter:off

/*
 * type:"STATE" = src, dst, proto, mode, reqid, auth, spi, enc, flag, replayWindow, authKey, encKey, selSrc, selDst
 * type:"POLICY" = src, dst, proto, mode, reqid, ptype, dir, priority, tmplSrc, tmplDst
 * type:"DISPLAY" = src (local ip), dst (remote host), tmplSrc (local network), tmplDst (remote network), proto (description), mode (active / inactive) 
 */

// THIS IS FOR ECLIPSE - @formatter:onn

@SuppressWarnings("serial")
public class ConnectionStatusRecord implements JSONString, Serializable
{
    private String id;
    private String description;
    private String type;
    private String src;
    private String dst;
    private String proto;
    private String flag;
    private String auth;
    private String spi;
    private String reqid;
    private String enc;
    private String mode;
    private String dir;
    private String ptype;
    private String priority;
    private String authKey;
    private String encKey;
    private String replayWindow;
    private String tmplSrc;
    private String tmplDst;
    private String selSrc;
    private String selDst;
    private String inBytes;
    private String outBytes;

    public ConnectionStatusRecord()
    {
    }

    // THIS IS FOR ECLIPSE - @formatter:off

    public String getId() { return(id); }
    public void setId(String id) { this.id = id; }

    public String getDescription() { return(description); }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return(type); }
    public void setType(String type) { this.type = type; }
    
    public String getSrc() { return(src); }
    public void setSrc(String src) { this.src = src; }
    
    public String getDst() { return(dst); }
    public void setDst(String dst) { this.dst = dst; }

    public String getProto() { return(proto); }
    public void setProto(String proto) { this.proto = proto; }

    public String getFlag() { return(flag); }
    public void setFlag(String flag) { this.flag = flag; }

    public String getAuth() { return(auth); }
    public void setAuth(String auth) { this.auth = auth; }

    public String getSpi() { return(spi); }
    public void setSpi(String spi) { this.spi = spi; }

    public String getReqid() { return(reqid); }
    public void setReqid(String reqid) { this.reqid = reqid; }

    public String getEnc() { return(enc); }
    public void setEnc(String enc) { this.enc = enc; }

    public String getMode() { return(mode); }
    public void setMode(String mode) { this.mode = mode; }

    public String getDir() { return(dir); }
    public void setDir(String dir) { this.dir = dir; }

    public String getPtype() { return(ptype); }
    public void setPtype(String ptype) { this.ptype = ptype; }

    public String getPriority() { return(priority); }
    public void setPriority(String priority) { this.priority = priority; }

    public String getAuthKey() { return(authKey); }
    public void setAuthKey(String authKey) { this.authKey = authKey; }

    public String getEncKey() { return(encKey); }
    public void setEncKey(String encKey) { this.encKey = encKey; }

    public String getReplayWindow() { return(replayWindow); }
    public void setReplayWindow(String replayWindow) { this.replayWindow = replayWindow; }

    public String getTmplSrc() { return(tmplSrc); }
    public void setTmplSrc(String tmplSrc) { this.tmplSrc = tmplSrc; }

    public String getTmplDst() { return(tmplDst); }
    public void setTmplDst(String tmplDst) { this.tmplDst = tmplDst; }

    public String getSelSrc() { return(selSrc); }
    public void setSelSrc(String selSrc) { this.selSrc = selSrc; }

    public String getSelDst() { return(selDst); }
    public void setSelDst(String selDst) { this.selDst = selDst; }
    
    public String getInBytes() { return(inBytes); }
    public void setInBytes(String inBytes) { this.inBytes = inBytes; }
    
    public String getOutBytes() { return(outBytes); }
    public void setOutBytes(String outBytes) { this.outBytes = outBytes; }

    /*
    * Use the id and description to create a unique connection name that
    * won't cause problems in the ipsec.conf file by replacing non-word
    * characters with a hyphen. We also prefix this name with UT123_ to
    * ensure no dupes in the config file.
    *
    * Moved this into the ConnectionStatusRecord class as a public getter to be consistent
    */
    public String getWorkName() { return "UT" + this.id + "_" + this.description.replaceAll("\\W", "-"); }
    
    // THIS IS FOR ECLIPSE - @formatter:on

    public String toString()
    {
        String local = "";

        local += " type:" + type;
        local += " src:" + src;
        local += " dst:" + dst;
        local += " proto:" + proto;
        local += " flag:" + flag;
        local += " auth:" + auth;
        local += " spi:" + spi;
        local += " reqid:" + reqid;
        local += " enc:" + enc;
        local += " mode:" + mode;
        local += " dir:" + dir;
        local += " ptype:" + ptype;
        local += " priority:" + priority;
        local += " authKey:" + authKey;
        local += " encKey:" + encKey;
        local += " replayWindow:" + replayWindow;
        local += " tmplSrc:" + tmplSrc;
        local += " tmplDst:" + tmplDst;
        local += " selSrc:" + selSrc;
        local += " selDst:" + selDst;
        local += " inBytes:" + inBytes;
        local += " outBytes:" + outBytes;

        return (local);
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
