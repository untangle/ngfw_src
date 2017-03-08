# -*-ruby-*-

deps = []

http = BuildEnv::SRC['untangle-casing-http']
deps << http['src']

web_filter_base = BuildEnv::SRC['untangle-base-web-filter']
deps << web_filter_base['src']

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-web-filter', 'web-filter', deps )
