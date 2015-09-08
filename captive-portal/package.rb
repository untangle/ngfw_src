# -*-ruby-*-

http = BuildEnv::SRC['untangle-casing-http']

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-captive-portal', 'captive-portal', [http['src']])

