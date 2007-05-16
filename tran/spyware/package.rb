# -*-ruby-*-

http = Package['http-casing']
spyware = Package['spyware-transform']

TransformBuilder.makeTransform('spyware', [http["localapi"]], [http["localapi"]])

deps = [spyware['gui'], http['gui']]

ServletBuilder.new(spyware, 'com.untangle.tran.spyware.jsp',
                   "#{ALPINE_HOME}/tran/spyware/servlets/spyware", [], deps, [],
                   [$BuildEnv.servletcommon])
