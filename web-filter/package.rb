# -*-ruby-*-

deps = []

http = BuildEnv::SRC['untangle-casing-http']
deps << http['src']

web_filter_base = BuildEnv::SRC['web-filter-base']
deps << web_filter_base['src']

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-node-web-filter', 'web-filter', deps )
