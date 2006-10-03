# -*-ruby-*-

http = Package['http-casing']
httpblocker = Package['httpblocker-transform']

TransformBuilder.makeTransform('httpblocker', [http['localapi']], [http['gui']])

deps = [Package['mvvm']['api'], httpblocker['gui']]

ServletBuilder.new(httpblocker, 'com.metavize.tran.httpblocker.jsp',
                   'tran/httpblocker/servlets/httpblocker', deps, [],
                   [$BuildEnv.servletcommon])
