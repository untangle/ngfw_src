/*global
 Ext, Ung, Webui, JSONRpcClient, rpc:true, i18n:true, window
 */
var rpc = {};

Ext.define('Ung.setupWizard.Language', {
    constructor: function (config) {
        Ext.applyIf(this, config);
        var languageStore = Ext.create('Ext.data.JsonStore', {
            fields: ['code', 'languageName'],
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
                value: this.language,
                labelWidth: 200,
                queryMode: 'local',
                validationEvent: 'blur',
                msgTarget: 'side',
                margin: '50 0 0 0'
            }]
        });

        this.card = {
            title: i18n._('Language'),
            panel: this.panel,
            onValidate: Ext.bind(function () {
                return Ung.Util.validate(this.panel);
            }, this),
            onNext: Ext.bind(function (handler) {
                var language = this.panel.down('combo[name="language"]').getValue();
                rpc.setup.setLanguage(Ext.bind(function (result, exception) {
                    if (Ung.Util.handleException(exception, "Unable to save the language")) {
                        return;
                    }
                    // Send the user to the setup wizard.
                    window.location.href = 'index.do';
                }, this), language);
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
        var language = Ext.create('Ung.setupWizard.Language', {languageList: this.languageList, language: this.language});

        this.wizard = Ext.create('Ung.Wizard', {
            height: 500,
            maxWidth: 800,
            minWidth: 320,
            showLogo: true,
            languageSetup: true,
            cardDefaults: {
                padding: 5
            },
            cards: [ language.card ]
        });
    }
});