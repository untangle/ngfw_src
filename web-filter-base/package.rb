# -*-ruby-*-

http = BuildEnv::SRC['untangle-casing-http']
web_filter = BuildEnv::SRC['untangle-base-web-filter']

NodeBuilder.makeBase(BuildEnv::SRC, 'untangle-base-web-filter', 'web-filter-base', [http['src']])

deps = [web_filter['src'], http['src']]

