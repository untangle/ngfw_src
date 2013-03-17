/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.smtp;

import com.untangle.node.smtp.FatalMailParseException;
import com.untangle.node.mime.EmailAddress;
import com.untangle.node.token.ParseException;

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
                       String argStr) throws ParseException, FatalMailParseException {

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
