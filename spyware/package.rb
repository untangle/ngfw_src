# -*-ruby-*-

http = BuildEnv::SRC['untangle-casing-http']
spyware = BuildEnv::SRC['untangle-node-spyware']

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-spyware', 'spyware',
                     [http["api"]], [http["api"]])

deps = [http['api'], spyware['impl'], spyware['api']]

ServletBuilder.new(spyware, 'com.untangle.node.spyware.jsp', "./spyware/servlets/spyware", [], deps, [], [BuildEnv::SERVLET_COMMON])
