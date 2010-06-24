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

package com.untangle.node.mime;

import static com.untangle.node.util.Ascii.AT_B;
import static com.untangle.node.util.Ascii.CLOSE_PAREN_B;
import static com.untangle.node.util.Ascii.COLON;
import static com.untangle.node.util.Ascii.COMMA;
import static com.untangle.node.util.Ascii.COMMA_B;
import static com.untangle.node.util.Ascii.GT;
import static com.untangle.node.util.Ascii.GT_B;
import static com.untangle.node.util.Ascii.HT;
import static com.untangle.node.util.Ascii.LT_B;
import static com.untangle.node.util.Ascii.QUOTE;
import static com.untangle.node.util.Ascii.SEMI_B;
import static com.untangle.node.util.Ascii.SP;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

//===========================================
// Implementation Note.  We're currently
// leaning on the JavaMail API for the
// heavy lifting (parsing).  We've created
// this wrapper in case we need to move alway
// from JavaMail in the future.
// -wrs 6/05
//===========================================


/**
 * HeaderField containing EmailAddresses.  This is useful for
 * things like "cc" and "To".
 */
public class EmailAddressHeaderField
    extends HeaderField {

    private List<EmailAddress> m_addresses;

    public EmailAddressHeaderField(String name,
                                   LCString lCName) {
        super(name, lCName);
    }
    public EmailAddressHeaderField(String name) {
        super(name);
    }


    /**
     * Get the Address at the given index.
     *
     */
    public EmailAddress getAddress(int index)
        throws IndexOutOfBoundsException {
        if(m_addresses == null ||
           m_addresses.size() <= index) {
            throw new IndexOutOfBoundsException("" + index);
        }
        return m_addresses.get(index);
    }

    /**
     * Returns the number of addresses repesented
     * by this header
     */
    public int size() {
        return m_addresses == null?
            0:
            m_addresses.size();
    }

    /**
     * Iterate over the addressed contained within
     *
     * @return a typed Iterator
     */
    public Iterator<EmailAddress> iterator() {
        ensureList();
        return m_addresses.iterator();
    }

    /**
     * Remove all occurances of EmailAddresses which
     * test equals (true) for the argument.  The argument
     * address itself (instance) need not be contained
     * in this Header
     *
     * @param address the address
     * @return true if one or more were present, and removed.
     */
    public boolean remove(EmailAddress address) {
        ensureList();
        boolean ret = false;
        for(int i = 0; i<m_addresses.size(); i++) {
            if(m_addresses.get(i).equals(address)) {
                m_addresses.remove(i);
                ret = true;
            }
        }
        if(ret) {
            changed();
        }
        return ret;
    }

    /**
     * Removes all EmailAddresses from this header field.
     */
    public void removeAll() {
        if(m_addresses != null && m_addresses.size() > 0) {
            m_addresses.clear();
            changed();
        }
    }

    /**
     * Test if this Header contains any EmailAddresses which
     * Match the argument.
     */
    public boolean contains(EmailAddress address) {
        ensureList();
        for(int i = 0; i<m_addresses.size(); i++) {
            if(m_addresses.get(i).equals(address)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Duplicates are not prevented.
     */
    public void add(EmailAddress address) {
        ensureList();
        m_addresses.add(address);
        changed();
    }


    /**
     * Makes sure List<EmailAddresses> is never null.
     */
    private void ensureList() {
        if(m_addresses == null) {
            m_addresses = new ArrayList<EmailAddress>();
        }
    }

    @Override
    protected void parseStringValue()
        throws HeaderParseException {

        m_addresses = EmailAddressHeaderField.parseHeaderLine(getValueAsString());

    }

    @Override
    public void parseLines()
        throws HeaderParseException {

        parseStringValue();
    }

    /**
     * Really only for debugging, not to produce output suitable
     * for output.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        sb.append(":");
        Iterator<EmailAddress> it = iterator();
        boolean first = true;
        while(it.hasNext()) {
            if(first) {
                first = false;
            }
            else {
                sb.append(",");
            }
            sb.append(it.next().toMIMEString());
        }
        return sb.toString();
    }

    @Override
    public void writeToAssemble(MIMEOutputStream out)
        throws IOException {

        out.write(getName());
        out.write((byte) COLON);

        int written = getName().length()+1;

        boolean first = true;
        for(EmailAddress address : m_addresses) {
            if(address.isNullAddress()) {
                continue;
            }
            String addrStr = address.toMIMEString();
            if(first) {
                out.write(addrStr);
                written+=addrStr.length();
                first = false;
            }
            else {
                out.write((byte) COMMA);
                out.write((byte) SP);
                if(written + addrStr.length() > 76) {
                    out.writeLine();
                    out.write((byte) HT);
                    written = 0;
                }
                out.write(addrStr);
                written+=addrStr.length();
            }
        }
        out.writeLine();
    }

    public static List<EmailAddress> parseHeaderLine(String line)
        throws HeaderParseException {
        return parseHeaderLine(line, true);
    }

    /**
     * Parse a raw header line into a collection of EmailAddresses,
     * as per RFC821 and its successors.
     *
     * @param line a line containing addresses
     * @param skipBadAddresses if true, bad addresses will be skipped (no exceptions thrown)
     * @return a Listof EmailAddresses.
     */
    @SuppressWarnings("unused")
    public static List<EmailAddress> parseHeaderLine(String line,
                                                     boolean skipBadAddresses)
        throws HeaderParseException {

        Logger logger = Logger.getLogger(EmailAddressHeaderField.class);

        //TODO bscott See bug 808.  This isn't really "fixed".  It
        //still cannot handle <foo:;> which is legal.

        List<EmailAddress> ret = new ArrayList<EmailAddress>();

        HeaderFieldTokenizer tokenizer = new HeaderFieldTokenizer(line);

        HeaderFieldTokenizer.Token t = null;
        StringBuilder sb = new StringBuilder();
        boolean sawGT = false;
        boolean sawAt = false;
        boolean sawLT = false;
        do {
            t = tokenizer.nextTokenWithComments();

            if(t == null) {
                //process last bits, if there were any
                if(sb.length() == 0) {
                    //Nothing to see here
                    continue;
                }
                //For now, I'll assume we can parse a personal only?
                if(sawLT && !sawGT) {
                    sb.append(GT);
                }
                String addrStr = sb.toString().trim();
                if("".equals(addrStr) || "<>".equals(addrStr)) {
                    continue;
                }
                try {
                    ret.add(EmailAddress.parse(sb.toString()));
                }
                catch(Exception ex) {
                    HeaderParseException hpe = new HeaderParseException("Unable to parse \"" +
                                                                        sb.toString() + "\" into address from header line \"" +
                                                                        line + "\"", ex);
                    if(skipBadAddresses) {
                        logger.warn(hpe.fillInStackTrace());
                    }
                    else {
                        throw hpe;
                    }
                }
                sb = new StringBuilder();
                sawGT = false;
                sawAt = false;
                sawLT = false;
                break;
            }
            //If we're in a comment, simply append blindly
            if(tokenizer.openCommentCount() > 0 ||
               t.getDelim() == CLOSE_PAREN_B) {
                t.appendTo(sb);
                continue;
            }
            //Look for ending delims
            if(t.getDelim() == SEMI_B || t.getDelim() == COMMA_B) {
                //Separator
                if(sb.length() == 0) {
                    //Nothing to see here
                    continue;
                }
                //For now, I'll assume we can parse a personal only?
                if(sawLT && !sawGT) {
                    sb.append(GT);
                }
                String addrStr = sb.toString().trim();
                if("".equals(addrStr) || "<>".equals(addrStr)) {
                    continue;
                }
                try {
                    ret.add(EmailAddress.parse(sb.toString()));
                }
                catch(Exception ex) {
                    HeaderParseException hpe = new HeaderParseException("Unable to parse \"" +
                                                                        sb.toString() + "\" into address from header line \"" +
                                                                        line + "\"", ex);
                    if(skipBadAddresses) {
                        logger.warn(hpe.fillInStackTrace());
                    }
                    else {
                        throw hpe;
                    }
                }
                sb = new StringBuilder();
                sawGT = false;
                sawAt = false;
                sawLT = false;
                continue;
            }
            if(t.isDelim()) {
                if(t.getDelim() == GT_B) {
                    sawGT = true;
                }
                if(t.getDelim() == LT_B) {
                    sawLT = true;
                }
                if(t.getDelim() == AT_B) {
                    sawAt = true;
                }
                t.appendTo(sb);
            }
            else {
                boolean wasQuote = t.getType() == HeaderFieldTokenizer.TokenType.QTEXT;
                if(wasQuote) {
                    sb.append(QUOTE);
                }
                t.appendTo(sb);
                if(wasQuote) {
                    sb.append(QUOTE);
                }
            }

        }while(t != null);

        return ret;
    }

    public static void main(String[] args) throws Exception {
        if(args.length < 1) {
            testAddress("foo moo<foo moo>");
            testAddress("\"foo moo\"");
            testAddress("foo moo");
            testAddress("Linus Torvalds; linux-kernel@vger.kernel.org");
            testAddress("\"Davda, Bhavesh P \\(Bhavesh\\)\" <bhavesh@avaya.com>");
            testAddress("\"Andi Kleen\" <ak@suse.de>, \"Brown, Len\" <len.brown@intel.com>,");
            testAddress("Bill Scott<bscott@untangle.com>");
            testAddress(";;Bill Scott<bscott@untangle.com>");
            testAddress(",,Bill Scott<bscott@untangle.com>");
            testAddress(", Bill Scott<bscott@untangle.com>");
            testAddress(", ; Bill Scott<bscott@untangle.com>");
            testAddress(",;Bill Scott<bscott@untangle.com>");
            testAddress("Bill Scott<bscott@untangle.com>,");
            testAddress(";;Bill Scott<bscott@untangle.com> ,");
            testAddress(",,Bill Scott<bscott@untangle.com> ;");
            testAddress(", Bill Scott<bscott@untangle.com>;");
            testAddress(", ; Bill Scott<bscott@untangle.com>;,");
            testAddress(",;Bill Scott<bscott@untangle.com> ,;");

            testAddress("Bill Scott<bscott@untangle.com");
            testAddress(";;Bill Scott<bscott@untangle.com");
            testAddress(",,Bill Scott<bscott@untangle.com");
            testAddress(", Bill Scott<bscott@untangle.com");
            testAddress(", ; Bill Scott<bscott@untangle.com");
            testAddress(",;Bill Scott<bscott@untangle.com");
            testAddress("Bill Scott<bscott@untangle.com,");
            testAddress(";;Bill Scott<bscott@untangle.com ,");
            testAddress(",,Bill Scott<bscott@untangle.com ;");
            testAddress(", Bill Scott<bscott@untangle.com;");
            testAddress(", ; Bill Scott<bscott@untangle.com;,");
            testAddress(",;Bill Scott<bscott@untangle.com ,;");
        }
        else {
            BufferedReader reader = new BufferedReader(new FileReader(args[0]));

            String line = null;

            while((line = reader.readLine()) != null) {
                line = line.trim();
                if("".equals(line)) {
                    continue;
                }
                testAddress(line);
            }
        }
    }

    //Reads the named file and parses each line.
    private static void testAddress(String line) {
        System.out.println("=====================================");
        System.out.println(line);
        System.out.println("-----------------------------");
        try {
            List<EmailAddress> list = parseHeaderLine(line, false);
            boolean first = true;
            for(EmailAddress addr : list) {
                if(first) {
                    first = false;
                }
                else {
                    System.out.print(", ");
                }
                System.out.print(addr.toMIMEString());
            }
            System.out.println("");

        }
        catch(Exception ex) {
            System.out.println("***EXCEPTION***");
            System.err.println("***EXCEPTION***");
            ex.printStackTrace(System.out);
        }
        System.out.println("-----------------------------");
    }



}
