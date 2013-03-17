# -*-ruby-*-

deps = [BuildEnv::SRC['untangle-casing-http']['api']] 

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-ips', 'ips', deps, deps)
