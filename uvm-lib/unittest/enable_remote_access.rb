
nm = Untangle::RemoteUvmContext.networkManager()

access_settings =  nm.getAccessSettings()

is_enabled = access_settings["isOutsideAdministrationEnabled"]
puts "remote administration is currently #{is_enabled ? "enabled" : "disabled"}"

access_settings["isOutsideAdministrationEnabled"] = true

nm.setAccessSettings( access_settings )

access_settings =  nm.getAccessSettings()
is_enabled = access_settings["isOutsideAdministrationEnabled"]
puts "remote administration is now #{is_enabled ? "enabled" : "disabled"}"



