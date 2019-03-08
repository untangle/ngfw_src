/**
 * $Id$
 */
package com.untangle.app.smtp;

import java.nio.ByteBuffer;

import com.untangle.uvm.util.AsciiUtil;
import com.untangle.uvm.util.BufferUtil;

/**
 * Because of classloader issues this class is public. However, it should really not be used other than in the casing.
 */
public class CommandParser
{
    public static final char SP = ' ';
    public static final char HTAB = '\t';
    public static final char CR = '\r';

    /**
     * Parse the buffer (which must have a complete line!) into a Command. May return a subclass of Command for Commands
     * with interesting arguments we wish parsed.
     *
     * @param buf ByteBuffer to initialize command parser.
     * @return Instance of Command.
     */
    public static Command parse(ByteBuffer buf)
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

    /**
     * Consumes contiguous whitespace.
     *
     * @param buf buffer.
     * @return true if whitespace was consumed.
     */
    public static boolean eatSpace(ByteBuffer buf)
    {
        if (buf.hasRemaining() && isWhitespace(buf.get(buf.position()))) {
            while (buf.hasRemaining()) {
                if (!isWhitespace(buf.get())) {
                    buf.position(buf.position() - 1);
                    break;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Consumes from the point, until but not including the CRLF.
     *
     * @param buf buffer with position
     * @return a <code>String</code> value
     */
    public static String consumeLine( ByteBuffer buf )
    {
        int index = BufferUtil.findCrLf(buf);
        if(index < 0) {
            throw new RuntimeException("No Line terminator in \"" + AsciiUtil.bbToString(buf) + "\"");
        }
        ByteBuffer dup = buf.duplicate();
        dup.limit(index);
        buf.position(index+2);
        return AsciiUtil.bbToString(dup);
    }

    /**
     * Consumes the next token from the buffer.
     *
     * @param buf buffer with point on the first character of the token.
     * @return the token.
     */
    public static String consumeToken(ByteBuffer buf)
    {
        StringBuilder sb = new StringBuilder();
        while (buf.hasRemaining()) {
            char c = (char)buf.get();
            if (CR == c || isWhitespace(c)) {
                buf.position(buf.position() - 1);
                break;
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * Whitespace according to RFC 2822 2.2.2 (SP, HTAB).
     *
     * @param c a <code>char</code> value
     * @return a <code>boolean</code> value
     */
    public static boolean isWhitespace(char c)
    {
        return SP == c || HTAB == c;
    }

    /**
     * Determines if byte is whitespace character.
     *
     * @param  b Byte to check.
     * @return   true if whitespace, false otherwise.
     */
    public static boolean isWhitespace(byte b)
    {
        return isWhitespace((char)b);
    }

    /**
     * Determines if character is a spacial character.
     *
     * @param  c Character to check.
     * @return   true if special, false otherwise.
     */
    public static boolean isTspecial(char c)
    {
        switch (c) {
        case '(': case ')': case '<': case '>': case '@':
        case ',': case ';': case ':': case '\\': case '"':
        case '/': case '[': case ']': case '?': case '=':
            return true;
        default:
            return false;
        }
    }
    
    /************** Tests ******************/

    /**
     * Run tests for class.
     * 
     * @param  args      Unused.
     * @return           String of result.
     * @throws Exception If any problems encountered.
     */
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

    /**
     * Test parsing of a string.
     * @param  str       String to parse.
     * @return           String of result.
     * @throws Exception If any problems
     */
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
