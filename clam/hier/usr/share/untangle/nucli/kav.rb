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

include_class(['com.untangle.node.virus.VirusMessageAction',
               'com.untangle.node.virus.SMTPVirusMessageAction'])

class Kav < UVMFilterNode

  UVM_NODE_NAME = "untangle-node-kav"
  NODE_NAME = "Kaspersky Virus Blocker"
  MIB_ROOT = UVM_FILTERNODE_MIB_ROOT + ".11"
  
  EXT_LIST = "file-extension-list"
  MIME_LIST = "mime-type-list"

  @@virusActionValues = {
    "remove" => VirusMessageAction::REMOVE,
    "pass" => VirusMessageAction::PASS
  }

  @@smtpVirusActionValues = {
    "remove" => SMTPVirusMessageAction::REMOVE,
    "pass" => SMTPVirusMessageAction::PASS,
    "block" =>  SMTPVirusMessageAction::BLOCK
  }

  def initialize
    @@diag.if_level(3) { puts! "Initializing #{get_node_name()}..." }
    super
    @@diag.if_level(3) { puts! "Done initializing #{get_node_name()}..." }
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
- kav -- enumerate all Kaspersky virus blocker nodes running on effective #{BRAND} server.
- kav <TID> http <scan:true|false>
    -- Display/Update 'Scan HTTP files' option for node TID.
- kav <TID> ftp <scan:true|false>
    -- Display/Update 'Scan FTP files' option for node TID.
- kav <TID> extension-list
    -- Display virus scanning options for each specified file type for node TID
- kav <TID> extension-list add extension [scan:true|false] description
    -- Add a new item to the file-extension-list with specified settings.
- kav <TID> extension-list update [item-number] extension [scan:true|false] description
    -- Update item '[item-number]' with specified settings.
- virus <TID> extension-list remove [item-number]
    -- Remove item '[item-number]' from file-extension-list.
- kav <TID> mime-list
    -- Display virus scanning options for each specified MIME type for node TID
- kav <TID> mime-list add mime-type [scan:true|false] description
    -- Add a new item to the mime-type-list with specified settings.
- kav <TID> mime-list update [item-number] mime-type [scan:true|false] description
    -- Update item '[item-number]' with specified settings.
- kav <TID> mime-list remove [item-number]
    -- Remove item '[item-number]' from mime-type-list.
- kav <TID> [protocol:SMTP|POP|IMAP]
    -- Display email anti-virus protection options for node TID and the specified protocol.
- kav <TID> [protocol:SMTP|POP|IMAP] update [key:scan|action|description] new_value
    -- Update email anti-virus protection options with specified settings:
      scan:true|false
      action:remove|pass|block for SMTP and remove|pass for POP and IMAP
      description:any string
- kav <TID> ftp-disable-resume <value:true|false>
    -- Display/Update 'disable FTP download resume' option for node TID.
- kav <TID> http-disable-resume <value:true|false>
    -- Display/Update 'disable HTTP download resume' option for node TID.
- kav <TID> trickle-rate <new_value>
    -- Display/Update 'scan trickle rate (percent)' option for node TID.
- kav <TID> <snmp|stats>
    -- Display virus blocker TID statistics
HELP
  end
  
  def cmd_http(tid, *args)
    scan_files_helper(tid, "HTTP", *args)
  end
  
  def cmd_ftp(tid, *args)
    scan_files_helper(tid, "FTP", *args)
  end
  
  def scan_files_helper(tid, protocol, *args)
    if args.empty?
      then 
        display_scan_files_settings(tid, protocol)
      else
        update_scan_files_settings(tid, protocol, args[0])
    end
  end
  
  def display_scan_files_settings(tid, protocol)
    config = get_protocol_config(tid, protocol)
    return "Scan #{protocol} files: " + 
        (config.getScan ? "enabled" : "disabled"); 
  end
  
  def update_scan_files_settings(tid, protocol, scan)
    config = get_protocol_config(tid, protocol)
    begin
      validate_bool(scan, "scan")
      config.setScan(scan == "true")
    rescue => ex
      return ex.to_s
    end
    update_protocol_config(tid, protocol, config)
    msg = "Scan #{protocol} files " + 
        (scan == "true" ? "enabled" : "disabled");

    @@diag.if_level(2) { puts! msg }
    return msg
  end

  def cmd_SMTP(tid)
    display_email_settings(tid, "SMTP")
  end

  def cmd_POP(tid)
    display_email_settings(tid, "POP")
  end
  
  def cmd_IMAP(tid)
    display_email_settings(tid, "IMAP")
  end
  
  def cmd_SMTP_update(tid, key, value)
    update_email_setting(tid, "SMTP", key, value)
  end

  def cmd_POP_update(tid, key, value)
    update_email_setting(tid, "POP", key, value)
  end
  
  def cmd_IMAP_update(tid, key, value)
    update_email_setting(tid, "IMAP", key, value)
  end

  def display_email_settings(tid, protocol)
    config = get_protocol_config(tid, protocol)
    ret = "scan,action,description\n"
    ret << "#{config.getScan.to_s}"
    ret << ",#{config.getMsgAction}"
    ret << ",#{config.getNotes}"
    ret << "\n"
    return ret
  end
  
  def update_email_setting(tid, protocol, key, new_value)
    config = get_protocol_config(tid, protocol)
    begin
      case key
      when "scan"
        validate_bool(new_value, "scan")
        config.setScan(new_value == "true")
      when "action"
        actionValues = protocol == "SMTP" ? @@smtpVirusActionValues : @@virusActionValues
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
    msg = "#{key.capitalize} for #{protocol} was updated."

    @@diag.if_level(2) { puts! msg }
    return msg
  end

  def cmd_extension_list(tid)
    display_scan_list(tid, EXT_LIST)
  end
  
  def cmd_extension_list_add(tid, extension, scan, description)
    update_scan_list(tid, -1, EXT_LIST, extension, scan, description)
  end  
  
  def cmd_extension_list_update(tid, pos, extension, scan, description)
    update_scan_list(tid, pos.to_i, EXT_LIST, extension, scan, description)
  end
  
  def cmd_extension_list_remove(tid, pos)
    remove_scan_list(tid, pos.to_i, EXT_LIST)
  end  

  def cmd_mime_list(tid)
    display_scan_list(tid, MIME_LIST)
  end
  
  def cmd_mime_list_add(tid, mime_type, scan, description)
    update_scan_list(tid, -1, MIME_LIST, mime_type, scan, description)
  end  
  
  def cmd_mime_list_update(tid, pos, mime_type, scan, description)
    update_scan_list(tid, pos.to_i, MIME_LIST, mime_type, scan, description)
  end
  
  def cmd_mime_list_remove(tid, pos)
    remove_scan_list(tid, pos.to_i, MIME_LIST)
  end  
  
  # TODO consider using a unique identification instead of the position, 
  # when updating/removing an element; for example extension and MIME type
  def display_scan_list(tid, list_type)
    ret = "#,"
    ret << (list_type == EXT_LIST ? "extension," : "MIME type,")
    ret << "scan,description\n"
    get_scan_list(tid, list_type).each_with_index { |elem, index|
      ret << [(index+1).to_s,
              list_type == EXT_LIST ? elem.getString() : elem.getMimeType().getType(),
              elem.isLive.to_s,
              elem.getName
             ].join(",")
      ret << "\n"
    }
    return ret
  end
  
  def update_scan_list(tid, pos, list_type, value, scan, description)
    list = get_scan_list(tid, list_type)
    begin
      validate_range(pos, 1..list.length, "item-number") if pos != -1 
      validate_bool(scan, "scan")
      
      if list_type == EXT_LIST
        elem = com.untangle.uvm.node.StringRule.new
        elem.setString(value)
      else
        elem = com.untangle.uvm.node.MimeTypeRule.new
        elem.setMimeType(com.untangle.uvm.node.MimeType.new(value))
      end
      elem.setLive(scan == "true")
      elem.setName(description)
    rescue => ex
      return ex.to_s
    end

    if pos == -1
      list.add(elem)
      msg = "Item added to the #{list_type}."
    else
      list[pos - 1] = elem
      msg = "Item #{pos} was updated."
    end
    
    update_scan_list_settings(tid, list_type, list)

    @@diag.if_level(2) { puts! msg }
    return msg
  end
  
  def remove_scan_list(tid, pos, list_type)
    list = get_scan_list(tid, list_type)
    begin
      validate_range(pos, 1..list.length, "item-number") 
      
      list.remove(list[pos - 1])
      update_scan_list_settings(tid, list_type, list)
      msg = "Item #{pos} was removed from the #{list_type}."
      
      @@diag.if_level(2) { puts! msg }
      return msg
    rescue => ex
      return ex.to_s
    end
  end  

  def cmd_ftp_disable_resume(tid, *args)
    disable_resume_helper(tid, "FTP", *args)
  end
  
  def cmd_http_disable_resume(tid, *args)
    disable_resume_helper(tid, "HTTP", *args)
  end
  
  def disable_resume_helper(tid, protocol, *args)
    if args.empty?
      then 
        display_disable_resume_settings(tid, protocol)
      else
        update_disable_resume_settings(tid, protocol, args[0])
    end
  end
  
  def display_disable_resume_settings(tid, protocol)
    settings = get_settings(tid)
    value = (protocol == "FTP" ? settings.getFtpDisableResume : settings.getHttpDisableResume)
    return "Disable #{protocol} download resume: #{value}" 
  end
  
  def update_disable_resume_settings(tid, protocol, value)
    settings = get_settings(tid)
    begin
      validate_bool(value, "value")
      if protocol == "FTP"
      then settings.setFtpDisableResume(value == "true")
      else settings.setHttpDisableResume(value == "true")
      end
    rescue => ex
      return ex.to_s
    end
    update_settings(tid, settings)
    msg = "Disable #{protocol} download resume set to #{value}"

    @@diag.if_level(2) { puts! msg }
    return msg
  end

  def cmd_trickle_rate(tid, *args)
    if args.empty?
      then 
        display_trickle_rate(tid)
      else
        update_trickle_rate(tid, args[0].to_i)
    end
  end
  
  def display_trickle_rate(tid)
    settings = get_settings(tid)
    return "Scan trickle rate (percent): #{settings.getTricklePercent}"
  end
  
  def update_trickle_rate(tid, value)
    settings = get_settings(tid)
    begin
      validate_range(value, 0..100, "trickle-rate")
      settings.setTricklePercent(value)
    rescue => ex
      return ex.to_s
    end
    update_settings(tid, settings)
    msg = "Scan trickle rate (percent) set to #{value}"

    @@diag.if_level(2) { puts! msg }
    return msg
  end

  #-- Helper methods
  def get_node(tid)
    @@uvmRemoteContext.nodeManager.nodeContext(tid).node
  end
  
  def get_settings(tid)
    get_node(tid).getVirusSettings
  end
  
  def update_settings(tid, settings)
    get_node(tid).setVirusSettings(settings)
  end
  
  def get_protocol_config(tid, protocol)
    settings = get_settings(tid)
    case protocol
    when "FTP"
      settings.getFtpConfig
    when "HTTP"
      settings.getHttpConfig
    when "SMTP"
      settings.getSmtpConfig
    when "POP"
      settings.getPopConfig
    when "IMAP"
      settings.getImapConfig
    end
  end
  
  def update_protocol_config(tid, protocol, config)
    node = get_node(tid)
    settings = node.getVirusSettings

    case protocol
    when "FTP"
      settings.setFtpConfig(config)
    when "HTTP"
      settings.setHttpConfig(config)
    when "SMTP"
      settings.setSmtpConfig(config)
    when "POP"
      settings.setPopConfig(config)
    when "IMAP"
      settings.setImapConfig(config)
    end
    
    node.setVirusSettings(settings)
  end
  
  def get_scan_list(tid, list_type)
    list_type == EXT_LIST ? get_settings(tid).getExtensions : get_settings(tid).getHttpMimeTypes 
  end  
  
  def update_scan_list_settings(tid, list_type, list)
    settings = get_settings(tid)
    list_type == EXT_LIST ? settings.setExtensions(list) : settings.setHttpMimeTypes(list) 
    update_settings(tid, settings)
  end
  
end
