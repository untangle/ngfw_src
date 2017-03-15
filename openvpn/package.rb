# -*-ruby-*-

NodeBuilder.makeNode(BuildEnv::SRC, 'openvpn', 'openvpn')

openvpn = BuildEnv::SRC['openvpn']

ServletBuilder.new(openvpn, 'com.untangle.node.openvpn.servlet','openvpn/servlets/openvpn', [], [openvpn['src']])

