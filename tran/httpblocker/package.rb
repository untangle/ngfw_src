# -*-ruby-*-

http = Package['http-casing']
httpblocker = Package['httpblocker-transform']

TransformBuilder.makeTransform('httpblocker', [http['localapi']], [http['gui']])

ServletBuilder.new(httpblocker, 'com.metavize.tran.httpblocker.jsp',
                   'tran/httpblocker/servlets/httpblocker', [], [],
                   [$BuildEnv.servletcommon])
