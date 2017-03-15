# -*-ruby-*-

http = BuildEnv::SRC['http']

NodeBuilder.makeNode(BuildEnv::SRC, 'captive-portal', 'captive-portal', [http['src']])

