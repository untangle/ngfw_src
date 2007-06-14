# -*-ruby-*-

http = BuildEnv::SRC['untangle-casing-http']
webfilter = BuildEnv::SRC['webfilter-node']

NodeBuilder.makeNode(BuildEnv::SRC, 'webfilter',
                     [http['localapi']], [http['gui']])

deps = [webfilter['gui'], http['gui']]

ServletBuilder.new(webfilter, 'com.untangle.node.webfilter.jsp',
                   "#{SRC_HOME}/webfilter/servlets/webfilter", [],
                   deps, [], [BuildEnv::SERVLET_COMMON])
