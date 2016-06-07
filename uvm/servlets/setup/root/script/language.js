/*global
 Ext, Ung, Webui, JSONRpcClient, rpc:true, i18n:true, window
 */
var rpc = {};

Ext.define('Ung.setupWizard.Language', {
    constructor: function (config) {
        Ext.applyIf(this, config);
        var languageStore = Ext.create('Ext.data.JsonStore', {
            fields: ['code', 'name'],
            data: this.languageList.list
        });
        this.panel = Ext.create('Ext.container.Container', {
            items: [{
                xtype: 'combo',
                fieldLabel: i18n._('Please select your language'),
                name: 'language',
                editable: false,
                valueField: 'code',
                displayField: 'languageName',
                store: languageStore,
                value: this.languageSource + "-" + this.language,
                labelWidth: 200,
                queryMode: 'local',
                validationEvent: 'blur',
                msgTarget: 'side',
                margin: '50 0 0 0',
                listeners: {
                    "select": {
                        fn: Ext.bind(function(elem, record) {
                            if(record.get("code") == null){
                                /* Ignore source entries and instead get the next record. */
                                var nextRecord = null;
                                var sourceName = record.get("name"); 
                                var getNext = false;
                                record.store.each(function(record){
                                    if(getNext == true){
                                        nextRecord = record;
                                        return false;
                                    }else if(record.get("name") == sourceName){
                                        getNext = true;
                                    }
                                },this);
                                if(nextRecord == null){
                                    return;
                                }
                                record = nextRecord;
                                elem.setValue(record.get("code"));
                            }
                            var source_language = record.get("code").split("-",2);
                            this.source = source_language[0];
                            this.language = source_language[1];
                        }, this)
                    }
                }
            }]
        });

        this.card = {
            title: i18n._('Language'),
            panel: this.panel,
            onValidate: Ext.bind(function () {
                return Ung.Util.validate(this.panel);
            }, this),
            onNext: Ext.bind(function (handler) {
                var source_language = this.panel.down('combo[name="language"]').getValue().split("-", 2);
                rpc.setup.setLanguage(Ext.bind(function (result, exception) {
                    if (Ung.Util.handleException(exception, "Unable to save the language")) {
                        return;
                    }
                    // Send the user to the setup wizard.
                    window.location.href = 'index.do';
                }, this), source_language[1], source_language[0]);
            }, this)
        };
    }
});

Ext.define("Ung.Language", {
    singleton: true,
    init: function (config) {
        Ext.applyIf(this, config);
        JSONRpcClient.toplevel_ex_handler = Ung.Util.rpcExHandler;
        rpc = {};
        rpc.setup = new JSONRpcClient("/setup/JSON-RPC").SetupContext;
        rpc.setup.getTranslations(Ext.bind(function (result, exception) {
            if (Ung.Util.handleException(exception)) {
                return;
            }
            Ext.applyIf(rpc, result);
            this.initComplete();
        }, this));
    },
    initComplete: function () {
        i18n = new Ung.I18N({ 'map': rpc.translations });
        console.log(this);
        var language = Ext.create('Ung.setupWizard.Language', {languageList: this.languageList, language: this.language, languageSource: this.languageSource});

        Ext.create('Ext.container.Viewport', {
            layout: 'auto',
            border: false,
            items: Ext.create('Ung.Wizard', {
                maxWidth: 800,
                minWidth: 320,
                showLogo: true,
                languageSetup: true,
                cardDefaults: {
                    padding: 5
                },
                cards: [ language.card ]
            })
        });
    }
});