package com.metavize.tran.ids.options;

import java.util.regex.*;

//import org.apache.log4j.Logger;
import com.metavize.tran.ids.IDSRuleSignature;

/**
 * This class matches the reference option found in snort based rule signatures.
 *
 * @Author Nick Childers
 */

public class ReferenceOption extends IDSOption {

    //private static final Logger logger = Logger.getLogger(ReferenceOption.class);

    private static final Pattern URLP = Pattern.compile("url,", Pattern.CASE_INSENSITIVE);

    public ReferenceOption(IDSRuleSignature signature, String params) {
        super(signature, params);

        //logger.debug("reference option: " + params);
        Matcher urlm = URLP.matcher(params);
        if (true == urlm.find()) {
            String url = "http://" + params.substring(urlm.end()).trim();
            //logger.debug("reference option url: " + url);
            signature.setURL(url);
        }
    }
}
