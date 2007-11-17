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

class Attack < UVMFilterNode
    
    UVM_NODE_NAME = "untangle-node-shield"
    NODE_NAME = "Attack Blocker"
    MIB_ROOT = UVM_FILTERNODE_MIB_ROOT + ".3"

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
- attack -- enumerate all web filters running on effective #{BRAND} server.
- attack <TID> list
    -- Display rule list for attack TID
- attack <TID> add enable action log traffic_type direction src-addr dst-addr src-port dst-port category description
    -- Add item to rule-list by type (or update) with specified block and log settings.
- attack <TID> remove [rule-number]
    -- Remove item '[rule-number]' from rule list.
    HELP
    end

    #
    # Command handlers.
    #
    def cmd_list(tid, *args)
        return list_rules(tid)
    end
    
    def cmd_add(tid, *args)
        return add_rule(tid, -1, *args)
    end
    
    def cmd_update(tid, *args)
        return add_rule(tid, *args)
    end
    
    def cmd_remove(tid, *args)
        return remove_rule(tid, *args)
    end
    
    def list_rules(tid)
        begin
            node_ctx = @@uvmRemoteContext.nodeManager.nodeContext(tid)
            node = node_ctx.node()
            settings =  node.getSettings()
            ruleList = settings.getShieldNodeRuleList()
            rules = "#,enabled,address/netmask,user count,description\n"
            rule_num = 1
            ruleList.each { |rule|
                rules << "#{rule_num},#{rule.isLive().to_s},#{rule.getAddress()},'#{rule.getDescription()}'\n"
                rule_num += 1                
            }
            return rules
        rescue Exception => ex
            msg = "Error: #{self.class}.list_rules caught an unhandled exception -- " + ex
            @diag.if_level(3) { puts! msg ; p ex }
            return msg
        end
    end

    def add_rule(tid, rule_num, enable, address, user_count, description=nil)
	
	if !["true", "false"].include?(enable)
	    msg = "Error: invalid value for 'enable' - valid values are 'true' and 'false'."
            @diag.if_level(3) { puts! msg }
            return msg
	elsif !(address =~ /(\d+\.)+\d+/)
            msg = "Error: invalid value for 'address' - valid values are well formatted IP addresses plus optional netmask, e.g., 192.168.0.10[/255.255.255.0]"
            @diag.if_level(3) { puts! msg }
            return msg
	elsif !["5", "25", "50", "100"].include?(user_count)
	    msg = "Error: invalid value for 'user-count' - valid values are 5, 25, 50, 100."
            @diag.if_level(3) { puts! msg }
            return msg
	end
	
	# Add rule...
        begin
            node_ctx = @@uvmRemoteContext.nodeManager.nodeContext(tid)
            node = node_ctx.node()
            settings =  node.getSettings()
            ruleList = settings.getShieldNodeRuleList()

            rule_num = rule_num.to_i
            if (rule_num < 1 || rule_num > ruleList.length) && (rule_num != -1)
                msg = "Error: invalid rule number - valid values are 1...#{ruleList.length}"
                @diag.if_level(3) { puts! msg }
                return msg
            end
            
	    rule = com.untangle.node.shield.ShieldNodeRule.new
	    rule.setLive(enable == "true")
	    rule.setCategory("[no category]")
	    rule.setDescription(description) if description
	    begin 
                rule.setAddress(com.untangle.uvm.node.IPaddr.parse(address))
	    rescue Exception
                msg = "Error: invalid IP address '#{address}'"
                @diag.if_level(3) { puts! msg }
                return msg                
            end
	    
	    (rule_num == -1) ? ruleList.add(rule) : ruleList[rule_num-1] = rule
	    settings.setShieldNodeRuleList(ruleList)
	    node.setSettings(settings)
	    
	    msg = (rule_num != -1) ? "Rule ##{rule_num} updated in #{get_node_name} rule list." : "Rule added to #{get_node_name} rule list."
	    @diag.if_level(3) { puts! msg }
            return msg
        rescue Exception => ex
            msg = "Error: #{self.class}.add_rule caught an unhandled exception -- " + ex
            @diag.if_level(3) { puts! msg ; p ex }
            return msg
        end
    end

    def remove_rule(tid, rule_num)
	
	# Remove rule...
        begin
            node_ctx = @@uvmRemoteContext.nodeManager.nodeContext(tid)
            node = node_ctx.node()
            settings =  node.getSettings()
            ruleList = settings.getShieldNodeRuleList()

            rule_num = rule_num.to_i
            if (rule_num < 1 || rule_num > ruleList.length)
                return "Error: invalid rule number - valid values are 1...#{ruleList.length}"
            end

            ruleList.remove(ruleList[rule_num-1])
	    settings.setShieldNodeRuleList(ruleList)
	    node.setSettings(settings)
	    
	    msg = "Rule #{rule_num} removed from #{get_node_name} rule list."
	    @diag.if_level(2) { puts! msg }
            return msg
        rescue Exception => ex
            msg = "Error: #{self.class}.remove_rule caught an unhandled exception -- " + ex
            @diag.if_level(2) { puts! msg ; p ex }
            return msg
        end
    end
   
end # Attack
