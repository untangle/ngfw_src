# -*-ruby-*-

http = BuildEnv::SRC['untangle-casing-http']

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-node-ad-blocker', 'ad-blocker', [http['src']])
