# -*-ruby-*-

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-ips', 'ips',
                     [BuildEnv::SRC['untangle-casing-http']['api']])
