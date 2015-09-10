# -*-ruby-*-

deps = []

http = BuildEnv::SRC['untangle-casing-http']
deps << http['src']

web_filter_base = BuildEnv::SRC['untangle-base-web-filter']
deps << web_filter_base['src']

web_filter_lite = BuildEnv::SRC['untangle-node-web-filter-lite']

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-web-filter-lite', 'web-filter-lite', deps)

deps = [web_filter_base['src'], http['src'], web_filter_lite['src']]

ServletBuilder.new(web_filter_lite, 'com.untangle.node.web_filter_lite.jsp', "./web-filter-lite/servlets/web-filter-lite", [], deps, [], [BuildEnv::SERVLET_COMMON])
