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
import com.untangle.node.smtp.mime.BadEmailAddressFormatException;
import com.untangle.node.smtp.mime.EmailAddress;
import com.untangle.node.token.ParseException;


/**
 * Base class of a Command which holds a parsed
 * EmailAddress (parsed from the arguments).
 */
public abstract class CommandWithEmailAddress
    extends Command {

    private EmailAddress m_address;
    private String m_esmtpExtra;

    protected CommandWithEmailAddress(CommandType type,
                                      String cmdStr,
                                      String argStr) throws ParseException {
        super(type, cmdStr, argStr);
    }

    public final void setAddress(EmailAddress address) {
        m_address = address;
    }

    public final void setEsmtpExtra(String esmtpExtra) {
        m_esmtpExtra = esmtpExtra;
    }

    /**
     * Get any ESMTP directives which followed the
     * address.  These are "unparsed" in that any internal
     * format of the directive is not understood
     * (i.e. something like "SIZE=100")
     */
    public final String getEsmtpExtra() {
        return m_esmtpExtra;
    }

    /**
     * Get the EmailAddress parsed from this Command.
     */
    public final EmailAddress getAddress() {
        return m_address;
    }

    protected abstract String getArgStrPrefix();

    @Override
    public String getArgString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getArgStrPrefix());
        sb.append(getAddress().toSMTPString());
        if(getEsmtpExtra() != null) {
            if(!getEsmtpExtra().startsWith(" ")) {
                sb.append(" ");
            }
            sb.append(getEsmtpExtra());
        }
        return sb.toString();
    }

    /**
     * Helper for subclasses, which have already stripped-off
     * the "TO" or "FROM"
     */
    protected void assignFromWire(String str)
        throws ParseException, FatalMailParseException {
        EmailAddressAndExtra ewe = parseAddressAndExtra(str);
        setAddress(ewe.addr);
        setEsmtpExtra(ewe.extra);
    }

    /**
     * Parses an email address (which is assumed to begin at str index 0)
     * and any extra ESMTP junk at the end
     */
    protected static EmailAddressAndExtra parseAddressAndExtra(String str)
        throws ParseException, FatalMailParseException {
        if(str == null) {
            return new EmailAddressAndExtra(EmailAddress.NULL_ADDRESS, null);
        }
        str = str.trim();
        int addrEnd = getAddressEnd(str);

        if(addrEnd == str.length()-1) {
            //No ESMTP junk
            return new EmailAddressAndExtra(parseAddress(str), null);
        }
        String addrString = str.substring(0, addrEnd+1);
        String esmtpString = str.substring(addrEnd+1, str.length());

        return new EmailAddressAndExtra(parseAddress(addrString), esmtpString);
    }

    private static class EmailAddressAndExtra {
        final EmailAddress addr;
        final String extra;

        EmailAddressAndExtra(EmailAddress addr,
                             String extra) {
            this.addr = addr;
            this.extra = extra;
        }

    }


    /**
     * Helper method to parse an address read from
     * a Command argument.  Subclasses using this method
     * must tokenize away any leading stuff ("FROM:", "TO:")
     * as well as any ESMTP trailing tokens.  In other words,
     * this should be one and only one address.
     * <br>
     * Leading spaces are trimmed.
     * <br>
     * Will never return null.
     */
    private static EmailAddress parseAddress(String str)
        throws FatalMailParseException {
        if(str == null) {
            return EmailAddress.NULL_ADDRESS;
        }
        str = str.trim();
        if("".equals(str.trim()) || "<>".equals(str)) {
            return EmailAddress.NULL_ADDRESS;
        }
        try {
            return EmailAddress.parse(str);
        }
        catch(BadEmailAddressFormatException ex) {
            throw new FatalMailParseException("could not parse email address: " + str, ex);
        }
    }



    //For this quick-hack, I'm making the following assumptions:
    //
    // 1) If I see "<" and ">" in non-quoted portions, then
    //    there is an address bounded by "<>" within the address
    // 2) If not, then the whole line is an address
    /**
     * Returns the index of the last valid character in the
     * <code>addr</code> string from the email address (i.e.
     * inclusive).
     */
    private static int getAddressEnd(String addr) {
        byte[] bytes = addr.getBytes();

        boolean inQuote = false;
        boolean foundOpen = false;

        for(int i = 0; i<bytes.length; i++) {
            if(bytes[i] == '<' && !inQuote) {
                foundOpen = true;
                continue;
            }
            if(bytes[i] == '>' && !inQuote && foundOpen) {
                return i;
            }
            if(bytes[i] == '\"') {
                if(i != 0 && bytes[i-1] == '\'') {
                    continue;
                }
                inQuote = !inQuote;
            }
        }
        return bytes.length - 1;
    }

    protected static String extractAddress(String str) {
        return str.substring(0, getAddressEnd(str) + 1);
    }


    /*
    //TESTING CODE

    public static void main(String[] args)
    throws Exception {

    String[] boundedTest = new String[] {
    "foo",
    "<foo",
    "foo>",
    "<foo>",
    "\"doo\"<foo>",
    "\"doo<foo>",
    "\"doo<foo\">",
    "\"doo\"<foo> ABCXYZ FOO",
    "<foo> ABC123 XYZ"
    };

    for(String s : boundedTest) {
    System.out.println("String: " + s);
    System.out.println("   extractAddress: " + extractAddress(s));
    }

    String[] tests = new String[] {
    "foo@moo.com",
    "<foo@moo.com",
    "foo@moo.com>",
    "<foo@moo.com>",
    "<>"
    };
    for(String s : tests) {
    System.out.println("Address: " + s);
    System.out.println("   Became \"" + parseAddress(s) + "\"");
    }
    }
    */

}
