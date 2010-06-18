/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.mail.web.euv;

import java.util.List;

import com.untangle.uvm.node.ParseException;

import com.untangle.node.mail.papi.quarantine.BadTokenException;
import com.untangle.node.mail.papi.quarantine.InboxAlreadyRemappedException;
import com.untangle.node.mail.papi.quarantine.NoSuchInboxException;
import com.untangle.node.mail.papi.quarantine.QuarantineUserActionFailedException;
import com.untangle.node.mail.papi.safelist.NoSuchSafelistException;
import com.untangle.node.mail.papi.safelist.SafelistActionFailedException;

public interface JsonInterface
{
    public boolean requestDigest( String account )
        throws ParseException, QuarantineUserActionFailedException;

    public List<JsonInboxRecord> getInboxRecords( String token, int start, int limit, 
                                                  String sortColumn, boolean isAscending )
        throws BadTokenException, NoSuchInboxException, QuarantineUserActionFailedException;

    public ActionResponse releaseMessages( String token, String messages[] )
        throws BadTokenException, NoSuchInboxException, QuarantineUserActionFailedException;

    public ActionResponse purgeMessages( String token, String messages[] )
        throws BadTokenException, NoSuchInboxException, QuarantineUserActionFailedException;

    /* Add the addresses in addresses to the safelist associated with token */
    public ActionResponse safelist( String token, String addresses[] )
        throws BadTokenException, NoSuchInboxException, NoSuchSafelistException, QuarantineUserActionFailedException, SafelistActionFailedException;

    /* Replace the safelist for the account associated with token. */
    public ActionResponse replaceSafelist( String token, String addresses[] )
        throws BadTokenException, NoSuchInboxException, NoSuchSafelistException, QuarantineUserActionFailedException, SafelistActionFailedException;

    /* Delete users from the safelist */
    public ActionResponse deleteAddressesFromSafelist( String token, String addresses[] )
        throws BadTokenException, NoSuchInboxException, NoSuchSafelistException, QuarantineUserActionFailedException, SafelistActionFailedException;

    /* Map the account associated with token to address. */
    public void setRemap( String token, String address )
        throws BadTokenException, NoSuchInboxException, QuarantineUserActionFailedException,
               InboxAlreadyRemappedException;

    /* Delete a set of remaps to the account associated with token, this returns the new list
     * of addresses that are mapped to this address. */
    public String[] deleteRemaps( String token, String[] address )
        throws BadTokenException, NoSuchInboxException, QuarantineUserActionFailedException;
}
