## A rush script for retrieving the network information

networkManager = Untangle::RemoteUvmContext.networkManager()
settings = networkManager.getNetworkSettings()

bridge_map = {}
settings["interfaceList"]["list"].each do |i|
  bridge = bridge_map[i["networkSpace"]["name"]]
  bridge = [] if bridge.nil?
  system_name = i["systemName"]
  next if system_name == "tun0"
  bridge << system_name

  bridge_map[i["networkSpace"]["name"]] = bridge
end

settings["networkSpaceList"]["list"].each do |space|
  space["networkList"]["list"].each do |networkRule|
    i_name = ( bridge_map[space["name"]] or []).join( "," )
    puts "ip network: #{space["name"]}[#{i_name}] #{networkRule["IPNetwork"]["network"]}/#{networkRule["IPNetwork"]["netmask"]}"
  end
end


