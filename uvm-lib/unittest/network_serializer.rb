## A rush script for retrieving the network information

networkManager = Untangle::RemoteUvmContext.networkManager()
settings = networkManager.getNetworkSettings()

settings["networkSpaceList"]["list"].each do |space|
  space["networkList"]["list"].each do |networkRule|
    puts "ip network: #{space["name"]} #{networkRule["IPNetwork"]["network"]}/#{networkRule["IPNetwork"]["netmask"]}"
  end
end


