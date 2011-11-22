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

import static com.untangle.node.mime.HeaderNames.CC_LC;
import static com.untangle.node.mime.HeaderNames.FROM_LC;
import static com.untangle.node.mime.HeaderNames.TO_LC;

import java.util.List;
import java.util.Map;

//TODO: bscott This class should be renamed "MIMEMessageHeadersFieldFactory".

/**
 * Subclass of MIMEPartHeaderFieldFactory which adds strong typing
 * for the following headers:
 * <ul>
 * <li><b>TO</b> via the {@link com.untangle.node.mime.EmailAddressHeaderField class}</li>
 * <li><b>CC</b> via the {@link com.untangle.node.mime.EmailAddressHeaderField class}</li>
 * </ul>
 *
 */
public class MailMessageHeaderFieldFactory
    extends MIMEPartHeaderFieldFactory {

    @Override
    protected HeaderField createHeaderField(String mixedCaseName) {


        LCString lcString = new LCString(mixedCaseName);

        if(lcString.equals(TO_LC) ||
           lcString.equals(CC_LC) ||
           lcString.equals(FROM_LC)) {
            return new EmailAddressHeaderField(mixedCaseName,
                                               lcString);
        }

        return super.createHeaderField(mixedCaseName);
    }

    @Override
    protected Headers createHeaders(MIMESource source,
                                    int sourceStart,
                                    int sourceLen,
                                    List<HeaderField> headersInOrder,
                                    Map<LCString, List<HeaderField>> headersByName) {

        return new MIMEMessageHeaders(this,
                                      source,
                                      sourceStart,
                                      sourceLen,
                                      headersInOrder,
                                      headersByName);

    }

}
