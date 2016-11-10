# -*-ruby-*-

deps = []

http = BuildEnv::SRC['untangle-casing-http']
deps << http['src']

web_filter_base = BuildEnv::SRC['untangle-base-web-filter']
deps << web_filter_base['src']

web_filter = HadesBuildEnv['untangle-node-web-filter']

NodeBuilder.makeNode(HadesBuildEnv, 'untangle-node-web-filter', 'web-filter', deps )
deps << web_filter['src']

