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
 * Class representing the "RCPT TO:&lt;X>" Command.
 * <br>
 * Understands a null address as a recipient, although
 * this is semantically nonsense as-per SMTP.
 * <br>
 * If we are to understand the ESMTP commands which modify
 * the RCPT line, this class must be modified.
 */
public class RCPTCommand extends CommandWithEmailAddress 
{

    public RCPTCommand(String cmd, String argStr) 
    	throws ParseException, FatalMailParseException 
    {
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
