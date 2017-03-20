# -*-ruby-*-

http = BuildEnv::SRC['untangle-casing-http']
web_filter = BuildEnv::SRC['web-filter-base']

AppBuilder.makeBase(BuildEnv::SRC, 'web-filter-base', 'web-filter-base', [http['src']])

deps = [web_filter['src'], http['src']]

ServletBuilder.new(web_filter, 'com.untangle.app.web_filter.jsp', "./web-filter-base/servlets/web-filter", [], deps, [], [BuildEnv::SERVLET_COMMON])


