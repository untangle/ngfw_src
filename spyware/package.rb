# -*-ruby-*-

http = BuildEnv::SRC['untangle-casing-http']
spyware = BuildEnv::SRC['untangle-node-spyware']

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-spyware', 'spyware', [http['src']])

deps = [http['src'], spyware['src']]

ServletBuilder.new(spyware, 'com.untangle.node.spyware.jsp', "./spyware/servlets/spyware", [], deps, [], [BuildEnv::SERVLET_COMMON])
