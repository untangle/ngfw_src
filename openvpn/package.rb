# -*-ruby-*-

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-app-openvpn', 'openvpn')

openvpn = BuildEnv::SRC['untangle-app-openvpn']

ServletBuilder.new(openvpn, 'com.untangle.app.openvpn.servlet','openvpn/servlets/openvpn', [], [openvpn['src']])

