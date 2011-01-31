
default_policy = Untangle::RemoteUvmContext.policyManager().getDefaultPolicy()
toolbox_manager = Untangle::RemoteUvmContext.toolboxManager()

ARGV.each do |node_name|
  mackage_desc = toolbox_manager.packageDesc( node_name )
  puts "mackage_desc['type']: #{mackage_desc['type']}"
  puts "SERVICE" if mackage_desc['type'] == "SERVICE"
end
