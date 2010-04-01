
nm = Untangle::RemoteUvmContext.networkManager()

address_settings =  nm.getAddressSettings()
puts "Current HTTPS port is #{address_settings["httpsPort"]}"
address_settings["httpsPort"] = ARGV[0].to_i

if address_settings["publicAddress"] == nil
   address_settings.delete("publicAddress")
end
if address_settings["publicIPaddr"] == nil
   address_settings.delete("publicIPaddr")
end
nm.setAddressSettings( address_settings )

address_settings =  nm.getAddressSettings()
puts "New HTTPS port is #{address_settings["httpsPort"]}"

