-- settings converter for release 3.2
-- $HeadURL$
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

ALTER TABLE settings.tr_mail_quarantine_settings
    DROP COLUMN digest_from;



-- com.untangle.mvvm.tran.EmailAddressRule
CREATE TABLE settings.email_addr_rule (
    rule_id int8 NOT NULL,
    address text,
    name text,
    category text,
    description text,
    live bool,
    alert bool,
    log bool,
    position int4,
    settings_id int8,     
    PRIMARY KEY (rule_id));    

-- com.untangle.tran.mail.papi.EmailAddressPairRule
CREATE TABLE settings.email_addr_pair_rule (
    rule_id int8 NOT NULL,
    address1 text,
    address2 text,
    name text,
    category text,
    description text,
    live bool,
    alert bool,
    log bool,
    position int4,
    settings_id int8,    
    PRIMARY KEY (rule_id));      
    
