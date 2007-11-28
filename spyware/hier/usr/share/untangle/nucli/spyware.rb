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
include_class('com.untangle.node.http.UserWhitelistMode')

class Spyware < UVMFilterNode

  UVM_NODE_NAME = "untangle-node-spyware"
  NODE_NAME = "Spyware"
  MIB_ROOT = UVM_FILTERNODE_MIB_ROOT + ".8"

  @@userWhitelistModeValues = {
    "none" => UserWhitelistMode::NONE,
    "user-only" => UserWhitelistMode::USER_ONLY,
    "user-and-global" => UserWhitelistMode::USER_AND_GLOBAL
  }

  def initialize
    @diag = Diag.new(DEFAULT_DIAG_LEVEL)
    @diag.if_level(3) { puts! "Initializing #{get_node_name()}..." }
    super
    @diag.if_level(3) { puts! "Done initializing #{get_node_name()}..." }
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
    return <<-HELP
- spyware -- enumerate all spyware blocker filter nodes running on effective #{BRAND} server.
- spyware <TID> block-list url <enable:true|false>
    -- Display/Update Spyware and ad URL blocking settings.
- spyware <TID> block-list [item-type:subnet|cookie|activex]
    -- Display spyware blocker block list for items of the given type for node TID
- spyware <TID> block-list subnet add name subnet [log:true|false]
    -- Add a new item subnet item to the block-list with specified settings.
- spyware <TID> block-list [item-type:cookie|activex] add identification [block:true|false]
    -- Add a new item of the given type to the block-list with specified settings.
- spyware <TID> block-list subnet update [item-number] subnet name subnet [log:true|false]
    -- Update item '[item-number]' with specified settings.
- spyware <TID> block-list [item-type:cookie|activex] update [item-number] [item-type:cookie|activex] identification [block:true|false]
    -- Update item '[item-number]' with specified settings.
- spyware <TID> block-list [item-type:subnet|cookie|activex] remove [item-number]
    -- Remove item '[item-number]' from block list.
- spyware <TID> pass-list
    -- Display spyware blocker pass list for node TID
- spyware <TID> pass-list add [pass:true|false] domain
    -- Add a new item to the pass-list with specified settings.
- spyware <TID> pass-list update [item-number] [pass:true|false] domain
    -- Update item '[item-number]' with specified settings.
- spyware <TID> pass-list remove [item-number]
    -- Remove item '[item-number]' from pass list.
- spyware <TID> block-all-activex <new-value:true|false>
    -- Display/Update 'block all ActiveX' settings for node TID.
- spyware <TID> quick-passlist <new-value:none|user-only|user-and-global>
    -- Display/Update 'quick-passlist' settings for node TID.
- spyware <TID> <snmp|stats>
    -- Display spyware blocker TID statistics (TODO document this)
  HELP
  end

  def cmd_block_list_url(tid, *args)
    if args.empty?
      then 
        display_block_list_url_settings(tid)
      else
        update_block_list_url_settings(tid, args[0])
    end
  end
  
  def display_block_list_url_settings(tid)
    settings = get_settings(tid)
    return "Spyware and ad URL blocking: " + 
        (settings.getUrlBlacklistEnabled ? "enabled" : "disabled"); 
  end
  
  def update_block_list_url_settings(tid, enable)
    settings = get_settings(tid)
    begin
      validate_bool(enable, "enable")
      settings.setUrlBlacklistEnabled(enable == "true")
    rescue => ex
      return ex.to_s
    end
    update_settings(tid, settings)
    msg = "Spyware and ad URL blocking " + 
        (enable == "true" ? "enabled" : "disabled");

    @diag.if_level(2) { puts! msg }
    return msg
  end

  def cmd_block_list_subnet(tid)
    ret = "#,name,subnet,log\n"
    get_block_list_subnet_rules(tid).each_with_index { |rule, index|
      ret << [(index+1).to_s,
              rule.getName,
              rule.getIpMaddr.to_s,
              rule.getLog.to_s
             ].join(",")
      ret << "\n"
    }
    return ret
  end
  
  def cmd_block_list_subnet_add(tid, name, subnet, log)
    update_block_list_subnet(tid, -1,  name, subnet, log)
  end  
  
  def cmd_block_list_subnet_update(tid, pos, name, subnet, log)
    update_block_list_subnet(tid, pos.to_i,  name, subnet, log)
  end
  
  def cmd_block_list_subnet_remove(tid, pos)
    remove_block_list_subnet(tid, pos.to_i)
  end  
  
  # TODO consider using a unique identification instead of the position, 
  # when updating/removing a subnet rule; for example name or ip_maddr
  def update_block_list_subnet(tid, pos, name, subnet, log)
    rules = get_block_list_subnet_rules(tid)
    begin
      validate_range(pos, 1..rules.length, "subnet rule number") if pos != -1
      validate_bool(log, "log")
      ip_maddr = validate_subnet(subnet, "subnet")
      
      rule = com.untangle.uvm.node.IPMaddrRule.new
      rule.setName(name)
      rule.setIpMaddr(ip_maddr)
      rule.setLog(log == "true")
    rescue => ex
      return ex.to_s
    end

    if pos == -1
      rules.add(rule)
      msg = "Subnet rule added to the block list."
    else
      rules[pos - 1] = rule
      msg = "Subnet rule #{pos} updated."
    end
    
    update_block_list_subnet_rules(tid, rules)

    @diag.if_level(2) { puts! msg }
    return msg
  end
  
  def remove_block_list_subnet(tid, pos)
    rules = get_block_list_subnet_rules(tid)
    begin
      validate_range(pos, 1..rules.length, "subnet rule number")
      
      rules.remove(rules[pos - 1])
      update_block_list_subnet_rules(tid, rules)
      msg = "Subnet rule #{pos} was removed from the block list."
      
      @diag.if_level(2) { puts! msg }
      return msg
    rescue => ex
      return ex.to_s
    end
  end  
    
  def cmd_block_list_cookie(tid)
    ret = "#,identification,block\n"
    get_block_list_cookie_rules(tid).each_with_index { |rule, index|
      ret << [(index+1).to_s,
              rule.getString,
              rule.isLive.to_s
             ].join(",")
      ret << "\n"
    }
    return ret
  end
  
  def cmd_block_list_cookie_add(tid, identification, block)
    update_block_list_cookie(tid, -1,  identification, block)
  end  
  
  def cmd_block_list_cookie_update(tid, pos, identification, block)
    update_block_list_cookie(tid, pos.to_i, identification, block)
  end
  
  def cmd_block_list_cookie_remove(tid, pos)
    remove_block_list_cookie(tid, pos.to_i)
  end  
  
  # TODO consider using a unique identification instead of the position, 
  # when updating/removing a cookie rule; for example identification
  def update_block_list_cookie(tid, pos, identification, block)
    rules = get_block_list_cookie_rules(tid)
    begin
      validate_range(pos, 1..rules.length, "cookie rule number") if pos != -1 
      validate_bool(block, "block")
      
      rule = com.untangle.uvm.node.StringRule.new
      rule.setString(identification)
      rule.setLive(block == "true")
    rescue => ex
      return ex.to_s
    end

    if pos == -1
      rules.add(rule)
      msg = "Cookie rule added to the block list."
    else
      rules[pos - 1] = rule
      msg = "Cookie rule #{pos} updated."
    end
    
    update_block_list_cookie_rules(tid, rules)

    @diag.if_level(2) { puts! msg }
    return msg
  end
  
  def remove_block_list_cookie(tid, pos)
    rules = get_block_list_cookie_rules(tid)
    begin
      validate_range(pos, 1..rules.length, "cookie rule number")
      
      rules.remove(rules[pos - 1])
      update_block_list_cookie_rules(tid, rules)
      msg = "Cookie rule #{pos} was removed from the block list."
      
      @diag.if_level(2) { puts! msg }
      return msg
    rescue => ex
      return ex.to_s
    end
  end  

  def cmd_block_list_activex(tid)
    ret = "#,identification,block\n"
    get_block_list_activex_rules(tid).each_with_index { |rule, index|
      ret << [(index+1).to_s,
              rule.getString,
              rule.isLive.to_s
             ].join(",")
      ret << "\n"
    }
    return ret
  end

  def cmd_block_list_activex_add(tid, identification, block)
    update_block_list_activex(tid, -1,  identification, block)
  end  
  
  def cmd_block_list_activex_update(tid, pos, identification, block)
    update_block_list_activex(tid, pos.to_i, identification, block)
  end
  
  def cmd_block_list_activex_remove(tid, pos)
    remove_block_list_activex(tid, pos.to_i)
  end  
  
  # TODO consider using a unique identification instead of the position, 
  # when updating/removing a activex rule; for example identification
  def update_block_list_activex(tid, pos, identification, block)
    rules = get_block_list_activex_rules(tid)
    begin
      validate_range(pos, 1..rules.length, "ActiveX rule number") if pos != -1
      validate_bool(block, "block")
      
      rule = com.untangle.uvm.node.StringRule.new
      rule.setString(identification)
      rule.setLive(block == "true")
    rescue => ex
      return ex.to_s
    end

    if pos == -1
      rules.add(rule)
      msg = "ActiveX rule added to the block list."
    else
      rules[pos - 1] = rule
      msg = "ActiveX rule #{pos} updated."
    end
    
    update_block_list_activex_rules(tid, rules)

    @diag.if_level(2) { puts! msg }
    return msg
  end
  
  def remove_block_list_activex(tid, pos)
    rules = get_block_list_activex_rules(tid)
    begin
      validate_range(pos, 1..rules.length, "ActiveX rule number")
      
      rules.remove(rules[pos - 1])
      update_block_list_activex_rules(tid, rules)
      msg = "ActiveX rule #{pos} was removed from the block list."
      
      @diag.if_level(2) { puts! msg }
      return msg
    rescue => ex
      return ex.to_s
    end
  end  

  def cmd_pass_list(tid)
    ret = "#,pass,domain\n"
    get_pass_list(tid).each_with_index { |domain, index|
      ret << [(index+1).to_s,
              domain.isLive.to_s,
              domain.getString
             ].join(",")
      ret << "\n"
    }
    return ret
  end
  
  def cmd_pass_list_add(tid, pass, domain)
    update_pass_list(tid, -1, pass, domain)
  end  
  
  def cmd_pass_list_update(tid, pos, pass, domain)
    update_pass_list(tid, pos.to_i, pass, domain)
  end
  
  def cmd_pass_list_remove(tid, pos)
    remove_pass_list(tid, pos.to_i)
  end  
  
  # TODO consider using a unique identification instead of the position, 
  # when updating/removing a pass domaine; for example domain
  def update_pass_list(tid, pos, pass, domain)
    list = get_pass_list(tid)
    begin
      validate_range(pos, 1..list.length, "domain number") if pos != -1 
      validate_bool(pass, "pass")
      #TODO validate domain
      
      elem = com.untangle.uvm.node.StringRule.new
      elem.setLive(pass == "true")
      elem.setString(domain)
    rescue => ex
      return ex.to_s
    end

    if pos == -1
      list.add(elem)
      msg = "Domain added to the pass list."
    else
      list[pos - 1] = elem
      msg = "Domain #{pos} updated."
    end
    
    update_pass_list_settings(tid, list)

    @diag.if_level(2) { puts! msg }
    return msg
  end
  
  def remove_pass_list(tid, pos)
    list = get_pass_list(tid)
    begin
      validate_range(pos, 1..list.length, "domain number")
      
      list.remove(list[pos - 1])
      update_pass_list_settings(tid, list)
      msg = "Domain #{pos} was removed from the pass list."
      
      @diag.if_level(2) { puts! msg }
      return msg
    rescue => ex
      return ex.to_s
    end
  end  

  def cmd_block_all_activex(tid, *args)
    if args.empty?
      then 
        display_block_all_activex_settings(tid)
      else
        update_block_all_activex_settings(tid, args[0])
    end
  end
  
  def display_block_all_activex_settings(tid)
    settings = get_settings(tid)
    return "Block all ActiveX: " + 
        (settings.getBlockAllActiveX ? "enabled" : "disabled"); 
  end
  
  def update_block_all_activex_settings(tid, enable)
    settings = get_settings(tid)
    begin
      validate_bool(enable, "enable")
      settings.setBlockAllActiveX(enable == "true")
    rescue => ex
      return ex.to_s
    end
    update_settings(tid, settings)
    msg = "Block all ActiveX " + 
        (enable == "true" ? "enabled" : "disabled")

    @diag.if_level(2) { puts! msg }
    return msg
  end

  def cmd_quick_passlist(tid, *args)
    if args.empty?
      then 
        display_quick_passlist_settings(tid)
      else
        update_quick_passlist_settings(tid, args[0])
    end
  end
  
  def display_quick_passlist_settings(tid)
    settings = get_settings(tid)
    return "Quick-passlist: " + settings.getUserWhitelistMode.to_s
  end
  
  def update_quick_passlist_settings(tid, new_value)
    settings = get_settings(tid)
    begin
      validate_list(new_value, @@userWhitelistModeValues.keys ,"quick-passlist")
      settings.setUserWhitelistMode(@@userWhitelistModeValues[new_value])
    rescue => ex
      return ex.to_s
    end
    update_settings(tid, settings)
    msg = "Quick-passlist " + settings.getUserWhitelistMode.to_s

    @diag.if_level(2) { puts! msg }
    return msg
  end
  
  #
  # Command handlers.
  #

  def validate_subnet(var, varname)
      begin
          return com.untangle.uvm.node.IPMaddr.parse(var)
      rescue
        raise "Error: invalid value for '#{varname}': #{var}"
      end
  end
    
  #-- Helper methods
  def get_node(tid)
    @@uvmRemoteContext.nodeManager.nodeContext(tid).node
  end
  
  def get_settings(tid)
    get_node(tid).getSpywareSettings
  end
  
  def update_settings(tid, settings)
    get_node(tid).setSpywareSettings(settings)
  end
  
  def get_block_list_subnet_rules(tid)
    get_settings(tid).getSubnetRules
  end
  
  def update_block_list_subnet_rules(tid, rules)
    settings = get_node(tid).getSpywareSettings
    settings.setSubnetRules(rules)
    update_settings(tid, settings)
  end
  
  def get_block_list_cookie_rules(tid)
    get_settings(tid).getCookieRules
  end
  
  def update_block_list_cookie_rules(tid, rules)
    settings = get_node(tid).getSpywareSettings
    settings.setCookieRules(rules)
    update_settings(tid, settings)
  end
  
  def get_block_list_activex_rules(tid)
    get_settings(tid).getActiveXRules
  end
  
  def update_block_list_activex_rules(tid, rules)
    settings = get_node(tid).getSpywareSettings
    settings.setActiveXRules(rules)
    update_settings(tid, settings)
  end
  
  def get_pass_list(tid)
    get_settings(tid).getDomainWhitelist
  end
  
  def update_pass_list_settings(tid, list)
    settings = get_node(tid).getSpywareSettings
    settings.setDomainWhitelist(list)
    update_settings(tid, settings)
  end
  
end
