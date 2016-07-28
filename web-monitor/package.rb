# -*-ruby-*-

deps = []

http = BuildEnv::SRC['untangle-casing-http']
deps << http['src']

web_filter_base = BuildEnv::SRC['untangle-base-web-filter']
deps << web_filter_base['src']

web_monitor = BuildEnv::SRC['untangle-node-web-monitor']

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-web-monitor', 'web-monitor', deps )

