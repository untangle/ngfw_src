
nm = Untangle::RemoteUvmContext.networkManager()

address_settings =  nm.getAddressSettings()

puts "Current HTTPS port is #{address_settings["httpsPort"]}"
address_settings["httpsPort"] = ARGV[0].to_i

nm.setAddressSettings2( "aoeuaoeuaoeu" )

address_settings =  nm.getAddressSettings()
puts "New HTTPS port is #{address_settings["httpsPort"]}"

