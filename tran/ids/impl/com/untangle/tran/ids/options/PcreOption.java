/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.ids.options;

import java.nio.ByteBuffer;
import java.util.regex.*;

import com.untangle.tran.ids.IDSRuleSignature;
import com.untangle.tran.ids.IDSSessionInfo;
import org.apache.log4j.Logger;

public class PcreOption extends IDSOption {

    private final Logger logger = Logger.getLogger(getClass());

    private Pattern pcrePattern;

    public PcreOption(IDSRuleSignature signature, String params) {
        super(signature, params);

        int beginIndex = params.indexOf("/");
        int endIndex = params.lastIndexOf("/");

        if (endIndex < 0 || beginIndex < 0 || endIndex == beginIndex) {
            logger.warn("Malformed pcre: " + params + ", ignoring rule: " +
                        signature.rule().getText());
            signature.remove(true);
        } else {
            try {
                String pattern = params.substring(beginIndex+1, endIndex);
                String options = params.substring(endIndex+1);
                int flag = 0;
                for (int i = 0; i < options.length(); i++) {
                    char c = options.charAt(i);
                    switch (c) {
                    case 'i':
                        flag = flag | Pattern.CASE_INSENSITIVE;
                        break;
                    case 's':
                        flag = flag | Pattern.DOTALL;
                        break;
                    case 'm':
                        flag = flag | Pattern.MULTILINE;
                        break;
                    case 'x':
                        flag = flag | Pattern.COMMENTS;
                        break;
                    default:
                        logger.info("Unable to handle pcre option: " + c + ", ignoring rule: " +
                                    signature.rule().getText());
                        signature.remove(true);
                        break;
                    }
                }
                pcrePattern = Pattern.compile(pattern, flag);
            } catch(Exception e) {
                logger.warn("Unable to parse pcre: " + params + " (" + e.getMessage() + "), ignoring rule: " +
                            signature.rule().getText());
                signature.remove(true);
            }
        }
    }

    public boolean runnable() {
        return true;
    }

    public boolean run(IDSSessionInfo sessionInfo) {
        ByteBuffer eventData = sessionInfo.getEvent().data();
        String data = new String(eventData.array());
    //  if(pcrePattern == null) {
    //      System.out.println("pcrePattern is null\n\n"+getSignature());
    //      return false;
    //  }
        return negationFlag ^ pcrePattern.matcher(data).find();
    }
}
