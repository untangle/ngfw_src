# -*-ruby-*-

deps = []

http = BuildEnv::SRC['untangle-app-http']
deps << http['src']

web_filter_base = BuildEnv::SRC['untangle-base-web-filter']
deps << web_filter_base['src']

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-app-web-filter', 'web-filter', deps )
