# -*-ruby-*-

http = Package['http-casing']
spyware = Package['spyware-transform']

TransformBuilder.makeTransform( "spyware", [ http["localapi"] ] )

deps = [Package['mvvm']['api'], spyware['gui']]

ServletBuilder.new(spyware, 'com.metavize.tran.spyware.jsp',
                   'tran/spyware/servlets/spyware', deps, [],
                   [$BuildEnv.servletcommon])
