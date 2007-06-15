# -*-ruby-*-

http = BuildEnv::SRC['untangle-casing-http']
spyware = BuildEnv::SRC['untangle-node-spyware']

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-spyware', 'spyware', [http["localapi"]],
                     [http["localapi"]])

deps = [spyware['gui'], http['gui']]

ServletBuilder.new(spyware, 'com.untangle.node.spyware.jsp',
                   "#{SRC_HOME}/tran/spyware/servlets/spyware", [], deps,
                   [], [BuildEnv::SERVLET_COMMON])
