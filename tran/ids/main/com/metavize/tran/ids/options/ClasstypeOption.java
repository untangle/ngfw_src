package com.metavize.tran.ids.options;

import java.nio.ByteBuffer;

import com.metavize.tran.ids.IDSRuleSignature;
import com.metavize.mvvm.tapi.event.*;

public class ClasstypeOption extends IDSOption {
    private final String[] HIGH_PRIORITY = {
        "attempted-admin","attempted-user","shellcode-detect","successful-admin",
        "sucessful-user","trojan-activity","unsuccessful-user","web-application-attack"
    };
	
    private final String[] MEDIUM_PRIORITY = {
        "attempted-dos","attempted-recon","bad-unknown","denial-of-service","misc-attack",
        "non-standard-protocol","rpc-portmap-decode","successful-dos","successful-recon-largescale",
        "successful-recon-limited","suspicious-filename-detect","suspicious-login","system-call-detect",
        "unusual-client-port-connection","web-application-activity" 
    };

    public ClasstypeOption(IDSRuleSignature signature, String params, boolean initializeSettingsTime) {
        super(signature, params);
        if (initializeSettingsTime) {
            for(String str : HIGH_PRIORITY) {
                if(str.equalsIgnoreCase(params)) {
                    signature.rule().setLog(true);
                    return;
                }
            }
            for(String str : MEDIUM_PRIORITY) {
                if(str.equalsIgnoreCase(params)) {
                    signature.rule().setLog(true);
                    return;
                }
            }
        }
    }
}
