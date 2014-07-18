/**
 * $Id$
 */
package com.untangle.node.smtp;

import static com.untangle.node.util.Rfc822Util.consumeLine;
import static com.untangle.node.util.Rfc822Util.consumeToken;
import static com.untangle.node.util.Rfc822Util.eatSpace;

import java.nio.ByteBuffer;

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
    public static Command parse(ByteBuffer buf) throws ParseException
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
