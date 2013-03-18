# -*-ruby-*-

deps = [BuildEnv::SRC['untangle-casing-ftp']['src']]

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-router', 'router', deps)
