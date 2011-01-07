/* $HeadURL$ */
package com.untangle.uvm.networking;

class ShellFlags
{
    static final String FILE_RULE_CFG        = System.getProperty( "uvm.conf.dir" ) + "/networking.sh";

    //inside http port open?
    // used by alpaca (iptables script)
    static final String FLAG_HTTP_INTERNAL         = "UVM_ALLOW_IN_HTTP";

    //is outside https open?
    // used by alpaca (iptables script)
    static final String FLAG_HTTPS_EXTERNAL        = "UVM_ALLOW_OUT_HTTPS"; 

    //is https port restricted?
    // used by alpaca (iptables script)
    static final String FLAG_HTTPS_RESTRICTED      = "UVM_ALLOW_OUT_RES"; 

    //is https port restricted to this network?
    // used by alpaca (iptables script)
    static final String FLAG_HTTPS_RESTRICTED_NET  = "UVM_ALLOW_OUT_NET"; 

    //is https port restricted to this network/netmask?
    // used by alpaca (iptables script)
    static final String FLAG_HTTPS_RESTRICTED_MASK = "UVM_ALLOW_OUT_MASK"; 

    // the public URL of Untangle
    // used by mailer.py
    static final String FLAG_PUBLIC_URL = "UVM_PUBLIC_URL"; 

    // https port
    // used by alpaca (iptables script)
    static final String FLAG_EXTERNAL_HTTPS_PORT  = "HTTPS_EXTERNAL_PORT";
    
    // public address (if set)
    // used by alpaca (iptables script)
    static final String FLAG_EXTERNAL_PUBLIC_ADDR       = "HTTPS_PUBLIC_ADDR";

    // public https port (if set)
    // used by alpaca (iptables script)
    static final String FLAG_EXTERNAL_PUBLIC_HTTPS_PORT = "HTTPS_PUBLIC_PORT";

    // is public address enabled 
    // used by alpaca (iptables script)
    static final String FLAG_EXTERNAL_PUBLIC_ENABLED    = "HTTPS_PUBLIC_REDIRECT_EN";
}
