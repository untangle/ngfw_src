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
include_class(['com.untangle.node.spam.SpamMessageAction',
               'com.untangle.node.spam.SMTPSpamMessageAction'])

class Phish < UVMFilterNode

  UVM_NODE_NAME = "untangle-node-phish"
  NODE_NAME = "Phish Blocker"
  MIB_ROOT = UVM_FILTERNODE_MIB_ROOT + ".6"

  @@spamActionValues = {
    "mark" => SpamMessageAction::MARK,
    "pass" => SpamMessageAction::PASS
  }

  @@smtpSpamActionValues = {
    "mark" => SMTPSpamMessageAction::MARK,
    "pass" => SMTPSpamMessageAction::PASS,
    "block" =>  SMTPSpamMessageAction::BLOCK,
    "quarantine" => SMTPSpamMessageAction::QUARANTINE
  }

  def initialize
    @@diag.if_level(3) { puts! "Initializing #{get_node_name()}..." }
    super
    @@diag.if_level(3) { puts! "Done initializing #{get_node_name()}..." }
  end

  #
  # Required UVMFilterNode methods.
  #
  
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
    return <<HELP
- phish -- enumerate all phish blocker nodes running on effective #{BRAND} server.
- phish <#X|TID> web
    -- Display web anti-phishing protection options for node #X|TID.
- phish <#X|TID> web [enable:true|false]
    -- Set web anti-phishing protection options for node #X|TID.
- phish <#X|TID> [protocol:SMTP|POP|IMAP]
    -- Display email anti-phishing protection options for node #X|TID and the specified protocol.
- phish <#X|TID> [protocol:SMTP|POP|IMAP] update [key:scan|action|description] new_value
    -- Update email anti-phishing protection options with specified settings:
      scan:true|false
      action:mark|pass|block|quarantine for SMTP and mark|pass for POP and IMAP
      description:any string
- phish <#X|TID> <snmp|stats>
    -- Display phish blocker #X|TID statistics
HELP
  end

  #
  # Command handlers.
  #

  def cmd_SMTP(tid)
    display_settings(tid, "SMTP")
  end

  def cmd_POP(tid)
    display_settings(tid, "POP")
  end
  
  def cmd_IMAP(tid)
    display_settings(tid, "IMAP")
  end
  
  def cmd_SMTP_update(tid, key, value)
    update_protocol_setting(tid, "SMTP", key, value)
  end

  def cmd_POP_update(tid, key, value)
    update_protocol_setting(tid, "POP", key, value)
  end
  
  def cmd_IMAP_update(tid, key, value)
    update_protocol_setting(tid, "IMAP", key, value)
  end
  
  def cmd_web(tid, *args)
    if args.empty?
      then 
        display_web_settings(tid)
      else
        update_web_settings(tid, args[0])
    end
  end
  
  def display_settings(tid, protocol)
    config = get_protocol_config(tid, protocol)
    ret = "scan,action,description\n"
    ret << "#{config.getScan.to_s}"
    ret << ",#{config.getMsgAction}"
    ret << ",#{config.getNotes}"
    ret << "\n"
    return ret
  end
  
  def update_protocol_setting(tid, protocol, key, new_value)
    config = get_protocol_config(tid, protocol)
    begin
      case key
      when "scan"
        validate_bool(new_value, "scan")
        config.setScan(new_value == "true")
      when "action"
        actionValues = protocol == "SMTP" ? @@smtpSpamActionValues : @@spamActionValues
        validate_list(new_value, actionValues.keys ,"action")
        config.setMsgAction(actionValues[new_value])
      when "description"
        config.setNotes(new_value)
      else
        raise "Error: invalid key for '#{protocol}'"
      end      
    rescue => ex
      return ex.to_s
    end
    update_protocol_config(tid, protocol, config)
    msg = "#{key} for #{protocol} updated."

    @@diag.if_level(2) { puts! msg }
    return msg
  end
  
  def display_web_settings(tid)
    settings = get_phish_settings(tid)
    return "Web anti-phishing protection: " + 
        (settings.getEnableGooglePhishList ? "enabled" : "disabled"); 
  end
  
  def update_web_settings(tid, enable)
    settings = get_phish_settings(tid)
    begin
      validate_bool(enable, "enable")
      settings.setEnableGooglePhishList(enable == "true")
    rescue => ex
      return ex.to_s
    end
    update_phish_settings(tid, settings)
    msg = "Web anti-phishing protection " + 
        (enable == "true" ? "enabled" : "disabled");

    @@diag.if_level(2) { puts! msg }
    return msg
  end

  #-- Helper methods
  def get_phish_settings(tid)
    @@uvmRemoteContext.nodeManager.nodeContext(tid).node.getPhishSettings
  end
  
  def update_phish_settings(tid, settings)
    @@uvmRemoteContext.nodeManager.nodeContext(tid).node.setPhishSettings(settings)
  end
  
  def get_protocol_config(tid, protocol)
    settings = get_phish_settings(tid)
    case protocol
    when "SMTP"
      settings.getSmtpConfig
    when "POP"
      settings.getPopConfig
    when "IMAP"
      settings.getImapConfig
    end
  end
  
  def update_protocol_config(tid, protocol, config)
    node = @@uvmRemoteContext.nodeManager.nodeContext(tid).node
    settings = node.getPhishSettings

    case protocol
    when "SMTP"
      settings.setSmtpConfig(config)
    when "POP"
      settings.setPopConfig(config)
    when "IMAP"
      settings.setImapConfig(config)
    end
    
    node.setPhishSettings(settings)
  end
  
  def get_snmp_stat_map()
      return {7 => "scan", 8 => "block", 9 => "pass", 10 => "remove"};
  end

end
