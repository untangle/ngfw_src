# -*-ruby-*-

http = BuildEnv::SRC['untangle-casing-http']

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-webfilter', 'webfilter', [http['api']], { 'webfilter-base' => BuildEnv::SRC['untangle-base-webfilter'] })
