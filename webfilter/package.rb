# -*-ruby-*-

http = BuildEnv::SRC['untangle-casing-http']
webfilter = BuildEnv::SRC['untangle-base-webfilter']

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-webfilter', 'webfilter', [http['src'], webfilter['src']])
