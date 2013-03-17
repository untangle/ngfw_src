# -*-ruby-*-

deps = [BuildEnv::SRC['untangle-casing-ftp']['api']]
NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-router', 'router', deps)
