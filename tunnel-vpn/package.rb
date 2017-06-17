# -*-ruby-*-

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-app-tunnel-vpn', 'tunnel-vpn')

tunnel_vpn = BuildEnv::SRC['untangle-app-tunnel-vpn']

ServletBuilder.new(tunnel_vpn, 'com.untangle.app.tunnel_vpn.servlet','tunnel-vpn/servlets/tunnel-vpn', [], [tunnel_vpn['src']])
