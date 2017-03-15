# -*-ruby-*-

deps = [BuildEnv::SRC['ftp']['src']]

NodeBuilder.makeNode(BuildEnv::SRC, 'router', 'router', deps)
