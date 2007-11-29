#
# $HeadURL:$
# Copyright (c) 2003-2007 Untangle, Inc. 
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License, version 2,
# as published by the Free Software Foundation.
#
# This program is distributed in the hope that it will be useful, but
# AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
# NONINFRINGEMENT.  See the GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
require 'filternode'

class Virus < UVMFilterNode

  UVM_NODE_NAME = "untangle-node-clam"
  NODE_NAME = "Virus Blocker"
  MIB_ROOT = UVM_FILTERNODE_MIB_ROOT + ".9"

  def initialize
    @diag = Diag.new(DEFAULT_DIAG_LEVEL)
    @diag.if_level(3) { puts! "Initializing #{get_node_name()}..." }
    super
    @diag.if_level(3) { puts! "Done initializing #{get_node_name()}..." }
  end
  
  def get_uvm_node_name()
    UVM_NODE_NAME
  end
  
  def get_node_name()
    NODE_NAME
  end
  
  def get_mib_root()
    MIB_ROOT
  end  
  
  def get_help_text()
    return <<-HELP
- virus -- enumerate all virus blocker nodes running on effective #{BRAND} server.
- virus <TID> web http <scan:true|false>
    -- Display/Update 'Scan HTTP files' option for node TID.
- virus <TID> web file-extension-list
    -- Display virus scanning options for each specified file type for node TID
- virus <TID> web file-extension-list add extension [scan:true|false] description
    -- Add a new item to the file-extension-list with specified settings.
- virus <TID> web file-extension-list update [item-number] extension [scan:true|false] description
    -- Update item '[item-number]' with specified settings.
- virus <TID> web file-extension-list remove [item-number]
    -- Remove item '[item-number]' from file-extension-list.
- virus <TID> web mime-type-list
    -- Display virus scanning options for each specified MIME type for node TID
- virus <TID> web mime-type-list add mime-type [scan:true|false] description
    -- Add a new item to the mime-type-list with specified settings.
- virus <TID> web mime-type-list update [item-number] mime-type [scan:true|false] description
    -- Update item '[item-number]' with specified settings.
- virus <TID> web mime-type-list remove [item-number]
    -- Remove item '[item-number]' from mime-type-list.
- virus <TID> email [protocol:SMTP|POP|IMAP]
    -- Display email anti-virus protection options for node TID and the specified protocol.
- virus <TID> email [protocol:SMTP|POP|IMAP] update [key:scan|action|description] new_value
    -- Update email anti-virus protection options with specified settings:
      scan:true|false
      action:remove-infection|pass-msg|block-msg for SMTP and remove-infection|pass-msg for POP and IMAP
      description:any string
- virus <TID> ftp <scan:true|false>
    -- Display/Update 'Scan FTP files' option for node TID.
- virus <TID> ftp-resume <enable:true|false>
    -- Display/Update 'FTP download resume' option for node TID.
- virus <TID> http-resume <enable:true|false>
    -- Display/Update 'HTTP download resume' option for node TID.
- virus <TID> trickle-rate <new_value>
    -- Display/Update 'scan trickle rate (percent)' option for node TID.
- virus <TID> <snmp|stats>
    -- Display virus blocker TID statistics
HELP
  end

end
