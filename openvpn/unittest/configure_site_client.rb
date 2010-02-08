## A rush script for testing the list of default VPN exports

## (Wherever the rush shell is)
## To run use: ./dist/usr/bin/rush ./openvpn/unittest/wizard.rb 

vpn_client = ARGV[0]
raise "USAGE: configure_site_client.rb <path-to-file>"

nm = Untangle::RemoteUvmContext.nodeManager()
tid = nm.nodeInstances( 'untangle-node-openvpn' ).first
openvpn = nm.nodeContext( tid ).node

puts "Initializing openvpn wizard"
puts "Installing VPN Client from the file #{vpn_client}"
openvpn.startConfig("CLIENT")
openvpn.installClientConfig( vpn_client )
openvpn.completeConfig()


