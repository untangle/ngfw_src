Ext.namespace('Ung');
Ext.namespace('Ung.SetupWizard');

// The location of the blank pixel image
Ext.BLANK_IMAGE_URL = 'ext/resources/images/default/s.gif';
// the main internationalization object
var i18n=null;
// the main json rpc object
var rpc = {};

Ung.SetupWizard.LabelWidth = 200;
Ung.SetupWizard.LabelWidth2 = 214;
Ung.SetupWizard.LabelWidth3 = 70;
Ung.SetupWizard.LabelWidth4 = 100;

Ung.SetupWizard.Language = Ext.extend(Object, {
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

        this.panel = new Ext.FormPanel({
            defaultType : 'fieldset',
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
                    width : 200,
                    listWidth : 205,
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

            onValidate : this.validateSettings.createDelegate(this)
        };
    },

    validateSettings : function(){
        var rv = _validate(this.panel.items.items);
        return rv;
    },

    saveSettings : function( handler )
    {
        var language = this.panel.find( "name", "language" )[0].getValue();
        rpc.setup.setLanguage( this.complete.createDelegate( this, [ handler ], true ), language );
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
        parent.location = "index.do";
    },

    enableHandler : function()
    {
        this.card.onNext = this.saveSettings.createDelegate( this );
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

        var language = new Ung.SetupWizard.Language();

        this.wizard = new Ung.Wizard({
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
