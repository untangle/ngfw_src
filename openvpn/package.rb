# -*-ruby-*-

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-openvpn', 'openvpn')

openvpn = BuildEnv::SRC['untangle-node-openvpn']

ServletBuilder.new(openvpn, 'com.untangle.node.openvpn.servlet','openvpn/servlets/openvpn', [], [openvpn['src']])

