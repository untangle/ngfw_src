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

class ProtoFilter < UVMFilterNode

  UVM_NODE_NAME = "untangle-node-protofilter"
  NODE_NAME = "Protocol Filter"
  PROTOFILTER_MIB_ROOT = UVM_FILTERNODE_MIB_ROOT + ".4"

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
    PROTOFILTER_MIB_ROOT
  end  

  def get_help_text()
    return <<-HELP
- protofilter -- enumerate all protocol filter nodes running on effective #{BRAND} server.
- protofilter <TID> list
    -- Display protocol list list for protofilter node TID
- protofilter <TID> add category protocol block log description signature
    -- Add a protocol to the protocol list with specified settings.
- protofilter <TID> update [proto-number] category protocol block log description signature
    -- Update item '[proto-number]' with specified settings.
- protofilter <TID> remove [proto-number]
    -- Remove item '[proto-number]' from protocol list.
- protofilter <TID> <snmp|stats>
    -- Display protocol filter TID statistics (TODO document this)
  HELP
  end

  #
  # Command handlers.
  #
  
  # Default command: list all protocol filters
  # TODO: we should consider moving this method to UVMFilterNode class
  def cmd_(*args)
    return list_filternodes()
  end
  
  # Add a protocol to the list
  def add_protocol(tid, category, protocol, block, log, description, signature)
    update_protocol_helper(tid, -1, category, protocol, block, log, description, signature)
  end

  # Update the protocol at position pos
  def update_protocol(tid, pos, category, protocol, block, log, description, signature)
    update_protocol_helper(tid, pos, category, protocol, block, log, description, signature)
  end

  # Remove a protocol from the list
  def remove_protocol(tid, pos)
    patterns = get_patterns(tid)
    begin
      validate_range(pos, 1..patterns.length, "protocol number")
      patterns.remove(patterns[pos - 1])
      update_patterns(tid, patterns)
      msg = "Protocol #{pos} was removed from the list."
      @diag.if_level(2) { puts! msg }
      return msg
    rescue => ex
      return ex.to_s
    end
  end

  #
  # Command handlers.
  #
  def cmd_list(tid)
    ret = "#,category,protocol,block,log,description,signature\n"
    get_patterns(tid).each_with_index { |pattern, index|
      ret << [(index+1).to_s,
              pattern.getCategory,
              pattern.getProtocol,
              pattern.isBlocked.to_s,
              pattern.getLog.to_s,
              pattern.getDescription,
              pattern.getDefinition,
             ].join(",")
      ret << "\n"
    }
    return ret
  end

  def cmd_add(tid, *args)
    add_protocol(tid, *args)
  end

  def cmd_update(tid, pos, *args)
    update_protocol(tid, pos.to_i, *args)
  end

  def cmd_remove(tid, pos)
    remove_protocol(tid, pos.to_i)
  end

  def cmd_snmp(tid, *args)
    get_statistics(tid, args)
  end

  def cmd_stats(tid, *args)
    get_statistics(tid, args)
  end

  #-- Helper methods
  def get_patterns(tid)
    @@uvmRemoteContext.nodeManager.nodeContext(tid).node.getSettings.getPatterns
  end

  def update_patterns(tid, patterns)
    node = @@uvmRemoteContext.nodeManager.nodeContext(tid).node
    settings = node.getSettings
    settings.setPatterns(patterns)
    node.setSettings(settings)
  end

  def update_protocol_helper(tid, pos, category, protocol, block, log, description, signature)
    patterns = get_patterns(tid)
    begin
      validate_range(pos, 1..patterns.length, "protocol number") unless pos == -1
      validate_bool(block, "block")
      validate_bool(log, "log")
      pattern = com.untangle.node.protofilter.ProtoFilterPattern.new
      pattern.setCategory(category)
      pattern.setProtocol(protocol)
      pattern.setBlocked(block == "true")
      pattern.setLog(log == "true")
      pattern.setDescription(description)
      pattern.setDefinition(signature)
    rescue => ex
      return ex.to_s
    end

    if pos == -1
      patterns.add(pattern)
      msg = "Protocol added to the list."
    else
      patterns[pos - 1] = pattern
      msg = "Protocol #{pos} updated."
    end

    update_patterns(tid, patterns)

    @diag.if_level(2) { puts! msg }
    return msg
  end
end
