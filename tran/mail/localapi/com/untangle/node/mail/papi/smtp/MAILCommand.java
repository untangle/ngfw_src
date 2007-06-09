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
 * Class representing the "MAIL FROM:&lt;X>" Command.
 * <br>
 * <b>If we are to understand the ESMTP commands which modify
 * the MAIL line, this class must be modified.</b>
 */
public class MAILCommand
    extends CommandWithEmailAddress {


    /**
     * Construct a MAIL command from the given
     * arguments.
     *
     * @param cmdStr the string ("MAIL" or some
     *        with mixed case equivilant)
     * @param argStr.  The argument String.  Should
     *        be in the form "FROM:&lt;X>" where "X"
     *        must be a parsable address or blank.
     */
    public MAILCommand(String cmd,
                       String argStr) throws ParseException {

        super(CommandType.MAIL, cmd, argStr);

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
            if(argStrLower.startsWith("from:")) {
                argStr = argStr.substring(5);
            }
            else if(argStrLower.startsWith("from")) {
                argStr = argStr.substring(4);
            }
            assignFromWire(argStr);
        }
    }

    /**
     * Constructs a valid MAIL command with the
     * given address.  If the passed-in address
     * is null, then the output string will
     * become "MAIL FROM:<>".
     */
    public MAILCommand(EmailAddress addr)
        throws ParseException {
        super(CommandType.MAIL, "MAIL", null);

        setEsmtpExtra(null);
        if(addr == null) {
            addr = EmailAddress.NULL_ADDRESS;
        }
        setAddress(addr);
    }

    protected String getArgStrPrefix() {
        return "FROM:";
    }





    /*

    //TESTING CODE

    public static void main(String[] args)
    throws Exception {
    String[] tests = new String[] {
    "FROM:foo@moo.com",
    "FROM:<foo@moo.com",
    "FROM:foo@moo.com>",
    "FROM:<foo@moo.com>",
    "FROM:<>",
    "FROM: foo@moo.com",
    "FROM: <foo@moo.com",
    "FROM: foo@moo.com>",
    "FROM: <foo@moo.com>",
    "FROM: <>",
    "from foo@moo.com",
    "from <foo@moo.com",
    "from foo@moo.com>",
    "from <foo@moo.com>",
    "from <>",
    "fromfoo@moo.com",
    "from<foo@moo.com",
    "fromfoo@moo.com>",
    "from<foo@moo.com>",
    "from<>"
    };
    for(String s : tests) {
    System.out.println("Address: " +s);
    System.out.println("   Became \"" + new MAILCommand("MAIL", s).getAddress() + "\"");
    }
    }
    */
}
