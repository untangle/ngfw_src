# -*-ruby-*-

http = Package['http-casing']
httpblocker = Package['httpblocker-transform']

TransformBuilder.makeTransform(ALPINE_HOME, 'httpblocker', [http['localapi']],
                               [http['gui']])

deps = [httpblocker['gui'], http['gui']]

ServletBuilder.new(httpblocker, 'com.untangle.tran.httpblocker.jsp',
                   "#{ALPINE_HOME}/tran/httpblocker/servlets/httpblocker", [],
                   deps, [], [$BuildEnv.servletcommon])
