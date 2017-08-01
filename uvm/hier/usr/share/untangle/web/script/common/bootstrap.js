/**
 * Bootstrap file loading resources
 */

var rpc = {}; // global rpc object

 Ext.define('Bootstrap', {
    singleton: true,

    servletContext: 'ADMIN',

    initRpc: function () {
        // initialize rpc
        if (this.servletContext === 'ADMIN' || this.servletContext === 'REPORTS') {
            var startUpInfo;
            rpc = new JSONRpcClient('/admin/JSON-RPC');
            try { startUpInfo = rpc.UvmContext.getWebuiStartupInfo(); } catch (ex) { alert(ex); }
            Ext.apply(rpc, startUpInfo);
        }

        if (this.servletContext === 'QUARANTINE') {
            rpc = new JSONRpcClient('/quarantine/JSON-RPC').Quarantine;
        }
    },

    initTranslations: function () {
        // initialize translations

        // if not defined (e.g. quarantine)
        if (!rpc.translations) { String.prototype.t = function() { return this.valueOf(); }; return; }

        if (!rpc.translations.decimal_sep) { rpc.translations.decimal_sep = '.'; }
        if (!rpc.translations.thousand_sep) { rpc.translations.thousand_sep = ','; }
        if (!rpc.translations.date_fmt) { rpc.translations.date_fmt = 'Y-m-d'; }
        if (!rpc.translations.timestamp_fmt) { rpc.translations.timestamp_fmt = 'Y-m-d h:i:s a'; }

        var lang = rpc.languageSettings.language;
        String.prototype.t = function() {
            // special case formatters needed for all languages
            if (Ext.Array.contains(['decimal_sep', 'thousand_sep', 'date_fmt', 'timestamp_fmt'], this.valueOf())) {
                return rpc.translations[this.valueOf()];
            }
            if (lang !== 'en') {
                return rpc.translations[this.valueOf()] || (lang === 'xx' ? '<cite>' + this.valueOf() + '</cite>' : this.valueOf());
            }
            return this.valueOf();
        };
    },

    initHighcharts: function () {
        // set server timezone for charts
        if (window.Highcharts) {
            Highcharts.setOptions({
                global: {
                    timezoneOffset: -(rpc.timeZoneOffset / 60000)
                }
            });
        }
    },

    load: function (scriptsArray, servletContext, cb) {
        var me = this;

        me.servletContext = servletContext;

        if (servletContext === 'ADMIN' || servletContext === 'REPORTS') {
            me.initRpc(); me.initHighcharts(); me.initTranslations();
        }

        // check local storage
        if (Ext.supports.LocalStorage) {
            Ext.state.Manager.setProvider(Ext.create('Ext.state.LocalStorageProvider'));
        }

        // init quicktips
        Ext.QuickTips.init();

        // disable some Aria features
        Ext.enableAria = false;
        Ext.enableAriaButtons = false;
        Ext.enableAriaPanels = false;
        Ext.supports.MouseWheel = false;

        // IMPORTANT! override the default models ext idProperty so it does not interfere with backend 'id'
        Ext.data.Model.prototype.idProperty = '_id';

        // add default onRejected handler to then function of promises
        Ext.promise.Promise.prototype.then = function (onFulfilled, onRejected, onProgress, scope) {
            var ref;

            if (arguments.length === 1 && Ext.isObject(arguments[0])) {
                ref = arguments[0];
                onFulfilled = ref.success;
                onRejected = ref.failure;
                onProgress = ref.progress;
                scope = ref.scope;
            }

            if (scope) {
                if (onFulfilled) {
                    onFulfilled = Ext.Function.bind(onFulfilled, scope);
                }

                if (onRejected) {
                    onRejected = Ext.Function.bind(onRejected, scope);
                }

                if (onProgress) {
                    onProgress = Ext.Function.bind(onProgress, scope);
                }
            }

            return this.owner.then(onFulfilled, onRejected, onProgress).otherwise(function(ex) {
                console.log(ex);
                Util.handleException(ex);
            throw ex;
            });
        };

        // load script dependencies after all initializations
        var fns = [];

        Ext.Array.each(scriptsArray, function (script) {
            if (script.length > 0) { // if empty string script URL, just skip it
                fns.push(function () {
                    var dfrd = new Ext.Deferred();
                    Ext.Loader.loadScript({
                        url: script,
                        onLoad: function () { dfrd.resolve(); },
                        onError: function () { dfrd.reject('Unable to load script: ' + script); }
                    });
                    return dfrd;
                });
            }
        });

        Ext.Deferred.sequence(fns).then(function () {
            cb(false);
        }, function (err) {
            cb(err);
        });
    }
 });
