# -*-ruby-*-

deps = [BuildEnv::SRC['untangle-casing-http']['src']] 

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-ips', 'ips', deps)
