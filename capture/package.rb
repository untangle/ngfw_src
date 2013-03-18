# -*-ruby-*-

http = BuildEnv::SRC['untangle-casing-http']

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-capture', 'capture', [http['src']])

