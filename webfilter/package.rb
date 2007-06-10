# -*-ruby-*-

http = BuildEnv::SRC['http-casing']
webfilter = BuildEnv::SRC['webfilter-node']

NodeBuilder.makeNode(BuildEnv::SRC, 'webfilter',
                     [http['localapi']], [http['gui']])

deps = [webfilter['gui'], http['gui']]

ServletBuilder.new(webfilter, 'com.untangle.node.webfilter.jsp',
                   "#{SRC_HOME}/tran/webfilter/servlets/webfilter", [],
                   deps, [], [BuildEnv::SERVLET_COMMON])
