# -*-ruby-*-

http = BuildEnv::SRC['untangle-casing-http']

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-node-captive-portal', 'captive-portal', [http['src']])

