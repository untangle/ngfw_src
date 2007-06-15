# -*-ruby-*-

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-router', 'router',
                     [BuildEnv::SRC['untangle-casing-ftp']['localapi']])
