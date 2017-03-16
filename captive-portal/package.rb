# -*-ruby-*-

http = BuildEnv::SRC['http']

AppBuilder.makeApp(BuildEnv::SRC, 'captive-portal', 'captive-portal', [http['src']])

