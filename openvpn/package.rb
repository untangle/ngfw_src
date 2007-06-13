# -*-ruby-*-

NodeBuilder.makeNode(BuildEnv::SRC, 'openvpn')

openvpn = BuildEnv::SRC['openvpn-node']


deps = Jars::Base + [BuildEnv::SRC['uvm']['api']]

jt = [JarTarget.buildTarget(openvpn, deps, 'api', 'node/openvpn/api')]

ServletBuilder.new(openvpn, 'com.untangle.node.openvpn.servlet','node/openvpn/servlets/openvpn', [], jt)

