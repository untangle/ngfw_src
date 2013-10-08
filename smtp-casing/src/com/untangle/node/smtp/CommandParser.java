/*
 * $HeadURL: svn://chef/work/src/smtp-casing/src/com/untangle/node/smtp/CommandParser.java $
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

import static com.untangle.node.util.Rfc822Util.consumeLine;
import static com.untangle.node.util.Rfc822Util.consumeToken;
import static com.untangle.node.util.Rfc822Util.eatSpace;

import java.nio.ByteBuffer;

import com.untangle.node.smtp.FatalMailParseException;
import com.untangle.node.token.ParseException;

/**
 * Because of classloader issues this class is public. However, it should really not be used other than in the casing.
 */
public class CommandParser
{

    /**
     * Parse the buffer (which must have a complete line!) into a Command. May return a subclass of Command for Commands
     * with interesting arguments we wish parsed.
     */
    public static Command parse(ByteBuffer buf) throws ParseException, FatalMailParseException
    {
        String cmdStr = consumeToken(buf);
        cmdStr = cmdStr == null ? "" : cmdStr.trim();
        eatSpace(buf);
        String argStr = consumeLine(buf);
        CommandType type = CommandType.fromCode(cmdStr);

        switch (type) {
            case MAIL:
            case RCPT:
                return new CommandWithEmailAddress(type, cmdStr, argStr);
            case AUTH:
                return new AUTHCommand(cmdStr, argStr);
            default:
                return new Command(type, cmdStr, argStr);
        }
    }

    /************** Tests ******************/

    public static String runTest(String[] args) throws Exception
    {
        String crlf = "\r\n";
        String result = "";

        result += testParse("FOO moo" + crlf);
        result += testParse("\r" + crlf);
        result += testParse("FOO" + crlf);
        result += testParse("" + crlf);
        result += CommandParser.parse(ByteBuffer.wrap((" " + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap(("\t" + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" \t" + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap(("\t " + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" \t " + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap(("FOO " + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap(("FOO  " + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap(("FOO\t" + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap(("FOO \t" + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap(("FOO\t " + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap(("FOO \t " + crlf).getBytes())).getCmdString() + "\n";

        result += CommandParser.parse(ByteBuffer.wrap((" FOO" + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO " + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO  " + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO\t" + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO \t" + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO\t " + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO \t " + crlf).getBytes())).getCmdString() + "\n";

        result += CommandParser.parse(ByteBuffer.wrap((" FOO x" + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO  x" + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO\tx" + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO \tx" + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO\t x" + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO \t x" + crlf).getBytes())).getCmdString() + "\n";

        result += CommandParser.parse(ByteBuffer.wrap((" FOO x " + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO x  " + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO x\t" + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO x \t" + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO x\t " + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO x \t " + crlf).getBytes())).getCmdString() + "\n";

        result += CommandParser.parse(ByteBuffer.wrap((" FOO  x " + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO  x  " + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO  x\t" + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO  x \t" + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO  x\t " + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO  x \t " + crlf).getBytes())).getCmdString() + "\n";

        result += CommandParser.parse(ByteBuffer.wrap((" FOO\tx " + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO\tx  " + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO\tx\t" + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO\tx \t" + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO\tx\t " + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO\tx \t " + crlf).getBytes())).getCmdString() + "\n";

        result += CommandParser.parse(ByteBuffer.wrap((" FOO \tx " + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO \tx  " + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO \tx\t" + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO \tx \t" + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO \tx\t " + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO \tx \t " + crlf).getBytes())).getCmdString() + "\n";

        result += CommandParser.parse(ByteBuffer.wrap((" FOO \tx " + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO \tx  " + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO \tx\t" + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO \tx\t " + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO \tx \t" + crlf).getBytes())).getCmdString() + "\n";
        result += CommandParser.parse(ByteBuffer.wrap((" FOO \tx \t " + crlf).getBytes())).getCmdString() + "\n";
        return result;
    }

    private static String testParse(String str) throws Exception
    {
        String result = "\n\n===================\n";
        result += str + "\n";
        Command c = CommandParser.parse(ByteBuffer.wrap(str.getBytes()));
        result += "CMD: " + c.getCmdString() + "\n";
        result += "ARGS: " + c.getArgString() + "\n";
        return result;
    }

}
