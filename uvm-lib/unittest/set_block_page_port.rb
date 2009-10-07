nm = Untangle::RemoteUvmContext.networkManager()

access_settings = nm.getAccessSettings()

puts "Current blockpage port is #{access_settings["blockPagePort"]}"

access_settings["blockPagePort"] = ARGV[0].to_i
puts "Settings blockpage port to #{access_settings["blockPagePort"]}"

nm.setAccessSettings( access_settings )

