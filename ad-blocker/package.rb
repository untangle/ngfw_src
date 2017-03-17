# -*-ruby-*-

http = BuildEnv::SRC['http']

AppBuilder.makeApp(BuildEnv::SRC, 'ad-blocker', 'ad-blocker', [http['src']])
