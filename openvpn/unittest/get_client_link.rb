## A rush script for getting the openvpn client link

## (Wherever the rush shell is)
## To run use: ./dist/usr/bin/rush ./openvpn/unittest/get_client_link.rb 

nm = Untangle::RemoteUvmContext.nodeManager()
tid = nm.nodeInstances( 'untangle-node-openvpn' ).first
openvpn = nm.nodeContext( tid ).node

link = openvpn.getAdminClientUploadLink()
puts "retrieving the client upload link"
puts "URL: openvpn.getAdminClientUploadLink() = #{link}"

clientName = ARGV[0]
link = openvpn.getAdminDownloadLink( clientName, "SETUP_EXE")
puts "retrieving the client link for #{clientName}"
puts "URL: openvpn.getAdminDownloadLink( clientName, \"SETUP_EXE\") = #{link}"

puts "wget 'http://localhost#{link}'"

link = openvpn.getAdminClientUploadLink()
puts "retrieving the client upload link"
puts "URL: openvpn.getAdminClientUploadLink() = #{link}"



