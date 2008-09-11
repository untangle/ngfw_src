## A rush script for retrieving an authentication token for an email address.

## (Wherever the rush shell is)
## To run use: ./dist/usr/bin/rush ./openvpn/unittest/wizard.rb 

nm = Untangle::RemoteUvmContext.nodeManager()
tid = nm.nodeInstances( 'untangle-node-openvpn' ).first
openvpn = nm.nodeContext( tid ).node

puts "Initializing openvpn wizard"
openvpn.startConfig("SERVER_ROUTE")
groupList = openvpn.getAddressGroups()
puts "Initialized the wizard with the following address groups"
groupList["groupList"]["list"].each do |group|
  puts "  group : #{group["name"]}, #{group["address"]}/#{group["netmask"]}"
end

puts "Initialized the wizard with the following exports"
exportList = openvpn.getExportedAddressList()
exportList["exportList"]["list"].each do |export|
  puts "  export : #{export["name"]}, #{export["network"]}/#{export["netmask"]}"
end
