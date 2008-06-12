-- settings conversion for release-webui
-- $HeadURL: svn://chef/work/src/uvm/hier/usr/share/untangle/schema/uvm/settings-convert-15.sql $
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

-- com.untangle.uvm.SkinSettings
CREATE TABLE settings.u_skin_settings (
    skin_settings_id int8 NOT NULL,
    admin_skin text,
    user_skin text,
    PRIMARY KEY (skin_settings_id));

-- com.untangle.uvm.LanguageSettings
CREATE TABLE settings.u_language_settings (
    language_settings_id int8 NOT NULL,
    language text,
    PRIMARY KEY (language_settings_id));

-- com.untangle.uvm.engine.BlingerSettings
CREATE TABLE settings.u_blinger_settings (
    settings_id       int8 NOT NULL,
    tid               int8 UNIQUE,
    PRIMARY KEY       (settings_id));

-- com.untangle.uvm.engine.BlingerSettings.activeBlingers
CREATE TABLE settings.u_blinger_active (
    blinger_settings_id  int8 NOT NULL,
    position             int4 NOT NULL,
    blinger_name         text NOT NULL,
    PRIMARY KEY          (blinger_settings_id, position));
