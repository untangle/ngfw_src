# -*-ruby-*-

http = BuildEnv::SRC['untangle-casing-http']
webfilter = BuildEnv::SRC['untangle-base-web-filter-lite']

NodeBuilder.makeBase(BuildEnv::SRC, 'untangle-base-web-filter-lite', 'webfilter-base', [http['src']])

deps = [webfilter['src'], http['src']]

