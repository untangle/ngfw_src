# -*-ruby-*-

http = BuildEnv::SRC['untangle-app-http']
AppBuilder.makeCasing(BuildEnv::SRC, 'untangle-app-ssl-inspector', 'ssl-inspector', [http['src']])
