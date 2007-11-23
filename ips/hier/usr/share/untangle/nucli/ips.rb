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

class Ips < UVMFilterNode
  include CmdDispatcher
  include RetryLogin

  UVM_NODE_NAME = "untangle-node-ips"
  NODE_NAME = "IPS"
  MIB_ROOT = UVM_FILTERNODE_MIB_ROOT + ".5"

  def initialize
    @diag = Diag.new(DEFAULT_DIAG_LEVEL)
    @diag.if_level(3) { puts! "Initializing #{get_node_name()}..." }
    super
    @diag.if_level(3) { puts! "Done initializing #{get_node_name()}..." }
  end
  
  
  #
  # Required UVMFilteNode methods.
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
- ips -- enumerate all intrusion prevention nodes running on effective #{BRAND} server.
- ips <TID> rule-list
    -- Display intrusion prevention rule list for node TID
- ips <TID> rule-list add sid category [block:true|false] [log:true|false] description info-URL signature
    -- Add a new rule to the rule-list with specified settings.
- ips <TID> rule-list update [rule-sid] category [block:true|false] [log:true|false] description info-URL signature
    -- Update item identified by '[rule-sid]' with specified settings.
- ips <TID> rule-list remove [rule-sid]
    -- Remove item identified by sid '[rule-sid]' from rule list.
- ips <TID> variable-list
    -- Display intrusion prevention variable list for node TID
- ips <TID> variable-list add name value description
    -- Add a new variable to the variable-list with specified settings.
- ips <TID> variable-list update [variable-name] value description
    -- Update item identified by '[variable-name]' with specified settings.
- ips <TID> variable-list remove [variable-name]
    -- Remove item identified by '[variable-name]' from variable list.
- ips <TID> <snmp|stats>
    -- Display intrusion prevention TID statistics.
  HELP
  end

  def cmd_rule_list(tid)
    ret = "sid, category,block,log,description,info-URL,signature\n"
    get_rules(tid).each { |rule|
      ret << [
              rule.getSid,
              rule.getCategory,
              rule.isLive.to_s,
              rule.getLog.to_s,
              rule.getDescription,
              rule.getURL,
              rule.getText
             ].join(",")
      ret << "\n"
    }
    return ret
  end

  def cmd_rule_list_add(tid, *args)
    add_rule(tid, false, *args)
  end

  def cmd_rule_list_update(tid, sid, *args)
    remove_rule(tid, sid.to_i)
    add_rule(tid, true, sid, *args)
  end

  def cmd_rule_list_remove(tid, sid)
    remove_rule(tid, sid.to_i)
  end

  def cmd_variable_list(tid)
    ret = "name,value,description\n"
    get_variables(tid).each { |variable|
      ret << [
              variable.getVariable,
              variable.getDefinition,
              variable.getDescription
             ].join(",")
      ret << "\n"
    }
    return ret
  end

  def cmd_variable_list_add(tid, *args)
    add_variable(tid, false, *args)
  end

  def cmd_variable_list_update(tid, var_name, *args)
    remove_variable(tid, var_name)
    add_variable(tid, true, var_name, *args)
  end

  def cmd_variable_list_remove(tid, var_name)
    remove_variable(tid, var_name)
  end
  
  #
  # Command handlers.
  #
  
  # Add a rule to the rule-list
  def add_rule(tid, update, sid, category, block, log, description, info_url, signature)
    node = get_node(tid)
    begin
      validate_bool(block, "block")
      validate_bool(log, "log")
      rule = com.untangle.node.ips.IPSRule.new
      rule.setSid(sid.to_i)      
      rule.setCategory(category)
      rule.setLive(block == "true")
      rule.setLog(log == "true")
      rule.setDescription(description)
      rule.setURL(info_url)
      rule.setText(signature)      
    rescue => ex
      return ex.to_s
    end

    node.addRule(rule)
    if update
      msg = "Rule #{sid} was updated."
    else 
      msg = "Rule added to the rule-list."
    end
    @diag.if_level(2) { puts! msg }
    return msg
  end
  
  # Remove a rule from the rule-list
  def remove_rule(tid, sid)
    begin
      node = get_node(tid)
      node.deleteRule(sid)
      msg = "Rule #{sid} was removed from the rule-list."
      @diag.if_level(2) { puts! msg }
      return msg
    rescue => ex
      return ex.to_s
    end
  end

  # Add a variable to the variable-list
  def add_variable(tid, update, name, value, description)
    node = get_node(tid)
    begin
      variable = com.untangle.node.ips.IPSVariable.new(name, value, description)
    rescue => ex
      return ex.to_s
    end

    node.addVariable(variable)
    if update
      msg = "Variable #{name} was updated."
    else 
      msg = "Variable added to the variable-list."
    end
    @diag.if_level(2) { puts! msg }
    return msg
  end
  
  # Remove a variable from the variable-list
  def remove_variable(tid, var_name)
    begin
      node = get_node(tid)
      node.deleteVariable(var_name)
      msg = "Variable #{var_name} was removed from the variable-list."
      @diag.if_level(2) { puts! msg }
      return msg
    rescue => ex
      return ex.to_s
    end
  end
  
  #-- Helper methods
  def get_node(tid)
    @@uvmRemoteContext.nodeManager.nodeContext(tid).node
  end
  
  def get_rules(tid)
    get_node(tid).getIPSSettings.getRules
  end
  
  def get_variables(tid)
    get_node(tid).getIPSSettings.getVariables
  end

end
