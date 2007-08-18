-- settings conversion for release-5.0.3
-- $HeadURL: svn://chef/work/src/spam-base/hier/usr/share/untangle/schema/untangle-base-spam/settings-convert-6.sql $
-- Copyright (c) 2003-2007 Untangle, Inc. 
--
-- This program is free software; you can redistribute it and/or modify
-- it under the terms of the GNU General Public License, version 2,
-- as published by the Free Software Foundation.
--
-- This program is distributed in the hope that it will be useful, but
-- AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
-- NONINFRINGEMENT.  See the GNU General Public License for more details.
--
-- You should have received a copy of the GNU General Public License
-- along with this program; if not, write to the Free Software
-- Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
--

-- The goal is to remap the DNSBL entries.  One way would be to just delete all.
-- but this depends on some function named initSpamRBLList never changing.
-- This technique just deletes everything except for the new rule.
-- Create a new RBL list.
UPDATE settings.n_spam_rbl 
        SET hostname ='zen.spamhaus.org', description='Spamhaus SBL, XBL and PBL lists.'
        WHERE hostname='sbl-xbl.spamhaus.org';

-- Update the list to remove the items that are no longer used.
DELETE FROM settings.n_spam_rbl_list WHERE rule_id IN
         ( SELECT id FROM n_spam_rbl WHERE hostname != 'zen.spamhaus.org' );

-- There is only one row, so its position is going to be 0
UPDATE settings.n_spam_rbl_list SET position=0;

-- Delete all of the other rules (sbl-xbl.spamhaus.org should never match)
DELETE FROM settings.n_spam_rbl WHERE 
        hostname = 'bl.spamcop.net' OR hostname = 'dul.dnsbl.sorbs.net' OR hostname = 'sbl-xbl.spamhaus.org';
