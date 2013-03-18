# -*-ruby-*-

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-openvpn', 'openvpn')

openvpn = BuildEnv::SRC['untangle-node-openvpn']

jt = [openvpn['src']]

ServletBuilder.new(openvpn, 'com.untangle.node.openvpn.servlet','openvpn/servlets/openvpn', [], jt)

