# -*-ruby-*-

http = BuildEnv::SRC['untangle-app-http']

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-app-ad-blocker', 'ad-blocker', [http['src']])
