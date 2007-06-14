# -*-ruby-*-

http = BuildEnv::SRC['untangle-casing-http']
spyware = BuildEnv::SRC['spyware-node']

NodeBuilder.makeNode(BuildEnv::SRC, 'spyware', [http["localapi"]],
                     [http["localapi"]])

deps = [spyware['gui'], http['gui']]

ServletBuilder.new(spyware, 'com.untangle.node.spyware.jsp',
                   "#{SRC_HOME}/spyware/servlets/spyware", [], deps,
                   [], [BuildEnv::SERVLET_COMMON])
