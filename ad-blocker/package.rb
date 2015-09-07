# -*-ruby-*-

http = BuildEnv::SRC['untangle-casing-http']

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-ad-blocker', 'ad-blocker', [http['src']])
