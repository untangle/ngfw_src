/*
 * $HeadURL: svn://chef/work/src/smtp-casing/src/com/untangle/node/smtp/sasl/PLAINObserver.java $
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
package com.untangle.node.smtp.sasl;

import static com.untangle.node.util.ASCIIUtil.bbToString;

import java.nio.ByteBuffer;

/**
 * Observer for the PLAIN (RFC 2595) SASL Mechanism.
 */
class PLAINObserver extends ClearObserver
{

    static final String[] MECH_NAMES = new String[] { "PLAIN".toLowerCase() };

    private String m_id;

    PLAINObserver() {
        super(MECH_NAMES[0], DEF_MAX_MSG_SZ);
    }

    @Override
    public FeatureStatus exchangeAuthIDFound()
    {
        return m_id == null ? FeatureStatus.UNKNOWN : FeatureStatus.YES;
    }

    @Override
    public String getAuthID()
    {
        return m_id;
    }

    @Override
    public boolean initialClientData(ByteBuffer buf)
    {
        return clientMessage(buf);
    }

    @Override
    public boolean clientData(ByteBuffer buf)
    {
        return clientMessage(buf);
    }

    private boolean clientMessage(ByteBuffer buf)
    {

        if (!buf.hasRemaining()) {
            return false;
        }

        // I'm unclear from the spec if the authorization ID
        // is blank, if there is a leading null. If so,
        // just strip it off
        if (buf.get(buf.position()) == 0) {
            buf.get();
        }
        if (!buf.hasRemaining()) {
            return false;
        }

        // Now, there should be at least one and at-most
        // two NULL bytes (0) in this buffer.
        int nullPos = -1;
        for (int i = buf.position(); i < buf.limit(); i++) {
            if (buf.get(i) == 0) {
                nullPos = i;
                break;
            }
        }

        if (nullPos == -1) {
            return false;
        }
        buf.limit(nullPos);
        m_id = bbToString(buf);

        return m_id != null;
    }
}
