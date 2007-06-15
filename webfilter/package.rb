# -*-ruby-*-

http = BuildEnv::SRC['untangle-casing-http']

webfilter = BuildEnv::SRC['untangle-node-webfilter']

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-webfilter', 'webfilter',
                     [http['localapi']], [http['gui']])

deps = [webfilter['gui'], http['gui']]

ServletBuilder.new(webfilter, 'com.untangle.node.webfilter.jsp',
                   "#{SRC_HOME}/tran/webfilter/servlets/webfilter", [],
                   deps, [], [BuildEnv::SERVLET_COMMON])
