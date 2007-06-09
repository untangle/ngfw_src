# -*-ruby-*-

NodeBuilder.makeNode(BuildEnv::SRC, 'openvpn')

openvpn = BuildEnv::SRC['openvpn-node']


deps = Jars::Base + [BuildEnv::SRC['uvm']['api']]

jt = [JarTarget.buildTarget(openvpn, deps, 'api', 'tran/openvpn/api')]

ServletBuilder.new(openvpn, 'com.untangle.node.openvpn.servlet','tran/openvpn/servlets/openvpn', [], jt)

