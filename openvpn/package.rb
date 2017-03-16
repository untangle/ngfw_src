# -*-ruby-*-

AppBuilder.makeApp(BuildEnv::SRC, 'openvpn', 'openvpn')

openvpn = BuildEnv::SRC['openvpn']

ServletBuilder.new(openvpn, 'com.untangle.app.openvpn.servlet','openvpn/servlets/openvpn', [], [openvpn['src']])

