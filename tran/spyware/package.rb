# -*-ruby-*-

http = BuildEnv::ALPINE['http-casing']
spyware = BuildEnv::ALPINE['spyware-transform']

TransformBuilder.makeTransform(BuildEnv::ALPINE, 'spyware', [http["localapi"]],
                               [http["localapi"]])

deps = [spyware['gui'], http['gui']]

ServletBuilder.new(spyware, 'com.untangle.tran.spyware.jsp',
                   "#{ALPINE_HOME}/tran/spyware/servlets/spyware", [], deps,
                   [], [BuildEnv::SERVLET_COMMON])
