# -*-ruby-*-

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-node-openvpn', 'openvpn')

openvpn = BuildEnv::SRC['untangle-node-openvpn']

ServletBuilder.new(openvpn, 'com.untangle.app.openvpn.servlet','openvpn/servlets/openvpn', [], [openvpn['src']])

