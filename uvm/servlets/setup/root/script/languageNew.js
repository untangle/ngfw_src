Ext.namespace('Ung');
Ext.namespace('Ung.SetupWizard');

// The location of the blank pixel image
Ext.BLANK_IMAGE_URL = '/ext/resources/images/default/s.gif';
// the main internationalization object
var i18n=null;
// the main json rpc object
var rpc = {};

Ung.SetupWizard.LabelWidth = 200;
Ung.SetupWizard.LabelWidth2 = 214;
Ung.SetupWizard.LabelWidth3 = 70;
Ung.SetupWizard.LabelWidth4 = 100;

Ext.define('Ung.SetupWizard.Language', {
    constructor : function( config )
    {
        this.languageStore = [];
        var c = 0;
        var languageList = Ung.SetupWizard.CurrentValues.languageList.list;
        var length = languageList.length;

        for ( c = 0 ; c < length ; c++ ) {
            var language = languageList[c];
            this.languageStore[c] = [ language.code, language.name ];
        }

        this.panel = Ext.create('Ext.form.Panel', {
            defaultType : 'fieldset',
            border: false,
            defaults : {
                autoHeight : true,
                cls : 'noborder'
            },
            items : [{
                xtype : 'label',
                html : '<h2 class="wizard-title">'+i18n._( "Language Selection" )+'</h2>'
            },{
                defaults : {
                    validationEvent : 'blur',
                    msgTarget : 'side'
                },
                items : [{
                    fieldLabel : i18n._('Please select your language'),
                    name : "language",
                    xtype : 'combo',
                    editable : false,
                    width : 350,
                    labelWidth : 200,
                    store : this.languageStore,
                    value : Ung.SetupWizard.CurrentValues.language,
                    mode : 'local',
                    triggerAction : 'all',
                    listClass : 'x-combo-list-small',
                    ctCls : 'small-top-margin'
                }]
            }]
        });

        this.card = {
            title : i18n._( "Language" ),
            panel : this.panel,

            onValidate : Ext.bind(this.validateSettings,this)
        };
    },

    validateSettings : function(){
        var rv = _validate(this.panel.items.items);
        return rv;
    },

    saveSettings : function( handler )
    {
        var language = this.panel.query('combo[name="language"]')[0].getValue();
        rpc.setup.setLanguage( Ext.bind(this.complete,this, [ handler ], true ), language );
    },

    complete : function( result, exception, foo, handler )
    {
        if ( exception ) {
            var message = exception.message;
            if (message == null || message == "Unknown") {
                message = i18n._("Please Try Again");
            }
            
            Ext.MessageBox.alert("Failed.",message);
            return;
        }

        /* Send the user to the setup wizard. */
        parent.location = "indexNew.do";
    },

    enableHandler : function()
    {
        this.card.onNext = Ext.bind(this.saveSettings, this );
    }
});

Ung.Language = {
    isInitialized : false,
    init : function()
    {
        if ( this.isInitialized == true ) return;
        this.isInitialized = true;

        rpc = {};

        rpc.setup = new JSONRpcClient("/setup/JSON-RPC").SetupContext;

        i18n = new Ung.I18N( { "map" : {} })

        var language = Ext.create('Ung.SetupWizard.Language',{});

        this.wizard = Ext.create('Ung.Wizard',{
            height : 500,
            width : 800,
            cardDefaults : {
                labelWidth : Ung.SetupWizard.LabelWidth,
                cls : 'untangle-form-panel'
            },
            cards : [ language.card ],
            disableNext : false,
            el : "container"
        });

        this.wizard.render();
        Ext.QuickTips.init();
        this.wizard.goToPage( 0 );

        this.wizard.nextButton.setText( "Next &raquo;" );

        /* The on next handler is always called when calling goToPage,
         * this disables it until after starting */
        language.enableHandler();
    }
};
