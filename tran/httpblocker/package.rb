# -*-ruby-*-

http = BuildEnv::ALPINE['http-casing']
httpblocker = BuildEnv::ALPINE['httpblocker-transform']

TransformBuilder.makeTransform(BuildEnv::ALPINE, 'httpblocker',
                               [http['localapi']], [http['gui']])

deps = [httpblocker['gui'], http['gui']]

ServletBuilder.new(httpblocker, 'com.untangle.tran.httpblocker.jsp',
                   "#{ALPINE_HOME}/tran/httpblocker/servlets/httpblocker", [],
                   deps, [], [BuildEnv::SERVLET_COMMON])
