# -*-ruby-*-

http = BuildEnv::SRC['untangle-app-http']
web_filter = BuildEnv::SRC['untangle-base-web-filter']

AppBuilder.makeBase(BuildEnv::SRC, 'untangle-base-web-filter', 'web-filter-base', [http['src']])

deps = [web_filter['src'], http['src']]

ServletBuilder.new(web_filter, 'com.untangle.app.web_filter.jsp', "./web-filter-base/servlets/web-filter", [], deps, [], [BuildEnv::SERVLET_COMMON])


