Ext.namespace('Ung');
Ext.namespace('Ung.SetupWizard');

// The location of the blank pixel image
Ext.BLANK_IMAGE_URL = 'ext/resources/images/default/s.gif';
// the main internationalization object
var i18n=null;
// the main json rpc object
var rpc = {};

Ung.SetupWizard.LabelWidth = 150;

Ung.SetupWizard.Welcome = Ext.extend(Object,
{
    constructor : function( config )
    {
        var panel = new Ext.FormPanel({
            items : [{
                xtype : 'label',
                html : i18n._( 'This wizard will guide you through the inital setup and configuration of your Untangle Server. Click <b>Next</b> to get started.' )
                    }]
                });
        
        this.card = {
            title : i18n._( "Welcome" ),
            cardTitle : i18n._( "Thanks for using Untangle" ),
            panel : panel
        };
    }
});

Ung.SetupWizard.Settings = Ext.extend(Object, {
    constructor : function( config )
    {
        var panel = new Ext.FormPanel({
            defaultType : 'fieldset',
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
                    fieldLabel : i18n._('Password'),
                    name : 'password'
                },{
                    inputType : 'password',
                    fieldLabel : i18n._('Confirm Password'),
                    name : 'confirmPassword'
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
                    value : Ung.SetupWizard.CurrentValues.timezone,
                    triggerAction : 'all',
                    listClass : 'x-combo-list-small'
                }]
            },{
                title : 'Hostname',
                items : [{
                    hideLabel : true,
                    xtype : 'textfield',
                    value : Ung.SetupWizard.CurrentValues.hostname,
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

        this.card = {
            title : i18n._( "Settings" ),
            cardTitle : i18n._( "Configure your Server" ),
            panel : panel,
            onNext : this.saveSettings.createDelegate( this )
        };
    },

    saveSettings : function( handler )
    {
        rpc.adminManager.setAdminSettings( Ung.SetupWizard.CurrentValues.users );
        handler();
    }
});


Ung.SetupWizard.Registration = Ext.extend( Object, {
    constructor : function( config )
    {
        this.employeeStore =  [
                              [ 5, i18n._( "1-5" )],
                              [ 10, i18n._( "5-10" )],
                              [ 50, i18n._( "10-50" )],
                              [ 100, i18n._( "50-100" )],
                              [ 250, i18n._( "100-250" )],
                              [ 500, i18n._( "250-500" )],
                              [ 1000, i18n._( "500-1000" )],
                              [ 2500, i18n._( "> 1000 " )]];

        this.industryStore = [
            i18n._( "Banking" ),
            i18n._( "Software" ) ];
        
        this.form = new Ext.FormPanel({
            defaultType : 'fieldset',
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
                    name : 'email'
                },{
                    fieldLabel : i18n._('Confirm Email'),
                    name : 'confirmEmail'
                },{
                    fieldLabel : i18n._('Company Name'),
                    name : 'companyName'
                },{
                    fieldLabel : i18n._('Name'),
                    name : 'name'
                },{
                    xtype : 'numberfield',
                    minValue : 0,
                    allowDecimals : false,
                    fieldLabel : i18n._('Number of machines behind Untangle'),
                    name : 'numSeats'
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
                    name : "findUntangle"
                },{
                    fieldLabel : i18n._('State/Province'),
                    name : "state"
                },{
                    fieldLabel : i18n._('Country'),
                    name : "country"
                },{
                    xtype : 'combo',
                    width : 100,
                    listWidth : 105,
                    store : this.industryStore,
                    mode : 'local',
                    triggerAction : 'all',
                    listClass : 'x-combo-list-small',
                    fieldLabel : i18n._('What industry is your company in'),
                    name : "industry"
                },{
                    xtype : 'combo',
                    width : 50,
                    mode : 'local',
                    triggerAction : 'all',
                    store : this.employeeStore,
                    listClass : 'x-combo-list-small',
                    fieldLabel : i18n._('Number of employees'),
                    name : "numEmployees"
                }]
            }]
        });

        this.card = {
            title : i18n._( "Registration" ),
            cardTitle : i18n._( "Registration" ),
            panel : this.form,
            onNext : this.saveRegistrationInfo.createDelegate( this )
        };
    },

    saveRegistrationInfo : function( handler )
    {
        var info = Ung.SetupWizard.CurrentValues.registrationInfo;
        var misc = {};
        this.setRegistrationValue( "name", misc, true );
        this.setRegistrationValue( "email", info, true, "emailAddr" );
        this.setRegistrationValue( "companyName", info, true );
        this.setRegistrationValue( "numSeats", info, true );

        this.setRegistrationValue( "findUntangle", misc, false );
        this.setRegistrationValue( "state", misc, false );
        this.setRegistrationValue( "country", misc, false );
        this.setRegistrationValue( "industry", misc, false );
        this.setRegistrationValue( "numEmployees", misc, false );

        info.misc.map = misc;

        rpc.adminManager.setRegistrationInfo(this.errorHandler.createDelegate( this, [ handler ], true ), info );
    },

    errorHandler : function( result, exception, foo, handler )
    {
        if(exception) {
            Ext.MessageBox.alert("Failed",exception.message); 
            return;
        }
        
        handler();
    },

    setRegistrationValue : function( fieldName, map, isRequired, param )
    {
        if ( param == null ) param = fieldName;

        var field = this.form.find( "name", fieldName );
        if ( field == null || field.length == 0 ) return;
        field = field[0];
        var value = null;

        /* Combo has to use the raw value, otherwise editable doesn't come through */
        if ( field.xtype == "combo" ) value = field.getRawValue();
        else value = field.getValue();

        if ( value.length == 0 ) return;
        map[param] = "" + value;
    }
});

Ung.SetupWizard.Interfaces = Ext.extend( Object, {
    constructor : function()
    {
        var data = this.fixInterfaceList( Ung.SetupWizard.CurrentValues.interfaceArray );
    
        this.interfaceStore = new Ext.data.Store({
            reader : new Ext.data.ArrayReader({},[{ name : "name" }, { name : "status" }]),
            data : data
        });
    
        var refreshButton = new Ext.Button( {
            text : i18n._( "Refresh" ),
            handler : this.refresh,
            scope : this
        });
    
        this.interfaceGrid = new Ext.grid.GridPanel({
            store : this.interfaceStore,
            loadMask : true,
            stripeRows : true,
            enableColumnResize : false,
            autoResizeColumn : 2,
            disableSelection : false,
            selModel: new Ext.grid.RowSelectionModel({singleSelect:true}),
            enableDragDrop : true,
            ddGroup: 'interfaceDND',
            ddText : '',
            height : 300,
            tbar : [ refreshButton ],
            cm : new Ext.grid.ColumnModel([
            {
               header: i18n._( "Name" ),
               dataIndex: 'name',
               sortable : false,
               fixed : true,
               width: 100
            },{
               header: i18n._( "Status" ),
               dataIndex: 'status',
               sortable : false,
               renderer : function( value ) {
                   return value[0] + ": " + value[1];
               },
               width : 400
            }]),
        });
    
        var panel = new Ext.Panel({
            items : [{
                html : i18n._( 'This step can help you identify your external, internal, and other network cards. Plug an active cable into each network card one at a time and hit refresh to determine which network card is which. You can also drag and drop the interfaces to remap them at this time.' ),
                border : false
    
            }, this.interfaceGrid ]
        });
        
        this.isDragAndDropInitialized = false;
    
        this.card = {
            title : i18n._( "Network Cards" ),
            panel : panel,
            onLoad : function( complete ) {
                if ( this.isDragAndDropInitialized == false ) {
                    this.initializeDragAndDrop();
                }
                this.isDragAndDropInitialized = true;

                complete();
            }.createDelegate( this )
        };
    },
    
    initializeDragAndDrop : function()
    {
        var ddrow = new Ext.dd.DropTarget(this.interfaceGrid.getView().mainBody, {
            ddGroup : 'interfaceDND',                
            copy:false,
            notifyDrop : this.onNotifyDrop.createDelegate( this )
        });
    },
    
    onNotifyDrop : function(dd, e, data)
    {
        var sm = this.interfaceGrid.getSelectionModel();
        var rows=sm.getSelections();
        var cindex=dd.getDragData(e).rowIndex;
        
        if ( typeof cindex == "undefined" ) return false;
        if ( rows.length != 1 ) return false;
        
        var row = this.interfaceStore.getById(rows[0].id);
        var status = row.get( "status" );
    
        var c = 0;
        var data = [];
        var index = -1;
        
        this.interfaceStore.each( function( currentRow ) {
            if ( currentRow == row ) index = c;
            data.push( currentRow.get( "status" ));
            c++;
        });
    
        if ( index == cindex ) return true;
        
        data.splice( index, 1 );
        data.splice( cindex, 0, status );
    
        this.interfaceStore.each( function( currentRow ) {
            currentRow.set( "status", data.shift());
        });
    
        sm.clearSelections();
    
        return true;
    },

    /* Given a list of interfaces, this takes out the ones that are not used */
    fixInterfaceList : function( interfaceArray )
    {
        var cleanArray = [];

        var data = interfaceArray.list;

        for ( var c = 0 ;  c < data.length ; c++ ) {
            var i = data[c];
            /* This is the VPN interfaces, and this is a magic number. */
            if ( i.argonIntf == 7 ) continue;
            
            /* This is an interface that doesn't exist */
            // if ( i.systemName.indexOf( 'nointerface' ) == 0 ) continue;

            cleanArray.push( i );
        }

        /* Now create a new array, in order to handle reordering, it is better
         * to just have two few fields */
        interfaceList = [];
        
        for ( var c = 0 ; c < cleanArray.length ; c++ ) {
            var i = cleanArray[c];
            interfaceList.push( [ i18n._( i.name ), [ i.systemName, i.connectionState, i.currentMedia ]] );
        }

        return interfaceList;
    }
});

Ung.SetupWizard.Internet = Ext.extend( Object, {
    constructor : function( config )
    {
        this.configTypes = 
        [[ "dhcp", i18n._( "Dynamic (DHCP)" ) ],
         [ "static", i18n._( "Static" ) ],
         [ "pppoe", i18n._( "PPPoE" ) ]];
         
        this.cards = [];

        this.cards.push( new Ext.FormPanel({
            border : false,
            labelWidth : Ung.SetupWizard.LabelWidth,
            defaultType : 'textfield',
            items : [{
                xtype : 'button',
                text : i18n._( 'Refresh' ),
                handler : this.refresh,
                scope : this,
                disabled : false
            },{
                xtype : 'fieldset',
                title : i18n._( "Status: not online" ),
                defaultType : 'textfield',
                defaults : {
                    disabled : true
                },
                autoHeight : true,
                items : [{
                    fieldLabel : i18n._( "IP" )
                },{
                    fieldLabel : i18n._( "Netmask" )
                },{
                    fieldLabel : i18n._( "Default Route" )
                },{
                    fieldLabel : i18n._( "Gateway" )
                },{
                    fieldLabel : i18n._( "Primary DNS" )
                },{
                    fieldLabel : i18n._( "Secondary DNS" )
                }]
             }]}));
                

        this.cards.push( new Ext.FormPanel({
            border : false,
            labelWidth : Ung.SetupWizard.LabelWidth,
            defaultType : 'textfield',
            items : [{
                fieldLabel : i18n._( "IP" ),
            },{
                fieldLabel : i18n._( "Netmask" ),
            },{
                fieldLabel : i18n._( "Default Route" ),
            },{
                fieldLabel : i18n._( "Gateway" ),
            },{
                fieldLabel : i18n._( "Primary DNS" ),
            },{
                fieldLabel : i18n._( "Secondary DNS" ),
            }]}));

        /* PPPoE Panel */
        this.cards.push( new Ext.FormPanel({
            border : false,
            labelWidth : Ung.SetupWizard.LabelWidth,
            defaultType : 'textfield',
            items : [{
                fieldLabel : i18n._( "Username" ),
                disabled : false,
            },{
                inputType : 'password',
                fieldLabel : i18n._( "Password" ),
                disabled : false,
            },{
                xtype : 'button',
                text : i18n._( 'Refresh' ),
                handler : this.refresh,
                scope : this,
                disabled : false
            },{
                xtype : 'fieldset',
                title : i18n._( "Status: not online" ),
                defaultType : 'textfield',
                defaults : {
                    disabled : true
                },
                autoHeight : true,
                items : [{
                    fieldLabel : i18n._( "IP" )
                },{
                    fieldLabel : i18n._( "Netmask" )
                },{
                    fieldLabel : i18n._( "Default Route" )
                },{
                    fieldLabel : i18n._( "Gateway" )
                },{
                    fieldLabel : i18n._( "Primary DNS" )
                },{
                    fieldLabel : i18n._( "Secondary DNS" )
                }]
             }]}));
    
        this.cardPanel = new Ext.Panel({
            cls : 'untangle-form-panel',
            border : false,
            layout : 'card',
            items : this.cards,
            autoHeight : true,
            activePanel : 0,
            defaults : { 
                autoHeight : true,
                border : false,
            }
        });

        var configure = new Ext.FormPanel({
            cls : 'untangle-form-panel',
            border : false,
            autoHeight : true,
            labelWidth : Ung.SetupWizard.LabelWidth,
            items : [{
                xtype : 'label',
                html : i18n._( 'Configure your External Interface' ),
                border : false
            },{
                xtype : 'combo',
                fieldLabel : i18n._('Configuration Type'),
                name : 'configType',
                editable : false,
                store : this.configTypes,
                mode : 'local',
                listeners : {
                    "select" : {
                        fn : this.onSelectConfig,
                        scope : this
                    }
                },
                width : 100,
                listWidth : 105,
                value : this.configTypes[0][0],
                triggerAction : 'all',
                listClass : 'x-combo-list-small'
            }]
        });

        var panel = new Ext.Panel({
            cls : null,
            defaults : {
                cls : null
            },
            items : [ configure, this.cardPanel, {
                xtype : 'button',
                text : i18n._( 'Test Connectivity' ),
                handler : this.refresh,
                scope : this,
                disabled : false
            }]
        });
        
        this.card = {
            title : i18n._( "Internet Connection" ),
            panel : panel,
            onLoad : function( complete )
            {
                this.cardPanel.layout.setActiveItem( 0 );

                complete();
            }.createDelegate(this)
        }
    },
    
    onSelectConfig : function( combo, record, index )
    {
        this.cardPanel.layout.setActiveItem( index );
    }

});

Ung.SetupWizard.InternalNetwork = Ext.extend( Object, {
    constructor : function( config )
    {
        var panel = new Ext.FormPanel({
            defaultType : 'fieldset',
            defaults : {
                autoHeight : true
            },
            items : [{
                title : i18n._( 'Transparent Bridge' ),
                items : [{
                    xtype : 'radio',
                    name : 'bridgeInterfaces',
                    boxLabel : i18n._( 'This is recommended if the external interface is connected to a router or firewall.' ),
                    hideLabel : 'true'
                },{
                    xtype : 'label',
                    html : '<img src="/webui/skins/' + Ung.SetupWizard.currentSkin + '/images/main/wizard_bridge.png"/>'
                }]
            },{
                title : i18n._( 'Router' ),
                items : [{
                    xtype : 'radio',
                    name : 'bridgeInterfaces',
                    boxLabel : i18n._( 'This is recommended if the external port is plugged into your internet connection.' ),
                    hideLabel : 'true'
                },{
                    xtype : 'label',
                    html : '<img src="/webui/skins/' + Ung.SetupWizard.currentSkin + '/images/main/wizard_router.png"/>'
                }]
           }]
        });
        
        this.card = {
            title : i18n._( "Internal Network" ),
            panel : panel
        }
    }
});

Ung.SetupWizard.Email = Ext.extend( Object, {
    constructor : function( config )
    {
        var panel = new Ext.FormPanel({
            defaultType : 'textfield',
            defaults : {
                autoHeight : true
            },
            items : [{
                xtype : 'label',
                html : i18n._( "Your Untangle Server sends email for Quarantine Digests, Reports, etc." ),
            },{
                xtype : 'button',
                text : i18n._( 'Email Test' ),
                handler : this.emailTest,
                scope : this,
            },{
                xtype : 'checkbox',
                hideLabel : true,
                value : false,
                boxLabel : i18n._("Advanced Email Configuration."),
                listeners : {
                    check : {
                        fn : function( checkbox, enabled ) {
                            this.onSetMode( enabled );
                        },
                        scope : this
                    }
                }
            },{
                name : 'email-from-address',
                xtype : "fieldset",
                items : [{
                    name : 'email-from-address-label',
                    xtype : 'label',
                    html : i18n._( "Please choose a <i>From Address</i> for emails originating from the Untangle Server" )
                },{
                    xtype : 'textfield',
                    name : 'email-from-address-textfield',
                    fieldLabel : i18n._("From Address")
                }]
            },{
                name : 'email-smtp-server-config',
                xtype : "fieldset",
                defaultType : 'textfield',
                items : [{
                    xtype : 'radio',
                    name : 'email-smtp-send-directly',
                    checked : true,
                    inputValue : "directly",
                    boxLabel : i18n._( 'Send Email Directly (default).' ),
                    hideLabel : 'true',
                    listeners : {
                        check : {
                            fn : function( checkbox, checked ) {
                                if ( checked ) this.onSetSendDirectly( true );
                            },
                            scope : this
                        }
                    }
                },{
                    xtype : 'radio',
                    name : 'email-smtp-send-directly',
                    inputValue : "smtp-server",
                    boxLabel : i18n._( 'Send Email using the specified SMTP server.' ),
                    hideLabel : 'true',
                    listeners : {
                        check : {
                            fn : function( checkbox, checked ) {
                                if ( checked ) this.onSetSendDirectly( false );
                            },
                            scope : this
                        }
                    }
                },{
                    name : 'email-smtp-server-addr',
                    fieldLabel : i18n._( "SMTP Server" )
                },{
                    name : 'email-smtp-server-port',
                    xtype : 'numberfield',
                    minValue : 0,
                    maxValue : 65536,
                    allowDecimals : false,
                    fieldLabel : i18n._( 'Port' )
                },{
                    name : 'email-smtp-server-requires-auth',
                    xtype : 'checkbox',
                    hideLabel : true,
                    value : false,
                    boxLabel : i18n._("Server Requires Authentication."),
                    listeners : {
                        check : {
                            fn : function( checkbox, checked ) {
                                this.onSetRequiresAuth( checked );
                            },
                            scope : this
                        }
                    }
                },{
                    name : 'email-smtp-server-username',
                    fieldLabel : i18n._( "Username" )
                },{
                    name : 'email-smtp-server-password',
                    inputType : 'password',
                    fieldLabel : i18n._( "Password" )
                }]
            }]
        });

        var field = null;
        this.advancedArray = [];
        this.authArray = [];
        this.directlyArray = [];

        this.advancedArray.push( panel.find( "name", "email-from-address" )[0]);
        this.advancedArray.push( panel.find( "name", "email-smtp-server-config" )[0]);

        this.directlyArray.push( panel.find( "name", "email-smtp-server-addr" )[0]);
        this.directlyArray.push( panel.find( "name", "email-smtp-server-port" )[0]);
        field = panel.find( "name", "email-smtp-server-requires-auth" )[0];
        this.requiresAuth = field;
        this.directlyArray.push( field );

        field = panel.find( "name", "email-smtp-server-username" )[0];
        this.directlyArray.push( field );
        this.authArray.push( field );
        
        field = panel.find( "name", "email-smtp-server-password" )[0];
        this.directlyArray.push( field );
        this.authArray.push( field );

        this.onSetMode( false );
        this.onSetSendDirectly( true );

        this.card = {
            title : i18n._( "Email" ),
            cardTitle : i18n._( "Email Configuration" ),
            panel : panel
        }
    },
    
    onSetMode : function( isAdvanced )
    {
        var length = this.advancedArray.length;
        for ( var c = 0 ; c < length ; c++ ) {
            this.advancedArray[c].setDisabled( !isAdvanced );
        }
    },

    onSetSendDirectly : function( isSendDirectly )
    {
        var length = this.directlyArray.length;
        for ( var c = 0 ; c < length ; c++ ) {
            this.directlyArray[c].setDisabled( isSendDirectly );
        }

        if ( !isSendDirectly ) this.onSetRequiresAuth( this.requiresAuth.getValue());
    },

    onSetRequiresAuth : function( requiresAuth )
    {
        var length = this.authArray.length;
        for ( var c = 0 ; c < length ; c++ ) {
            this.authArray[c].setDisabled( !requiresAuth );
        }
    }    
});

Ung.SetupWizard.Complete = Ext.extend( Object, {
    constructor : function( config )
    {
        var panel = new Ext.FormPanel({
        items : [{
            xtype : 'label',
            html : i18n._( 'Your Untangle Server is now configured.!<br/>You are now ready to login and download some applications.' )
        }]
    });

        this.card = {
            title : i18n._( "Finished" ),
            cardTitle : i18n._( "Congratulations!" ),
            panel : panel
        }
    }
});

Ung.SetupWizard.PasswordUtil = Ext.extend( Object, {
    /* Encode the password and return it as an array of bytes / ints */
    encodePassword : function( password )
    {
        
    }
    
});

Ung.SetupWizard.TimeZoneStore = [];

Ung.Setup =  {
    init: function() {
        rpc = {};

        /* Initialize the timezone data */
        for ( var i = 0; i < Ung.TimeZoneData.length; i++) {
            Ung.SetupWizard.TimeZoneStore.push([Ung.TimeZoneData[i][0], "(" + Ung.TimeZoneData[i][0] + ") " + Ung.TimeZoneData[i][1]]);
        }
        
        rpc.jsonrpc = new JSONRpcClient("/webui/JSON-RPC");
        
        rpc.languageManager = rpc.jsonrpc.RemoteUvmContext.languageManager();
        rpc.adminManager = rpc.jsonrpc.RemoteUvmContext.adminManager();

        var result = rpc.languageManager.getTranslations( "main" );
        i18n = new Ung.I18N( { "map": result.map })
        
        var welcome = new Ung.SetupWizard.Welcome();
        var settings = new Ung.SetupWizard.Settings();
        var registration = new Ung.SetupWizard.Registration();
        var interfaces = new Ung.SetupWizard.Interfaces();
        var internet = new Ung.SetupWizard.Internet();
        var internal = new Ung.SetupWizard.InternalNetwork();
        var email = new Ung.SetupWizard.Email();
        var complete = new Ung.SetupWizard.Complete();

        this.wizard = new Ung.Wizard({
            height : 500,
            width : 800,
            cardDefaults : {
                labelWidth : Ung.SetupWizard.LabelWidth,
                cls : 'untangle-form-panel'
            },
            cards : [welcome.card,
                     settings.card,
                     registration.card,
                     interfaces.card,
                     internet.card,
                     internal.card,
                     email.card,
                     complete.card
            ],
            el : "container"
        });

        this.wizard.render();

        this.wizard.goToPage( 2 );
	}
};
