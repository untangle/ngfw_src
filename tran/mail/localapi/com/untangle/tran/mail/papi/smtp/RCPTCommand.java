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

package com.untangle.tran.mail.papi.smtp;

import static com.untangle.tran.util.Rfc822Util.*;
import static com.untangle.tran.util.Ascii.*;


import com.untangle.tran.mime.*;
import com.untangle.tran.token.ParseException;

/**
 * Class representing the "RCPT TO:&lt;X>" Command.
 * <br>
 * Understands a null address as a recipient, although
 * this is semantically nonsense as-per SMTP.
 * <br>
 * If we are to understand the ESMTP commands which modify
 * the RCPT line, this class must be modified.
 */
public class RCPTCommand
    extends CommandWithEmailAddress {

    private static final String NULL_TO_STR = "TO:<>";

    public RCPTCommand(String cmd,
                       String argStr) throws ParseException {

        super(CommandType.RCPT, cmd, argStr);

        if(argStr == null) {
            setAddress(EmailAddress.NULL_ADDRESS);
        }
        argStr = argStr.trim();

        if(argStr.length() == 0 || "<>".equals(argStr)) {
            //TODO bscott What should we do?  Fix this up?
            setAddress(EmailAddress.NULL_ADDRESS);
        }
        else {
            //Strip-off the "from" if found
            //TODO bscott  This is a hack
            String argStrLower = argStr.toLowerCase();
            if(argStrLower.startsWith("to:")) {
                argStr = argStr.substring(3);
            }
            else if(argStrLower.startsWith("to")) {
                argStr = argStr.substring(2);
            }
            assignFromWire(argStr);
        }
    }

    /**
     * Constructs a valid RCPT command with the
     * given address.  If the passed-in address
     * is null, then the output string will
     * become "RCPT TO:<>" (which is nonsense
     * but this class is not intended to enforce
     * protocol semantics, only command format).
     */
    public RCPTCommand(EmailAddress addr)
        throws ParseException {
        super(CommandType.MAIL, "RCPT", null);

        setEsmtpExtra(null);
        if(addr == null) {
            addr = EmailAddress.NULL_ADDRESS;
        }
        setAddress(addr);
    }

    protected String getArgStrPrefix() {
        return "TO:";
    }

}
