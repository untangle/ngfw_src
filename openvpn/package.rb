# -*-ruby-*-

NodeBuilder.makeNode(BuildEnv::SRC, 'openvpn')

openvpn = BuildEnv::SRC['openvpn-node']


deps = Jars::Base + [BuildEnv::SRC['uvm']['api']]

jt = [JarTarget.buildTarget(openvpn, deps, 'api', 'openvpn/api')]

ServletBuilder.new(openvpn, 'com.untangle.node.openvpn.servlet','openvpn/servlets/openvpn', [], jt)

