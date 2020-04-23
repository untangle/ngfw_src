# -*-ruby-*-
http = BuildEnv::SRC['untangle-app-http']
webroot = BuildEnv::SRC['untangle-app-webroot']
threat_prevention = BuildEnv::SRC['untangle-app-threat-prevention']

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-app-threat-prevention', 'threat-prevention', [http['src'], webroot['src']])

deps = [threat_prevention['src'], http['src'], webroot['src']]

ServletBuilder.new(threat_prevention, 'com.untangle.app.threat_prevention.jsp', "./threat-prevention/servlets/threat-prevention", [], deps, [], [BuildEnv::SERVLET_COMMON])
