# -*-ruby-*-

http = BuildEnv::SRC['untangle-casing-http']
webfilter = BuildEnv::SRC['untangle-base-webfilter']

NodeBuilder.makeBase(BuildEnv::SRC, 'untangle-base-webfilter', 'webfilter-base', [http['src']])

deps = [webfilter['src'], http['src']]

ServletBuilder.new(webfilter, 'com.untangle.node.webfilter.jsp', "./webfilter-base/servlets/webfilter", [], deps, [], [BuildEnv::SERVLET_COMMON])
