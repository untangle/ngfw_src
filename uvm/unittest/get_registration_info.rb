## A rush script for getting the openvpn client link

## (Wherever the rush shell is)
## To run use: ./dist/usr/bin/rush ./openvpn/unittest/get_client_link.rb 

am = Untangle::RemoteUvmContext.adminManager()

registration_info =  am.getRegistrationInfo()

registration_info.each do |k,v| 
  next puts "registrationInfo[#{k}] = '#{v}'"  if k != "misc"
  v["map"].each { |k,v| puts "registrationInfo[misc][#{k}] = '#{v}'" }
end

