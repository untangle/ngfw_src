# -*-ruby-*-

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-openvpn', 'openvpn')

openvpn = BuildEnv::SRC['untangle-node-openvpn']


deps = Jars::Base + [BuildEnv::SRC['untangle-uvm']['api']]

jt = [JarTarget.buildTarget(openvpn, deps, 'api', 'openvpn/api')]

ServletBuilder.new(openvpn, 'com.untangle.node.openvpn.servlet','openvpn/servlets/openvpn', [], jt)

