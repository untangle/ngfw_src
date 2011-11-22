# -*-ruby-*-

http = BuildEnv::SRC['untangle-casing-http']

webfilter = BuildEnv::SRC['untangle-base-webfilter']

NodeBuilder.makeBase(BuildEnv::SRC, 'untangle-base-webfilter', 'webfilter-base', [http['api']], [http['api']])

deps = [webfilter['impl'], webfilter['api'], http['api']]

ServletBuilder.new(webfilter, 'com.untangle.node.webfilter.jsp',
                   "./webfilter-base/servlets/webfilter", [],
                   deps, [], [BuildEnv::SERVLET_COMMON])
