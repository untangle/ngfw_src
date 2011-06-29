echo '
require "untangle/remote_uvm_context"

nm = Untangle::RemoteUvmContext.networkManager()
access_settings =  nm.getAccessSettings()

is_enabled = access_settings["isOutsideAdministrationEnabled"]
puts "remote administration is currently #{is_enabled ? "enabled" : "disabled"}"

access_settings["isOutsideAdministrationEnabled"] = ( ARGV[0] != "false" )
nm.setAccessSettings( access_settings )

access_settings =  nm.getAccessSettings()
is_enabled = access_settings["isOutsideAdministrationEnabled"]
puts "remote administration is now #{is_enabled ? "enabled" : "disabled"}"' | /usr/bin/rush
