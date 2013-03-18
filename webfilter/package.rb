# -*-ruby-*-

http = BuildEnv::SRC['untangle-casing-http']

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-webfilter', 'webfilter', [http['src']], { 'webfilter-base' => BuildEnv::SRC['untangle-base-webfilter'] })
