Ext.namespace('Ung');
Ext.namespace('Ung.SetupWizard');

// The location of the blank pixel image
Ext.BLANK_IMAGE_URL = 'ext/resources/images/default/s.gif';
// the main internationalization object
var i18n=null;
// the main json rpc object
var rpc = {};

Ung.SetupWizard.LabelWidth = 150;

Ung.SetupWizard.welcome = function()
{
    var panel = new Ext.FormPanel({
        defaultType : 'textfield',
        items : [{
            xtype : 'label',
            html : i18n._( 'This wizard will guide you through the inital setup and configuration of your Untangle Server. Click <b>Next</b> to get started.' )
        }]
    });

    return {
        title : i18n._( "Welcome" ),
        cardTitle : i18n._( "Thanks for using Untangle" ),
        panel : panel
    };
}

Ung.SetupWizard.settings = function()
{
    var panel = new Ext.FormPanel({
        labelWidth : Ung.SetupWizard.LabelWidth,
        defaultType : 'fieldset',
        cls : 'untangle-form-panel',
        defaults : { 
            autoHeight : true,
        },
        items : [{
            defaultType : 'textfield',
            title : i18n._( 'Password' ),
            items : [{
                xtype : 'label',
                html : i18n._( '<b>Password:</b> Please choose a password for the admin account' ),
                border : false
            },{
                fieldLabel : i18n._('Login'),
                disabled : true,
                value : 'Admin'
            },{
                inputType : 'password',
                fieldLabel : i18n._('Password')
            },{
                inputType : 'password',
                fieldLabel : i18n._('Confirm Password')
            }]
        },{
            title : i18n._( 'Timezone' ),
            items : [{
                xtype : 'combo',
                name : 'Timezone',
                editable : false,
                store : Ung.SetupWizard.TimeZoneStore,
                width : 350,
                listWidth : 355,
                hideLabel : true,
                mode : 'local',
                triggerAction : 'all',
                listClass : 'x-combo-list-small'
            }]
        },{
            title : 'Hostname',
            items : [{
                hideLabel : true,
                xtype : 'textfield'
            }]
        },{
            title : 'Auto-Upgrade',
            items : [{
                xtype : 'checkbox',
                hideLabel : true,
                boxLabel : i18n._("Automatically download and install upgrades.")
            },{
                xtype : 'textfield',
                fieldLabel : i18n._("Check for upgrades at")
            }]
        },{
            xtype : 'label',
            fieldLabel : i18n._('Confirm Password')
        }]
    });

    return {
        title : i18n._( "Settings" ),
        cardTitle : i18n._( "Configure your Server" ),
        panel : panel
    };
}

Ung.SetupWizard.registration = function()
{
    var employeeStore = [
        [ 5, i18n._( "1-5" )],
        [ 10, i18n._( "5-10" )],
        [ 50, i18n._( "10-50" )],
        [ 100, i18n._( "50-100" )],
        [ 250, i18n._( "100-250" )],
        [ 500, i18n._( "250-500" )],
        [ 1000, i18n._( "500-1000" )],
        [ 2500, i18n._( "> 1000 " )]];

    var industryStore = [
        [ "finance", i18n._( "Banking" ) ],
        [ "software", i18n._( "Software" ) ]];

    var panel = new Ext.FormPanel({
        labelWidth : Ung.SetupWizard.LabelWidth,
        defaultType : 'fieldset',
        cls : 'untangle-form-panel',
        defaults : { 
            autoHeight : true
        },
        items : [{
            defaultType : 'textfield',
            title : i18n._( 'Required Information' ),
            items : [{
                xtype : 'label',
                html : i18n._( 'Please provide administrator contact info.' ),
                border : false
            },{
                fieldLabel : i18n._('Email'),
            },{
                fieldLabel : i18n._('Confirm Email'),
            },{
                fieldLabel : i18n._('Company Name'),
            },{
                fieldLabel : i18n._('Name'),
            },{
                fieldLabel : i18n._('Number of machines behind Untangle'),
            }]
        },{
            defaultType : 'textfield',
            title : i18n._( 'Optional Information' ),
            items : [{
                xtype : 'label',
                html : i18n._( 'Answering these questions will help us build a better product - for you!' ),
                border : false
            },{
                fieldLabel : i18n._('How did you find Untangle'),
            },{
                fieldLabel : i18n._('State/Province'),
            },{
                fieldLabel : i18n._('Country'),
            },{
                xtype : 'combo',
                width : 100,
                listWidth : 105,
                store : industryStore,
                fieldLabel : i18n._('What industry is your company in'),
            },{
                xtype : 'combo',
                width : 50,
                store : employeeStore,
                fieldLabel : i18n._('Number of employees'),
            }]
        }]
    });

    return {
        title : i18n._( "Registration" ),
        cardTitle : i18n._( "Registration" ),
        panel : panel
    };
}


Ung.SetupWizard.TimeZoneStore = [];

Ung.Setup =  {
	init: function() {
        rpc = {};

        /* Initialize the timezone data */
        for ( var i = 0; i < Ung.TimeZoneData.length; i++) {
            Ung.SetupWizard.TimeZoneStore.push([Ung.TimeZoneData[i][2], "(" + Ung.TimeZoneData[i][0] + ") " + Ung.TimeZoneData[i][1]]);
        }
        
        rpc.jsonrpc = new JSONRpcClient("/webui/JSON-RPC");
        
        rpc.languageManager = rpc.jsonrpc.RemoteUvmContext.languageManager();

        var result = rpc.languageManager.getTranslations( "main" );
        i18n = new Ung.I18N( { "map": result.map })


        this.wizard = new Ung.Wizard({
            height : 500,
            width : 800,
            cards : [Ung.SetupWizard.welcome(),
                     Ung.SetupWizard.settings(),
                     Ung.SetupWizard.registration(),
            {
                title : "Card 3",
                cardTitle : "Congrats",
                panel : new Ext.Panel({ defaults : { border : false }, items : [{ html : "The final countdown" }] } )
            }],
            el : "container"
        });

        this.wizard.render();

        this.wizard.goToPage( 2 );
	}
};	
