# -*-ruby-*-

http = Package['http-casing']
spyware = Package['spyware-transform']

TransformBuilder.makeTransform('spyware', [http["localapi"]], [http["localapi"]])

deps = [spyware['gui']]

ServletBuilder.new(spyware, 'com.untangle.tran.spyware.jsp',
                   'tran/spyware/servlets/spyware', [], deps, [],
                   [$BuildEnv.servletcommon])
