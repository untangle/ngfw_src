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
-- To be implemented.
    HELP
  end

  def cmd_rule_list(tid)
    ret = "#,category,block,log,description,info URL,sid,signature\n"
    get_rules(tid).each_with_index { |rule, index|
      ret << [(index+1).to_s,
              rule.getCategory,
#              pattern.getProtocol,
#              pattern.isBlocked.to_s,
              rule.getLog.to_s,
              rule.getDescription,
              rule.getRule
             ].join(",")
      ret << "\n"
    }
    return ret
  end

  #-- Helper methods
  def get_rules(tid)
    @@uvmRemoteContext.nodeManager.nodeContext(tid).node.getIPSSettings.getRules
  end

end
