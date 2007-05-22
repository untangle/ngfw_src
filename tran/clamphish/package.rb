# -*-ruby-*-

mail = BuildEnv::ALPINE['mail-casing']
http = BuildEnv::ALPINE['http-casing']
spam = BuildEnv::ALPINE['spam-base']
clamphish = BuildEnv::ALPINE['clamphish-transform']

TransformBuilder.makeTransform(BuildEnv::ALPINE, 'clamphish',
                               [mail['localapi'], http['localapi']],
                               [mail['gui'], http['gui'] ], [],
                               ['spam', 'clam-base'])

deps = [http['gui'], clamphish['gui'], spam['gui']]

ServletBuilder.new(clamphish, 'com.untangle.tran.clamphish.jsp',
                   "#{ALPINE_HOME}/tran/clamphish/servlets/idblocker", [],
                   deps, [], [BuildEnv::SERVLET_COMMON])
