# -*-ruby-*-
http = BuildEnv::SRC['untangle-app-http']
webroot = BuildEnv::SRC['untangle-app-webroot']
ip_reputation = BuildEnv::SRC['untangle-app-ip-reputation']

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-app-ip-reputation', 'ip-reputation', [http['src'], webroot['src']])

deps = [ip_reputation['src'], http['src'], webroot['src']]

ServletBuilder.new(ip_reputation, 'com.untangle.app.ip_reputation.jsp', "./ip-reputation/servlets/ip-reputation", [], deps, [], [BuildEnv::SERVLET_COMMON])

