/**
 * Bootstrap file loading resources
 */

var rpc = {}; // global rpc object

 Ext.define('Bootstrap', {
    singleton: true,

    initRpc: function () {
        // initialize rpc;
        rpc = new JSONRpcClient('/admin/JSON-RPC');
        try { var startUpInfo = rpc.UvmContext.getWebuiStartupInfo(); } catch (ex) { alert(ex); }
        Ext.apply(rpc, startUpInfo);
    },

    initTranslations: function () {
        // initialize translations
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
        Highcharts.setOptions({
            global: {
                timezoneOffset: -(rpc.timeZoneOffset / 60000)
            }
        });
    },

    load: function (scriptsArray, cb) {
        var me = this;
        me.initRpc(); me.initTranslations(); me.initHighcharts();

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
