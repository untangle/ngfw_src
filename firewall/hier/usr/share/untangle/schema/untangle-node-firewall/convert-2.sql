-- convert script for release 3.0
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

--------------------------------------------------
-- 1. Firewall events no longer references rules |
-- whether or not the session was blocked is     |
-- stored in the event itself.                   |
--                                               |
-- 2. Firewall rules no longer use source and    |
-- client interface, instead use inbound and     |
-- outbound booleans.                            |
--------------------------------------------------

-- com.untangle.tran.firewall.FirewallEvent (adding was_blocked)
ALTER TABLE events.tr_firewall_evt ADD COLUMN was_blocked BOOL;

-- Setting was_blocked to true initially (assuming that default, 
-- and then updating with what was in the rule.
UPDATE events.tr_firewall_evt SET was_blocked = true;

-- Update with what was in the latest rule that it referenced.
UPDATE tr_firewall_evt SET was_blocked = is_traffic_blocker 
        FROM tr_firewall_evt evt JOIN firewall_rule rule ON evt.rule_id = rule.rule_id 
        WHERE tr_firewall_evt.event_id=evt.event_id;

-- Delete all of the dangling firewall rules( rules that were only referenced by events)
DELETE FROM firewall_rule WHERE 
       rule_id NOT IN ( SELECT rule_id FROM tr_firewall_rules );

-- Convert from source and destination interface to inbound and outbound
ALTER TABLE settings.firewall_rule ADD COLUMN inbound BOOL;
ALTER TABLE settings.firewall_rule ADD COLUMN outbound BOOL;

-- fallback in case something fails in the conversion
UPDATE settings.firewall_rule SET inbound=true, outbound=true;

-- Set all of the values, just using the src_intf_matcher
UPDATE settings.firewall_rule SET
        inbound =CASE WHEN src_intf_matcher='O' OR src_intf_matcher='*' THEN true ELSE false END,
        outbound=CASE WHEN src_intf_matcher='I' OR src_intf_matcher='*' THEN true ELSE false END;

-- Drop the old columns
ALTER TABLE settings.firewall_rule DROP COLUMN src_intf_matcher;
ALTER TABLE settings.firewall_rule DROP COLUMN dst_intf_matcher;
 