# -*-ruby-*-

mail = Package['mail-casing']
http = Package['http-casing']
spam = Package['spam-base']
clamphish = Package['clamphish-transform']

TransformBuilder.makeTransform('clamphish',
                               [mail['localapi'], http['localapi']],
                               [mail['gui'], http['gui'] ], [],
                               ['spam', 'clam-base'])

deps = [http['gui'], clamphish['gui'], spam['gui']]

ServletBuilder.new(clamphish, 'com.untangle.tran.clamphish.jsp',
                   "#{ALPINE_HOME}/tran/clamphish/servlets/idblocker", [], deps, [],
                   [$BuildEnv.servletcommon])
