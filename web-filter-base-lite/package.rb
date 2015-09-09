# -*-ruby-*-

http = BuildEnv::SRC['untangle-casing-http']
webfilter = BuildEnv::SRC['untangle-base-web-filter-lite']

NodeBuilder.makeBase(BuildEnv::SRC, 'untangle-base-web-filter-lite', 'web-filter-base-lite', [http['src']])

deps = [webfilter['src'], http['src']]

