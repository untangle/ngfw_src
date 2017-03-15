# -*-ruby-*-

http = BuildEnv::SRC['http']

NodeBuilder.makeNode(BuildEnv::SRC, 'ad-blocker', 'ad-blocker', [http['src']])
