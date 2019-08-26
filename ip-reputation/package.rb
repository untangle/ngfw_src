# -*-ruby-*-
webroot = BuildEnv::SRC['untangle-app-webroot']

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-app-ip-reputation', 'ip-reputation', [webroot['src']])
