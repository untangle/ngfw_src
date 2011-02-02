destination_address = java.net.InetAddress.getByName( "10.0.12.1" )

ip_matcher_factory = com.untangle.uvm.node.firewall.ip.IPMatcherFactory
ip_matcher = ip_matcher_factory.parse( "10.0.11.0/255.255.248.0" )

puts "#{ip_matcher.toString()}.isMatch( #{destination_address} ): #{ip_matcher.isMatch( destination_address )}"

