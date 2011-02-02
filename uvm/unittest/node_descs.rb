
node_manager = Untangle::RemoteUvmContext.nodeManager()

default_policy = Untangle::RemoteUvmContext.policyManager().getDefaultPolicy()

node_manager.visibleNodes( default_policy ).each do |node_desc|
  puts <<EOF
node: #{node_desc["name"]}
\tclass-name: #{node_desc["className"]}
\tsingle-instance: #{node_desc["singleInstance"]}
\tdisplay-name: #{node_desc["displayName"]}
\tpower-button: #{node_desc["hasPowerButton"]}
EOF
end


