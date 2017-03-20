# -*-ruby-*-

http = BuildEnv::SRC['untangle-app-http']

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-app-captive-portal', 'captive-portal', [http['src']])

