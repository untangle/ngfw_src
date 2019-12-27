/**
 * Bootstrap file loading resources
 */

var rpc = {}; // global rpc object

Ext.define('Bootstrap', {
    singleton: true,

    servletContext: 'ADMIN',

    initRpc: function () {
        // initialize rpc
        if (this.servletContext === 'ADMIN')  {
            var startUpInfo;
            rpc = new JSONRpcClient('/admin/JSON-RPC');
            try { startUpInfo = rpc.UvmContext.getWebuiStartupInfo(); } catch (ex) { alert(ex); }
            Ext.apply(rpc, startUpInfo);
        }
        else if (this.servletContext === 'REPORTS') {
            rpc = new JSONRpcClient('/reports/JSON-RPC');
            // add timezone offset to rpc
            rpc.timeZoneOffset = rpc.ReportsContext.getTimeZoneOffset();
        }
        // else if (this.servletContext === 'QUARANTINE') {
        //     console.log('init here');
        //     rpc = new JSONRpcClient('/quarantine/JSON-RPC').Quarantine;
        //     rpc.timeZoneOffset = rpc.ReportsContext.getTimeZoneOffset();
        // }
    },

    initTranslations: function () {
        // initialize translations
        var languageManager, languageSettings, lang = 'en';

        // QUARANTINE does not seem to have access to translations
        if (this.servletContext === 'QUARANTINE') {
            String.prototype.t = function() { return this.valueOf(); };
            return;
        }

        // set languageManager
        if (this.servletContext === 'ADMIN') {
            languageManager = rpc.UvmContext.languageManager();
        }
        if (this.servletContext === 'REPORTS') {
            languageManager = rpc.ReportsContext.languageManager();
        }
        if(languageManager != null){
            // set languageSettings
            languageSettings = languageManager.getLanguageSettings();
            lang = languageSettings.language;
        }

        // for REPORTS need to fetch translations as for ADMIN there are already set
        if (languageManager != null) {
            rpc.translations = languageManager.getTranslations(lang).map;
        }

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
                },
                lang: {
                    noData: 'Please wait ...'.t()
                }
            });
        }
    },

    load: function (scriptsArray, servletContext, cb) {
        var me = this;

        me.servletContext = servletContext;

        if (servletContext === 'ADMIN' || servletContext === 'REPORTS') {
            me.initRpc(); me.initTranslations(); me.initHighcharts();
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

            return this.owner.then(onFulfilled, onRejected, onProgress).otherwise(function(ex){
                Util.handleException(ex);
                throw(ex);
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

        /**
         * Setup reference mapping to include js/common files as compiled to /script/common/app-APP-all.js
         */
        var appProperties = null;
        if (this.servletContext === 'ADMIN'){
            if(rpc.appManager.app("reports") != null) {
                reportsManager = rpc.appManager.app("reports").getReportsManager();
                appProperties = reportsManager.getCurrentApplications();
            }
            if(appProperties == null){
                appProperties = rpc.appManager.getAllAppProperties();
            }
        }else if (this.servletContext === 'REPORTS') {
            reportsManager = rpc.ReportsContext.reportsManager();
            appProperties = reportsManager.getCurrentApplications();
        }

        var referenceMapping = {};
        if(appProperties != null){
            appProperties.list.forEach( function(appProperties){
                var appReference = 'Ung.common.'+appProperties['name'].replace(/\-/g, '');
                var appFilename = appProperties['name'];
                referenceMapping[appReference] = '/script/common/app-'+appFilename+'-all.js';
            });
            Ext.Loader.addClassPathMappings(referenceMapping);
        }
    }
});
