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

package com.untangle.node.mail.papi;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Kind of Email address
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public class AddressKind implements Serializable
{
    private static final long serialVersionUID = 4520610095955193319L;
    
    public static final AddressKind FROM = new AddressKind('F', "FROM");
    public static final AddressKind TO   = new AddressKind('T', "TO");
    public static final AddressKind CC   = new AddressKind('C', "CC");

    // These only apply to SMTP:
    public static final AddressKind ENVELOPE_FROM = new AddressKind('G', "ENVELOPE_FROM");
    public static final AddressKind ENVELOPE_TO   = new AddressKind('B', "ENVELOPE_TO");

    // These only apply to IMAP/POP3:
    public static final AddressKind USER   = new AddressKind('U', "USER");

    private static final Map INSTANCES = new HashMap();
    private static final Map BY_NAME = new HashMap();

    static {
        INSTANCES.put(FROM.getKey(), FROM);
        INSTANCES.put(TO.getKey(), TO);
        INSTANCES.put(CC.getKey(), CC);
        INSTANCES.put(ENVELOPE_FROM.getKey(), ENVELOPE_FROM);
        INSTANCES.put(ENVELOPE_TO.getKey(), ENVELOPE_TO);
        INSTANCES.put(USER.getKey(), USER);

        BY_NAME.put(FROM.toString(), FROM);
        BY_NAME.put(TO.toString(), TO);
        BY_NAME.put(CC.toString(), CC);
        BY_NAME.put(ENVELOPE_FROM.toString(), ENVELOPE_FROM);
        BY_NAME.put(ENVELOPE_TO.toString(), ENVELOPE_TO);
        BY_NAME.put(USER.toString(), USER);
    }

    private final char key;
    private final String kind;

    // constructors -----------------------------------------------------------

    private AddressKind(char key, String kind)
    {
        this.key = key;
        this.kind = kind;
    }

    // static factories -------------------------------------------------------

    public static AddressKind getInstance(char key)
    {
        return (AddressKind)INSTANCES.get(key);
    }

    public static AddressKind getInstance(String kindStr)
    {
        AddressKind kind = (AddressKind)BY_NAME.get(kindStr.toUpperCase());
        if (null == kind) { /* XXX setting about accepting unknown kinds */
            kind = new AddressKind('X', kindStr);
        }

        return kind;
    }

    public char getKey()
    {
        return key;
    }

    // Object kinds -----------------------------------------------------------

    public String toString() { return kind; }

    // Serialization ----------------------------------------------------------

    Object readResolve()
    {
        return getInstance(key);
    }
}
