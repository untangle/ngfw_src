# -*-ruby-*-

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-openvpn', 'openvpn')

openvpn = BuildEnv::SRC['untangle-node-openvpn']


deps = Jars::Base + [BuildEnv::SRC['untangle-libuvm']['api']]


jt = [openvpn['api']]
#jt = [JarTarget.build_target(openvpn, deps, 'api', 'openvpn/api')]

ServletBuilder.new(openvpn, 'com.untangle.node.openvpn.servlet','openvpn/servlets/openvpn', [], jt)

