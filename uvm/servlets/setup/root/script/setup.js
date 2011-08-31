Ext.namespace('Ung');
Ext.namespace('Ung.SetupWizard');

Ung.SetupWizard.BOGUS_ADDRESS = "192.0.2.1";

// The location of the blank pixel image
Ext.BLANK_IMAGE_URL = '/ext/resources/images/default/s.gif';
// the main internationalization object
var i18n=null;
// the main json rpc object
var rpc = {};

var oemName = "Untangle";

Ung.SetupWizard.LabelWidth = 200;
Ung.SetupWizard.LabelWidth2 = 214;
Ung.SetupWizard.LabelWidth3 = 120;
Ung.SetupWizard.LabelWidth4 = 100;

Ung.SetupWizard.TextField = Ext.extend( Ext.form.TextField, {
    onRender : function(ct, position)
    {
        Ung.SetupWizard.TextField.superclass.onRender.call(this, ct, position);

        var parent = this.el.parent();

        if( this.boxLabel ) {
            this.labelEl = parent.createChild({
                tag: 'label',
                htmlFor: this.el.id,
                cls: 'x-form-textfield-detail',
                html: this.boxLabel
            });
        }
    }
});

Ung.SetupWizard.NumberField = Ext.extend( Ext.form.NumberField, {
    onRender : function(ct, position)
    {
        Ung.SetupWizard.NumberField.superclass.onRender.call(this, ct, position);

        var parent = this.el.parent();

        if( this.boxLabel ) {
            this.labelEl = parent.createChild({
                tag: 'label',
                htmlFor: this.el.id,
                cls: 'x-form-textfield-detail',
                html: this.boxLabel
            });
        }
    }
});

Ung.SetupWizard.EmailTester = Ext.extend( Object, {
    constructor : function( config )
    {
        if ( config == null ) {
            config = {};
        }

        this.emailTestMessage =
            i18n._( "Enter an email address to send a test email and then press ") + "<i>" + i18n._("Send Test Email") + "</i>." + "<br/>" +
            i18n._( "The specified email address should receive an email within a few minutes. If not, the current email configuration may not be correct." );

        this.emailAddress = config.emailAddress;
    },

    testMessageHandler : function( result, exception )
    {
        var message = i18n._( 'Email sent. ') + i18n._('Check ') + this.emailAddress + i18n._(' mailbox for successful delivery.' );
        if ( exception ) {
            message  = exception.message;
            if (message == null || message == "Unknown") {
                message = i18n._("Please Try Again");
            }
            Ext.MessageBox.alert( i18n._( "Failed" ), message );
            return;
        }

        if ( result != true ) {
            message = i18n._( 'Warning!  Test failed.  Check the settings.' );
        }

        this.showTester( { progressText : message } );
    },

    buttonHandler : function( button, emailAddress )
    {
        this.emailAddress = emailAddress;

        if ( button == 'ok' ) {
            if (this.emailAddress == '') {
                this.showTester( { progressText : i18n._( 'Enter a valid email address.' ) } );
            }
            else {
                this.showTester( { progressText : i18n._( 'Sending...' ) } );
                rpc.adminManager.sendTestMessage( this.testMessageHandler.createDelegate( this ), this.emailAddress );
            }
        }
    },

    showTester : function( config )
    {
        if ( config == null ) {
            config = {};
        }

        Ext.MessageBox.show({
            title : i18n._('Email Test'),
            buttons : {
                cancel : i18n._('Close'),
                ok : i18n._('Send Test Email')
            },
            width : 600,
            msg : this.emailTestMessage,
            modal : true,
            prompt : true,
            progress : (config.progressText != null),
            progressText : config.progressText,
            value : this.emailAddress,
            multiline: 20,
            fn : this.buttonHandler.createDelegate( this )
        });
    }
});

Ext.apply( Ext.form.VTypes, {
    ipCheck : function( val, field )
    {
        return val.match( this.ipCheckRegex );
    },
    ipCheckText : 'Please enter a valid IP Address',
    ipCheckRegex : /\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\b/,
    emailAddressCheck : function( val, field )
    {
        return val.match( this.emailAddressCheckRegex );
    },

    emailAddressCheckRegex : new RegExp( "[a-zA-Z0-9!#$%&'*+\/=?^_`{|}~-]+(?:\.[a-zA-Z0-9!#$%&'*+\/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?"),

    emailAddressCheckText : 'Please enter a valid email address',

    emailAddressMatchCheck : function( val, field )
    {
        var email_original = Ext.getCmp(field.compareEmailField);
        return val == email_original.getValue();
    },

    emailAddressMatchCheckText : 'Email addresses do not match',

    passwordConfirmCheck : function(val,field){
            var pass_original = Ext.getCmp(field.comparePasswordField);
        return val == pass_original.getValue();
    },

    passwordConfirmCheckText : 'Passwords do not match',

    hostname : function( val, field )
    {
        var labels = val.split( "." );
        for ( var c = 0 ; c < labels.length ; c++ ) {
            if ( !labels[c].match( this.hostnameRegex )) {
                return false;
            }
        }

        return true;
    },

    hostnameRegex : /^[0-9A-Za-z]([-/_0-9A-Za-z]*[0-9A-Za-z])?$/,

    hostnameText : "Please enter a valid hostname"
});

Ung.SetupWizard.Welcome = Ext.extend(Object, {
    constructor : function( config )
    {
        var panel = new Ext.FormPanel({
            items : [{
                xtype : 'label',
                html : '<h2 class="wizard-title">' + String.format(i18n._( "Thanks for choosing {0}!" ),oemName) + '</h2>'
            },{
                xtype : 'label',
                cls : 'noborder',
                html : String.format(i18n._( 'This wizard will guide you through the initial setup and configuration of the {0} Server.'),oemName) +
                    '<br/><br/>'+
                    String.format(i18n._('Click {0}Next{1} to get started.'),'<b>','</b>')
            }]
        });
        
        this.card = {
            title : i18n._( "Welcome" ),
            panel : panel
        };
    }
});

Ung.SetupWizard.Settings = Ext.extend(Object, {
    constructor : function( config )
    {
        this.panel = new Ext.FormPanel({
            defaultType : 'fieldset',
            defaults : {
                autoHeight : true,
                cls : 'noborder'
            },
            items : [{
                xtype : 'label',
                html : '<h2 class="wizard-title">'+i18n._( "Configure the Server" )+'</h2>'
            },{
                defaultType : 'textfield',
                defaults : {
                    msgTarget : 'side',
                    validationEvent : 'blur'
                },
                items : [{
                    xtype : 'label',
                    html : i18n._( '<b>Choose a password for the admin account.</b>' ),
                    border : false
                },{
                    inputType : 'text',
                    fieldLabel : i18n._('Login'),
                    name : 'login',
                    value : 'admin',
                    readOnly : true,
                    fieldClass : 'noborder',
                    itemCls : 'small-top-margin'
                },{
                    inputType : 'password',
                    fieldLabel : i18n._('Password'),
                    name : 'password',
                    id : 'settings_password',
                    allowBlank : false,
                    minLength : 3,
                    minLengthText : i18n.sprintf(i18n._("The password is shorter than the minimum %d characters."), 3)
                },{
                    inputType : 'password',
                    fieldLabel : i18n._('Confirm Password'),
                    name : 'confirmPassword',
                    allowBlank : false,
                    comparePasswordField : 'settings_password',
                    vtype : 'passwordConfirmCheck'
                }]
            },{
                items : [{
                    xtype : 'label',
                    html : i18n._( '<b>Select a timezone.</b>' ),
                    border : false
                },{
                    xtype : 'combo',
                    name : 'timezone',
                    editable : false,
                    store : Ung.SetupWizard.TimeZoneStore,
                    width : 350,
                    listWidth : 355,
                    hideLabel : true,
                    mode : 'local',
                    value : Ung.SetupWizard.CurrentValues.timezone,
                    triggerAction : 'all',
                    listClass : 'x-combo-list-small',
                    ctCls : 'small-top-margin'
                }]
            }]
        });

        this.card = {
            title : i18n._( "Settings" ),
            //cardTitle : i18n._( "Configure the Server" ),
            panel : this.panel,
            onNext : this.saveSettings.createDelegate( this ),
            onValidate : this.validateSettings.createDelegate(this)
        };
    },
    validateSettings : function()
    {
        var rv = _validate(this.panel.items.items);
        return rv;
    },
    saveSettings : function( handler )
    {
        Ext.MessageBox.wait( i18n._( "Saving Settings" ), i18n._( "Please wait" ));
        var saver = new Ung.SetupWizard.SettingsSaver( this.panel, handler );
        saver.savePassword();
    }
});

Ung.SetupWizard.SettingsSaver = Ext.extend( Object, {
    password : null,

    constructor : function( panel, handler )
    {
        this.panel = panel;
        this.handler = handler;
    },

    savePassword : function()
    {
        /* New Password */
        this.password = this.panel.find( "name", "password" )[0].getValue();
        rpc.setup.setAdminPassword( this.saveTimeZone.createDelegate( this ), this.password );
    },

    saveTimeZone : function( result, exception )
    {
        if( exception ) {
            Ext.MessageBox.alert(i18n._( "Unable to save the admin password" ), exception.message );
            return;
            }

        var timezone = this.panel.find( "name", "timezone" )[0].getValue();

        rpc.setup.setTimeZone( this.authenticate.createDelegate( this ), timezone );
    },

    authenticate : function( result, exception )
    {
        if ( exception ) {
            Ext.MessageBox.alert("Unable to save Time zone settings",exception.message);
            return;
        }

        /* Cache the password to reauthenticate later */
        Ung.SetupWizard.ReauthenticateHandler.password = this.password;

        Ext.Ajax.request({
            params : {
                username : 'admin',
                password : this.password
            },
            /* If it uses the default type then this will not work
             * because the authentication handler does not like utf8 */
            headers : {
                'Content-Type' : "application/x-www-form-urlencoded"
            },
            url : '/auth/login?url=/webui/setupSettings.js&realm=Administrator',
            callback : this.getManagers.createDelegate( this )
        });
    },

    getManagers : function( options, success, response )
    {
        if ( success ) {
            eval( response.responseText );
            /* It is very wrong to do this all synchronously */
            rpc.jsonrpc = new JSONRpcClient( "/webui/JSON-RPC" );
            rpc.adminManager = rpc.jsonrpc.RemoteUvmContext.adminManager();
            rpc.networkManager = rpc.jsonrpc.RemoteUvmContext.networkManager();
            rpc.connectivityTester = rpc.jsonrpc.RemoteUvmContext.getRemoteConnectivityTester();
            rpc.toolboxManager = rpc.jsonrpc.RemoteUvmContext.toolboxManager();
            rpc.mailSender = rpc.jsonrpc.RemoteUvmContext.mailSender();

            Ext.MessageBox.hide();
            this.handler();
        } else {
            Ext.MessageBox.alert( i18n._( "Unable to save password." ));
        }
    }
});

Ung.SetupWizard.Interfaces = Ext.extend( Object, {
    constructor : function()
    {
        this.interfaceStore = new Ext.data.Store({
            reader : new Ext.data.ArrayReader({},[{ name : "name" }, { name : "status" }]),
            data : []
        });

        var refreshButton = new Ext.Button( {
            text : i18n._( "Refresh" ),
            handler : this.refreshInterfaces.createDelegate( this ),
            iconCls : 'icon-autorefresh'
        });

        this.interfaceGrid = new Ext.grid.GridPanel({
            store : this.interfaceStore,
            loadMask : true,
            stripeRows : true,
            baseCls : 'small-top-margin',
            enableColumnResize : false,
            autoResizeColumn : 2,
            disableSelection : false,
            selModel : new Ext.grid.RowSelectionModel({singleSelect : true}),
            enableDragDrop : true,
            ddGroup : 'interfaceDND',
            ddText : '',
            height : 300,
            width : 555,
            tbar : [ refreshButton ],
            viewConfig : {
                forceFit : true
            },
            cm : new Ext.grid.ColumnModel([{
                header : i18n._( "Name" ),
                dataIndex : 'name',
                sortable : false,
                fixed : true,
                width : 100,
                renderer : function( value ) {
                    return i18n._( value );
                }
            },{
                header : i18n._( "Status" ),
                dataIndex : 'status',
                sortable : false,
                renderer : function( value ) {
                    var divClass = "draggable-disabled-interface";
                    var status = i18n._( "unknown" );

                    if (value[3] == "Unknown") {
                        value[3] = i18n._("Unknown Vendor");
                    }
                    
                    if ( value[1] == "connected" ) {
                        status = i18n._( "connected" ) + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + " [" + value[0] + " / " + value[2] + " / " + value[3] + "]";
                        divClass = "draggable-enabled-interface";
                    }
                    if ( value[1] == "disconnected" ) {
                        status = i18n._( "disconnected" ) + "&nbsp;&nbsp;" + " [" + value[0] + " / " + value[2] + " / " + value[3] + "]";
                        divClass = "draggable-disabled-interface";
                    }
                    
                    return "<div class='" + divClass + "'>" + status + "</div>";
                },
                width : 460
            }])
        });

        
        var panelTitle = i18n._('Identify Network Cards');
        var panelText = "<font color=\"red\"><b>" + i18n._( "Important:") + "</b></font>";
        panelText += i18n._( " This step identifies the external, internal, and other network cards. ");
        panelText += "<br/>";
        panelText += "<br/>";

        panelText += "<b>" + i18n._("Step 1: ") + "</b>";
        panelText += i18n._( "Plug an active cable into one network card and hit <i>Refresh</i> to determine which network card it is.");
        panelText += "<br/>";
        
        panelText += "<b>" + i18n._("Step 2: ") + "</b>";
        panelText += "<b>" + i18n._( "Drag and drop") + "</b>" + i18n._(" the network card to map it to the desired interface.");
        panelText += "<br/>";

        panelText += "<b>" + i18n._("Step 3: ") + "</b>";
        panelText += i18n._( "Repeat steps 1 and 2 for each network card and then click <i>Next</i>.");
        panelText += "<br/>";
        
        var panel = new Ext.Panel({
            defaults : { cls : 'noborder' },
            items : [{
                html : '<h2 class=" wizard-title">'+panelTitle+'<h2>',
                border : false
            },{
                xtype : 'label',
                html : panelText,
                border : false
            }, this.interfaceGrid ]
        });

        this.isDragAndDropInitialized = false;

        this.card = {
            title : i18n._( "Network Cards" ),
            panel : panel,
            onLoad : function( complete ) {

                this.refreshInterfaces();
                
                if ( this.isDragAndDropInitialized == false ) {
                    this.initializeDragAndDrop();
                }
                this.isDragAndDropInitialized = true;

                complete();
            }.createDelegate( this ),

            onNext : this.saveInterfaceList.createDelegate( this )
        };
    },

    initializeDragAndDrop : function()
    {
        var data = this.fixInterfaceList( Ung.SetupWizard.CurrentValues.interfaceArray );

        this.interfaceStore.loadData( data );

        var ddrow = new Ext.dd.DropTarget(this.interfaceGrid.getView().mainBody, {
            ddGroup : 'interfaceDND',
            copy : false,
            notifyDrop : this.onNotifyDrop.createDelegate( this )
        });
    },

    onNotifyDrop : function(dd, e, data)
    {
        var sm = this.interfaceGrid.getSelectionModel();
        var rows=sm.getSelections();
        var cindex=dd.getDragData(e).rowIndex;

        if ( typeof cindex == "undefined" ) {
            return false;
        }
        if ( rows.length != 1 ) {
            return false;
        }

        var row = this.interfaceStore.getById(rows[0].id);
        var status = row.get( "status" );

            var c = 0;
        var rowData = [];
        var index = -1;

        this.interfaceStore.each( function( currentRow ) {
            if ( currentRow == row ) {
                index = c;
            }
            rowData.push( currentRow.get( "status" ));
            c++;
        });

        if ( index == cindex ) {
            return true;
        }

        rowData.splice( index, 1 );
        rowData.splice( cindex, 0, status );

        this.interfaceStore.each( function( currentRow ) {
            currentRow.set( "status", rowData.shift());
        });

        sm.clearSelections();

        return true;
    },

    /* Given a list of interfaces, this takes out the ones that are not used */
    fixInterfaceList : function( interfaceArray )
    {
        var cleanArray = [];

        var data = interfaceArray.list;
        var c, i;

        for ( c = 0 ;  c < data.length ; c++ ) {
            i = data[c];
            /* This is the VPN interfaces, and this is a magic number. */
            if ( i.systemName.indexOf( 'tun0' ) == 0 ) continue;

            /* This is an interface that does not exist */
            if ( i.systemName.indexOf( 'nointerface' ) == 0 ) continue;

            cleanArray.push( i );
        }

        /* Now create a new array, in order to handle reordering, it is better
         * to just have two few fields */
        interfaceList = [];

        for ( c = 0 ; c < cleanArray.length ; c++ ) {
            i = cleanArray[c];
            if (i.vendor == null) 
                i.vendor = "Unknown";
            if (i.macAddress == null) 
                i.macAddress = "";
            interfaceList.push( [ i.name, [ i.systemName, i.connectionState, i.macAddress, i.vendor ]] );
        }

        return interfaceList;
    },

    saveInterfaceList : function( handler )
    {
        Ext.MessageBox.wait( i18n._( "Remapping Network Interfaces" ), i18n._( "Please wait" ));

        Ung.SetupWizard.ReauthenticateHandler.reauthenticate( this.afterReauthenticate.createDelegate( this, [ handler ] ));

        /* do this before the next step */
        rpc.setup.refreshNetworkConfig();
    },

    afterReauthenticate : function( handler )
    {
        /* Commit the store to get rid of the change marks */
        this.interfaceStore.commitChanges();

        /* Build the two interface arrays */
        var osArray = [];
        var userArray = [];
        this.interfaceStore.each( function( currentRow ) {
            var status = currentRow.get( "status" );
            userArray.push( currentRow.get( "name" ));
            osArray.push( status[0] );
        });

        
        rpc.networkManager.remapInterfaces( this.errorHandler.createDelegate( this, [ handler ], true ), osArray, userArray );
    },

    errorHandler : function( result, exception, foo, handler )
    {
        if(exception) {
            Ext.MessageBox.alert(i18n._( "Unable to remap the interfaces." ), exception.message );
            return;
        }

        Ext.MessageBox.hide();
        handler();
    },

    refreshInterfaces : function()
    {
        Ext.MessageBox.wait( i18n._( "Refreshing Network Interfaces" ), i18n._( "Please wait" ));
        Ung.SetupWizard.ReauthenticateHandler.reauthenticate( this.arRefreshInterfaces.createDelegate( this ));
    },

    arRefreshInterfaces : function()
    {
        rpc.networkManager.updateLinkStatus();
        rpc.networkManager.getNetworkConfiguration( this.completeRefreshInterfaces.createDelegate( this ) );
    },

    completeRefreshInterfaces : function( result, exception )
    {
        if ( exception ) {
            Ext.MessageBox.show({
                title:i18n._( "Unable to refresh the interfaces." ),
                msg:exception.message,
                width:300,
                buttons:Ext.MessageBox.OK,
                icon:Ext.MessageBox.INFO
            });
            return;
        }

        var interfaceList = this.fixInterfaceList( result.interfaceList );

        if ( interfaceList.length != this.interfaceStore.getCount()) {
            Ext.MessageBox.alert( i18n._( "New interfaces" ), i18n._ ( "There are new interfaces, please restart the wizard." ), "" );
            return;
        }

        if ( interfaceList.length < 2) {
            Ext.MessageBox.alert( i18n._( "Missing interfaces" ), i18n._ ( "Untangle requires two or more network cards. Please reinstall with at least two network cards." ), "" );
            return;
            }
        
        var statusHash = {};
        /* XXX This status array is brittle and should be refactored. XXXX */
        for ( var c = 0 ;c < interfaceList.length ; c++ ) {
            var status = interfaceList[c][1];
            statusHash[status[0]] = status;
        }

        /* This is designed to handle the case where the interfaces have been remapped. */
        this.interfaceStore.each( function( currentRow ) {
            var status = currentRow.get( "status" );
            currentRow.set( "status", statusHash[status[0]]);
        });

        Ext.MessageBox.hide();
    }
});

Ung.SetupWizard.Internet = Ext.extend( Object, {
    constructor : function( config )
    {
        this.configTypes = [];
        this.configTypes.push( [ "dynamic", i18n._( "Dynamic (DHCP)" ) ] );
        this.configTypes.push( [ "static", i18n._( "Static" ) ] );
        this.configTypes.push( [ "pppoe", i18n._( "PPPoE" ) ] );

        this.cards = [];

        this.cards.push( this.dhcpPanel = new Ext.FormPanel({
            saveData : this.saveDHCP.createDelegate( this ),
            border : false,
            cls : 'network-card-form-margin',
            labelWidth : Ung.SetupWizard.LabelWidth,
            defaultType : 'textfield',
            items : [{
                xtype : 'fieldset',
                title : i18n._( "DHCP Status" ),
                defaultType : 'textfield',
                defaults : {
                    readOnly : true,
                    fieldClass : 'noborder'
                },
                autoHeight : true,
                items : [{
                    name : "ip",
                    fieldLabel : i18n._( "IP Address" )
                },{
                    name : "netmask",
                    fieldLabel : i18n._( "Netmask" )
                },{
                    name : "gateway",
                    fieldLabel : i18n._( "Gateway" )
                },{
                    name : "dns1",
                    fieldLabel : i18n._( "Primary DNS" )
                },{
                    name : "dns2",
                    fieldLabel : i18n._( "Secondary DNS" )
                },{
                    xtype : 'button',
                    text : i18n._( 'Refresh' ),
                    handler : this.refresh.createDelegate( this ),
                    disabled : false,
                    cls : 'right-align'
                },{
                    xtype : 'button',
                    text : i18n._( 'Test Connectivity' ),
                    cls : 'test-connectivity',
                    handler : this.testConnectivity.createDelegate( this ),
                    disabled : false
                }]
            }]}));


        this.cards.push( this.staticPanel = new Ext.FormPanel({
            saveData : this.saveStatic.createDelegate( this ),
            border : false,
            cls : 'network-card-form-margin',
            labelWidth : Ung.SetupWizard.LabelWidth,
            defaultType : 'textfield',
            items : [{
                xtype : 'fieldset',
                title : i18n._( "Static Settings" ),
                defaultType : 'textfield',
                defaults : {
                    disabled : false,
                    msgTarget : 'side',
                    validationEvent : 'blur',
                    maskRe : /(\d+|\.)/,
                    vtype : 'ipCheck'
                },
                autoHeight : true,
                items : [{
                    name : "ip",
                    fieldLabel : i18n._( "IP Address" ),
                    allowBlank : false
                },{
                    name : "netmask",
                    fieldLabel : i18n._( "Netmask" ),
                    xtype : 'combo',
                    store : Ung.SetupWizard.NetmaskData,
                    mode : 'local',
                    triggerAction : 'all',
                    width : 120,
                    listWidth : 125,
                    value : "255.255.255.0",
                    editable : false
                },{
                    name : "gateway",
                    fieldLabel : i18n._( "Gateway" ),
                    allowBlank : false
                },{
                    name : "dns1",
                    fieldLabel : i18n._( "Primary DNS" ),
                    allowBlank : false
                },{
                    name : "dns2",
                    fieldLabel : i18n._( "Secondary DNS (optional)"),
                    allowBlank : true
                },{
                    xtype : 'button',
                    text : i18n._( 'Test Connectivity' ),
                    cls : 'test-connectivity-2',
                    handler : this.testConnectivity.createDelegate( this ),
                    disabled : false
                }]
            }]}));

        /* PPPoE Panel */
        this.pppoePanel = new Ext.FormPanel({
            saveData : this.savePPPoE.createDelegate( this ),
            border : false,
            cls : 'network-card-form-margin',
            labelWidth : Ung.SetupWizard.LabelWidth2,
            defaultType : 'textfield',
            items : [{
                xtype : 'label',
                cls : 'noborder',
                border : false,
                html : String.format( i18n._( 'Using PPPoE on {0} is <b>NOT</b> recommended.' ), oemName) + "<br/>" +
                    i18n._("It is recommended to use the ISP-supplied modem in bridge mode to handle PPPoE.") + "<br/>" +
                    "&nbsp;<br/>"
            }, {
                xtype : 'fieldset',
                title : i18n._( "PPPoE Settings" ),
                defaultType : 'textfield',
                autoHeight : true,
                items : [{
                    fieldLabel : i18n._( "Username" ),
                    name : "username",
                    disabled : false,
                    readOnly : false,
                    fieldClass : '',
                    labelStyle : 'width : '+Ung.SetupWizard.LabelWidth+'px'
                },{
                    name : "password",
                    inputType : 'password',
                    fieldLabel : i18n._( "Password" ),
                    disabled : false,
                    readOnly : false,
                    fieldClass : '',
                    labelStyle : 'width : '+Ung.SetupWizard.LabelWidth+'px'
                }]
            }, {
                xtype : 'fieldset',
                title : i18n._( "PPPoE Status" ),
                defaultType : 'textfield',
                defaults : {
                    readOnly : true
                },
                autoHeight : true,
                items : [{
                    fieldLabel : i18n._( "IP Address" ),
                    labelStyle : 'width : '+Ung.SetupWizard.LabelWidth+'px',
                    name : "ip",
                    fieldClass : 'noborder'
                },{
                    fieldLabel : i18n._( "Netmask" ),
                    name : "netmask",
                    labelStyle : 'width : '+Ung.SetupWizard.LabelWidth+'px',
                    fieldClass : 'noborder'
                },{
                    name : "gateway",
                    fieldLabel : i18n._( "Gateway" ),
                    labelStyle : 'width : '+Ung.SetupWizard.LabelWidth+'px',
                    fieldClass : 'noborder'
                },{
                    name : "dns1",
                    fieldLabel : i18n._( "Primary DNS" ),
                    labelStyle : 'width : '+Ung.SetupWizard.LabelWidth+'px',
                    fieldClass : 'noborder'
                },{
                    name : "dns2",
                    fieldLabel : i18n._( "Secondary DNS" ),
                    labelStyle : 'width : '+Ung.SetupWizard.LabelWidth+'px',
                    fieldClass : 'noborder'
                },{
                    xtype : 'button',
                    text : i18n._( 'Refresh' ),
                    handler : this.refresh.createDelegate( this ),
                    disabled : false,
                    cls : 'right-align'
                },{
                    xtype : 'button',
                    text : i18n._( 'Test Connectivity' ),
                    cls : 'test-connectivity',
                    handler : this.testConnectivity.createDelegate( this ),
                    disabled : false
                }]
            }]});
        this.cards.push( this.pppoePanel );


        this.cardPanel = new Ext.Panel({
            cls : 'untangle-form-panel',
            border : false,
            layout : 'card',
            items : this.cards,
            autoHeight : true,
            activePanel : 0,
            defaults : {
                autoHeight : true,
                border : false
                }
        });

        var configureText = i18n._("Configure the Internet Connection");
        
        var configure = new Ext.FormPanel({
            cls : "untangle-form-panel",
            border : false,
            autoHeight : true,
            labelWidth : Ung.SetupWizard.LabelWidth2,
            items : [{
                html : '<h2 class="wizard-title">'+configureText+'<h2>',
                border : false
            },{
                xtype : 'combo',
                fieldLabel : i18n._('Configuration Type'),
                name : 'configType',
                editable : false,
                store : this.configTypes,
                labelWidth : Ung.SetupWizard.LabelWidth2,
                mode : 'local',
                listeners : {
                    "select" : {
                        fn : this.onSelectConfig.createDelegate( this )
                    }
                },
                width : 115,
                listWidth : 120,
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
            items : [ configure, this.cardPanel]
        });

        this.isInitialized = false;

        var cardTitle = i18n._( "Internet Connection" );
        this.card = {
            title : cardTitle,
            panel : panel,
            onLoad : function( complete )
            {
                if ( !this.isInitialized ) {
                    this.cardPanel.layout.setActiveItem( 0 );
                }

                this.refreshNetworkDisplay();

                this.isInitialized = true;
                complete();
            }.createDelegate(this),
            onNext : this.saveSettings.createDelegate( this ),
            onValidate : this.validateInternetConnection.createDelegate(this)
        };
    },

    validateInternetConnection : function()
    {
        return _validate(this.cardPanel.layout.activeItem.items.items);
    },

    onSelectConfig : function( combo, record, index )
    {
        this.cardPanel.layout.setActiveItem( index );
    },

    saveSettings : function( handler )
    {
        Ext.MessageBox.wait( i18n._( "Saving Internet Connection Settings" ), i18n._( "Please wait" ));

        Ung.SetupWizard.ReauthenticateHandler.reauthenticate( this.afterReauthenticate.createDelegate( this, [ handler ] ));
    },

    afterReauthenticate : function( handler )
    {
        this.cardPanel.layout.activeItem.saveData( handler );
    },

    saveDHCP : function( handler, hideWindow )
    {
        if ( hideWindow == null ) {
            hideWindow = true;
        }

        var wanConfig = Ung.SetupWizard.CurrentValues.wanConfiguration;
        wanConfig.configType = "dynamic";

        var complete = this.complete.createDelegate( this, [ handler, hideWindow ], true );
        rpc.networkManager.setSetupSettings( complete, wanConfig );
    },
    saveStatic : function( handler, hideWindow )
    {
        var wanConfig = Ung.SetupWizard.CurrentValues.wanConfiguration;
        wanConfig.configType = "static";

        if ( hideWindow == null ) {
            hideWindow = true;
        }

        // delete unused stuff
        delete wanConfig.primaryAddress;
        delete wanConfig.dns1Str;
        delete wanConfig.dns2Str;
        delete wanConfig.gatewayStr;

        wanConfig.primaryAddressStr = this.staticPanel.find( "name", "ip" )[0].getValue() + "/" + this.staticPanel.find( "name", "netmask" )[0].getValue();
        wanConfig.gateway = this.staticPanel.find( "name", "gateway" )[0].getValue();
        wanConfig.dns1 = this.staticPanel.find( "name", "dns1" )[0].getValue();
        var dns2 = this.staticPanel.find( "name", "dns2" )[0].getValue();

        if ( dns2.length > 0 ) {
            wanConfig.dns2 = dns2;
        } else {
            wanConfig.dns2 = null;
        }

        var complete = this.complete.createDelegate( this, [ handler, hideWindow ], true );
        rpc.networkManager.setSetupSettings( complete, wanConfig );
    },

    savePPPoE : function( handler, hideWindow )
    {
        var wanConfig = Ung.SetupWizard.CurrentValues.wanConfiguration;
        wanConfig.configType = "pppoe";

        if ( hideWindow == null ) {
            hideWindow = true;
        }

        wanConfig.PPPoEUsername = this.pppoePanel.find( "name", "username" )[0].getValue();
        wanConfig.PPPoEPassword = this.pppoePanel.find( "name", "password" )[0].getValue();

        var complete = this.complete.createDelegate( this, [ handler, hideWindow ], true );
        rpc.networkManager.setSetupSettings( complete, wanConfig );
    },

    complete : function( result, exception, foo, handler, hideWindow )
    {
        if(exception) {
            Ext.MessageBox.show({
                title:i18n._( "Network Settings" ),
                msg:exception.message,
                width:300,
                buttons:Ext.MessageBox.OK,
                icon:Ext.MessageBox.INFO
            });
            return;
        }

        Ung.SetupWizard.CurrentValues.wanConfiguration = result;

        this.refreshNetworkDisplay();

        if ( hideWindow || ( hideWindow == null )) {
            Ext.MessageBox.hide();
        }

        if (handler != null)
            handler();
    },

    /* Refresh the current network settings (lease or whatever) */
    refresh : function()
    {
        Ext.MessageBox.wait(i18n._("Refreshing..."), i18n._("Please wait"));

        var handler = function() {
            Ext.MessageBox.hide();
        };

        Ung.SetupWizard.ReauthenticateHandler.reauthenticate( this.saveData.createDelegate( this, [ handler, false ] ));
    },

    testConnectivity : function()
    {
        if ( !( this.validateInternetConnection() === true )) {
            Ext.MessageBox.show({
                title : i18n._("Unable to Test Connectivity" ),
                msg : i18n._( "Please complete all of the required fields." ),
                width : 300,
                buttons : Ext.MessageBox.OK,
                icon : Ext.MessageBox.INFO
            });
            return;
        }

        Ext.MessageBox.wait(i18n._("Testing Connectivity..."), i18n._("Please wait"));

        var handler = this.execConnectivityTest.createDelegate( this );

        Ung.SetupWizard.ReauthenticateHandler.reauthenticate( this.saveData.createDelegate( this, [ handler, false ] ));
    },

    saveData : function( handler, hideWindow )
    {
        this.cardPanel.layout.activeItem.saveData( handler, hideWindow );
    },

    execConnectivityTest : function()
    {
        rpc.connectivityTester.getStatus( this.completeConnectivityTest.createDelegate( this ));
    },

    completeConnectivityTest : function( result, exception )
    {
        if ( exception ) {
            Ext.MessageBox.show({
                title:i18n._( "Network Settings" ),
                msg : i18n._( "Unable to complete connectivity test, please try again." ),
                width:300,
                buttons:Ext.MessageBox.OK,
                icon:Ext.MessageBox.INFO
            });
            return;
        }

        var message = "";

        if (( result.tcpWorking == false )  && ( result.dnsWorking == false )) {
            message = i18n._( "Warning! Internet and DNS failed." );
        } else if ( result.tcpWorking == false ) {
            message = i18n._( "Warning! DNS succeeded, but Internet failed." );
        } else if ( result.dnsWorking == false ) {
            message = i18n._( "Warning! Internet succeeded, but DNS failed." );
        } else {
            message = i18n._( "Success!" );
        }
        Ext.MessageBox.show({
            title:i18n._( "Internet Status" ),
            msg:message,
            width:300,
            buttons:Ext.MessageBox.OK,
            icon:Ext.MessageBox.INFO
        });
        //Ext.MessageBox.alert( i18n._( "Internet Status" ), message );
    },

    /* This does not reload the settings, it just updates what is
     * displayed inside of the User Interface. */
    refreshNetworkDisplay : function()
    {
        var c = 0;
        Ung.SetupWizard.CurrentValues.wanConfiguration = rpc.networkManager.getWizardWAN();
        var wanConfig = Ung.SetupWizard.CurrentValues.wanConfiguration;
        var isConfigured = (wanConfig.primaryAddress != null);

        if ( wanConfig.primaryAddress == Ung.SetupWizard.BOGUS_ADDRESS ) {
            wanConfig.primaryAddress = null;
            wanConfig.gateway = null;
            wanConfig.dns1 = null;
            wanConfig.dns2 = null;
            isConfigured = false;
        }
        
        if (isConfigured) {
            for ( c = 0; c < this.configTypes.length ; c++ ) {
                if (this.configTypes[c][0] == wanConfig.configType)
                    this.cardPanel.layout.setActiveItem( c );
            }

            this.updateValue( this.card.panel.find("name", "configType")[0], wanConfig.configType);
            
            for ( c = 0; c < this.cards.length ; c++ ) {
                var card = this.cards[c];
                if (wanConfig.primaryAddress != null) {
                    this.updateValue( card.find( "name", "ip" )[0] , wanConfig.primaryAddress.network );
                    this.updateValue( card.find( "name", "netmask" )[0] , wanConfig.primaryAddress.netmask );
                    this.updateValue( card.find( "name", "gateway" )[0], wanConfig.gateway );
                    this.updateValue( card.find( "name", "dns1" )[0], wanConfig.dns1 );
                    this.updateValue( card.find( "name", "dns2" )[0], wanConfig.dns2 );
                }
            }
        } else {
            /* not configured */
            for ( c = 0; c < this.cards.length ; c++ ) {
                var card = this.cards[c];
                this.updateValue( card.find( "name", "ip" )[0] , "" );
                this.updateValue( card.find( "name", "netmask" )[0] , "" );
                this.updateValue( card.find( "name", "gateway" )[0], "" );
                this.updateValue( card.find( "name", "dns1" )[0], "" );
                this.updateValue( card.find( "name", "dns2" )[0], "" );
            }
        }
    },

    /* Guard the field to shield strange values from the user. */
    updateValue : function( field, value )
    {
        if ( field == null ) {
            return;
        }
        if ( value == null || value == "0.0.0.0" ) {
            value = "";
        }

        field.setValue( value );
    }

});

Ung.SetupWizard.InternalNetwork = Ext.extend( Object, {
    constructor : function( config )
    {
        this.panel = new Ext.FormPanel({
            defaultType : 'fieldset',
            defaults : {
                autoHeight : true,
                labelWidth : Ung.SetupWizard.LabelWidth3
            },
            items : [{
                xtype : 'label',
                html : '<h2 class="wizard-title">'+i18n._( "Configure the Internal Network Interface" )+'</h2>'
            },{
                cls : 'noborder  wizard-internal-network',
                items : [{
                    xtype : 'radio',
                    name : 'bridgeOrRouter',
                    inputValue : 'router',
                    boxLabel : i18n._( 'Router' ),
                    ctCls : 'large-option',
                    hideLabel : 'true',
                    listeners : {
                        check : {
                            fn : function( checkbox, checked ) {
                                this.onSetRouter(checked);
                            }.createDelegate( this )
                        }
                    }
                },{
                    xtype : 'label',
                    html : "<div class='wizard-network-image-description'>" + i18n._('This is recommended if the external port is plugged into the internet connection. This enables NAT on the Internal Interface and DHCP.') + "</div>"
                },{
                    name : 'network',
                    xtype : 'textfield',
                    itemCls : 'wizard-internal-network-address spacing-margin-1',
                    fieldLabel : i18n._('Internal Address'),
                    vText : i18n._('Please enter a valid Network  Address'),
                    vtype : 'ipCheck',
                    allowBlank : false,
                    msgTarget : 'side',
                    maskRe : /(\d+|\.)/,
                    disabled : true,
                    value : "192.168.1.1",
                    validationEvent : 'blur'
                },{
                    name : "netmask",
                    itemCls : 'wizard-internal-network-address',
                    fieldLabel : i18n._( "Internal Netmask" ),
                    xtype : 'combo',
                    store : Ung.SetupWizard.NetmaskData,
                    mode : 'local',
                    triggerAction : 'all',
                    value : "255.255.255.0",
                    width : 120,
                    listWidth : 125,
                    disabled : true,
                    editable : false
                },{
                    xtype : 'checkbox',
                    hideLabel : true,
                    checked : true,
                    disabled : true,
                    name : 'enableDhcpServer',
                    itemCls : 'wizard-label-margin-9',
                    boxLabel : i18n._("Enable DHCP Server (default)")
                },{
                    xtype : 'label',
                    cls : 'wizard-network-image',
                    html : '<img src="/skins/' + Ung.SetupWizard.currentSkin + '/images/admin/wizard/router.png"/>'
                }]
            }, {
                cls : 'noborder wizard-internal-network',
                items : [{
                    xtype : 'radio',
                    name : 'bridgeOrRouter',
                    inputValue : 'bridge',
                    boxLabel : i18n._('Transparent Bridge'),
                    ctCls : 'large-option',
                    hideLabel : 'true',
                    checked : true
                },{
                    xtype : 'label',
                    html : "<div class='wizard-network-image-description'>" + i18n._('This is recommended if the external port is plugged into a firewall/router. This bridges Internal and External and disables DHCP.') + "</div>"
                },{
                    xtype : 'label',
                    cls : 'wizard-network-image',
                    html : '<img src="/skins/' + Ung.SetupWizard.currentSkin + '/images/admin/wizard/bridge.png"/>'
                }]
            }]
        });

        this.card = {
            title : i18n._( "Internal Network" ),
            panel : this.panel,
            onLoad : this.onLoadInternalSuggestion.createDelegate(this),
            onNext : this.saveInternalNetwork.createDelegate( this ),
            onValidate : this.validateInternalNetwork.createDelegate(this)
        };
    },
    onLoadInternalSuggestion : function( complete )
    {
        /* If the user modified the value, do not fetch a new value */
        if (( this.panel.find( "name", "netmask" )[0].getRawValue() != "255.255.255.0" ) ||
            (( this.panel.find( "name", "network" )[0].getValue() != "192.168.1.1" ) &&
             ( this.panel.find( "name", "network" )[0].getValue() != "172.16.0.1" ))) {
            complete();
            return;
        }

        // find the internal interface and see if its currently set to static.
        // if so change the default to router
        var netConf = rpc.networkManager.getNetworkConfiguration();
        if (netConf != null && netConf.interfaceList != null) {
            var intfs = netConf.interfaceList.list;
            for ( var c = 0 ;  c < intfs.length ; c++ ) {
                if (intfs[c].name == "Internal" && intfs[c].configType == "static" ) {
                    this.panel.find( "name", "bridgeOrRouter" )[0].setValue(true);
                    this.panel.find( "name", "bridgeOrRouter" )[1].setValue(false);
                }
            }
        }
        
        Ung.SetupWizard.ReauthenticateHandler.reauthenticate( this.loadInternalSuggestion.createDelegate( this, [ complete ] ));
    },
    loadInternalSuggestion : function( complete )
    {
        rpc.networkManager.getWizardInternalAddressSuggestion( this.completeLoadInternalSuggestion.createDelegate( this, [ complete ], true ), null );
    },
    completeLoadInternalSuggestion : function( result, exception, foo, handler )
    {
        if ( exception ) {
            /* Just ignore the attempt */
            handler();
            return;
        }

        this.panel.find( "name", "network" )[0].setValue( result["network"] );
        this.panel.find( "name", "netmask" )[0].setValue( result["netmask"] );
        handler();

    },
    onSetRouter : function(isSet){
        var ar = [this.panel.find('name','network')[0],this.panel.find('name','netmask')[0],this.panel.find('name','enableDhcpServer')[0]];
        for(var i=0;i<ar.length;i++){
            ar[i].setDisabled(!isSet);
        }
        _invalidate(ar);
    },

    validateInternalNetwork : function()
    {
        var rv = true;
        var nic = false;
        for(var i=0;i<this.panel.find('name','bridgeOrRouter').length;i++){
            if(this.panel.find('name','bridgeOrRouter')[i].getValue()){
                nic = this.panel.find('name','bridgeOrRouter')[i].inputValue;
                break;
            }
        }
        if ( nic == "router" ) {
            rv = _validate(this.panel.items.items);
        }
        return rv;
    },
    saveInternalNetwork : function( handler )
    {
        var value = this.panel.find( "name", "bridgeOrRouter" )[0].getGroupValue();

        if ( value == null ) {
            Ext.MessageBox.alert(i18n._( "Select a value" ), i18n._( "Please choose bridge or router." ));
            return;
        }

        Ext.MessageBox.wait( i18n._( "Saving Internal Network Settings" ), i18n._( "Please wait" ));

        Ung.SetupWizard.ReauthenticateHandler.reauthenticate( this.afterReauthenticate.createDelegate( this, [ handler ] ));
    },
    afterReauthenticate : function( handler )
    {
        var delegate = this.complete.createDelegate( this, [ handler ], true );
        var value = this.panel.find( "name", "bridgeOrRouter" )[0].getGroupValue();
        if ( value == 'bridge' ) {
            rpc.networkManager.setWizardNatDisabled( delegate );
        } else {
            var network = this.panel.find( "name", "network" )[0].getValue();
            var netmask = this.panel.find( "name", "netmask" )[0].getRawValue();
            var enableDhcpServer = this.panel.find( "name", "enableDhcpServer" )[0].getValue();
            rpc.networkManager.setWizardNatEnabled( delegate, network, netmask, enableDhcpServer );
        }
    },

    complete : function( result, exception, foo, handler )
    {
        if(exception) {
            Ext.MessageBox.alert(i18n._( "Local Network" ), i18n._( "Unable to save Local Network Settings" ) + exception.message );
            return;
        }

        Ext.MessageBox.hide();
        handler();
    }
});

Ung.SetupWizard.Email = Ext.extend( Object, {
    constructor : function( config )
    {
        this.showEmailConfig = false;

        this.panel = new Ext.FormPanel({
            defaultType : 'textfield',
            defaults : {
                autoHeight : true,
                cls : 'noborder'
            },
            items : [{
                xtype : 'label',
                itemCls : 'small-top-margin',
                html : '<h2 class="wizard-title">'+i18n._( "Email Configuration" )+ " <i>" + i18n._('(Optional)') + "</i>" + '</h2>'
            },{
                xtype : 'label',
                html : String.format(i18n._("The {0} Server needs a valid email configuration to send email."), oemName) + "<br/>" + "<br/>"
            },{
                xtype : 'label',
                html : "<b>" + i18n._("Step 1: ") + "</b>" + i18n._("Click") + " <i>" + i18n._("Verify Email Configuration") + "</i> " + i18n._("to test the current email configuration.") + "<br/>"
            },{
                xtype : 'button',
                text : i18n._( 'Verify Email Configuration' ),
                handler : this.emailTest.createDelegate( this ),
                iconCls : ' email-tester',
                cls : 'spacing-margin-1 email-tester'
            },{
                xtype : 'label',
                html : "<br/>" + "<b>" + i18n._("Step 2: ") + "</b>" + i18n._("If the verification succeeds click") + " <i>" + i18n._("Next") + " </i>" + i18n._(", otherwise configure and repeat step 1. ") + "<br/>"
            },{
                name:'advanced',
                xtype:'fieldset',
                title:i18n._("Email Configuration"),
                collapsible : true,
                collapsed : !this.showEmailConfig,
                autoHeight : true,
                style : 'margin-top:8px;',
                listeners : {
                    beforecollapse : this.onBeforeCollapse.createDelegate( this ),
                    beforeexpand : this.onBeforeExpand.createDelegate( this )
                },
                items : [{
                    name : 'from-address',
                    xtype : "fieldset",
                    autoHeight:true,

                    defaults : {
                        validationEvent : 'blur',
                        msgTarget : 'side'
                    },
                    items : [{
                        name : 'from-address-label',
                        xtype : 'label',
                        html : String.format(i18n._( "<b>Choose a <i>From Address</i> for emails sent from the {0} Server.</b>"),oemName)
                    },{
                        xtype : 'textfield',
                        name : 'from-address-textfield',
                        fieldLabel : i18n._("From Address"),
                        width : 230,
                        itemCls : 'spacing-margin-1',
                        vtype : 'emailAddressCheck',
                        allowBlank : false
                    }]
                },{
                    name : 'smtp-server-config',
                    xtype : "fieldset",
                    defaultType : 'textfield',
                    autoHeight:true,
                    defaults : {
                        msgTarget : 'side',
                        validationEvent : 'blur'
                    },
                    items : [{
                        name : 'smtp-message',
                        xtype : 'label',
                        html : i18n._( "<b>SMTP Configuration</b>" )
                    },{
                        xtype : 'radio',
                        name : 'smtp-send-directly',
                        checked : true,
                        inputValue : "directly",
                        boxLabel : i18n._( 'Send Email Directly (default).' ),
                        ctCls : 'wizard-label-margin-5',
                        hideLabel : 'true',
                        listeners : {
                            check : {
                                fn : function( checkbox, checked ) {
                                    if ( checked ) {
                                        this.onSetSendDirectly( true );
                                    }
                                }.createDelegate( this )
                            }
                        }
                    },{
                        xtype : 'radio',
                        name : 'smtp-send-directly',
                        inputValue : "smtp-server",
                        boxLabel : i18n._( 'Send Email using the specified SMTP server.' ),
                        hideLabel : 'true',
                        ctCls : 'wizard-label-margin-5',
                        listeners : {
                            check : {
                                fn : function( checkbox, checked ) {
                                    if ( checked ) {
                                        this.onSetSendDirectly( false );
                                    }
                                }.createDelegate( this )
                            }
                        }
                    },{
                        name : 'smtp-server-addr',
                        fieldLabel : i18n._( "SMTP Server" ),
                        maskRe : /[-/_0-9A-Za-z.]/,
                        vtype : 'hostname',
                        allowBlank : false
                    },{
                        name : 'smtp-server-port',
                        xtype : 'numberfield',
                        minValue : 0,
                        maxValue : 65536,
                        allowDecimals : false,
                        fieldLabel : i18n._( 'Port' ),
                        allowBlank : false
                    },{
                        name : 'smtp-server-requires-auth',
                        xtype : 'checkbox',
                        hideLabel : true,
                        value : false,
                        ctCls : 'wizard-label-margin-7 spacing-margin-1',
                        boxLabel : i18n._("Server Requires Authentication."),
                        listeners : {
                            check : {
                                fn : function( checkbox, checked ) {
                                    this.onSetRequiresAuth( checked );
                                }.createDelegate( this )
                            }
                        }
                    },{
                        xtype : 'textfield',
                        name : 'smtp-server-username',
                        fieldLabel : i18n._( "Username" ),
                        itemCls : 'wizard-label-margin-8',
                        allowBlank : false
                    },{
                        name : 'smtp-server-password',
                        inputType : 'password',
                        fieldLabel : i18n._( "Password" ),
                        itemCls : 'wizard-label-margin-8',
                        allowBlank : false
                    }]
                }]
            }]
        });

        var field = null;
        this.advancedArray = [];
        this.authArray = [];
        this.directlyArray = [];

        this.advancedArray.push( this.panel.find( "name", "from-address" )[0]);
        this.advancedArray.push( this.panel.find( "name", "smtp-server-config" )[0]);

        this.directlyArray.push( this.panel.find( "name", "smtp-server-addr" )[0]);
        this.directlyArray.push( this.panel.find( "name", "smtp-server-port" )[0]);
        field = this.panel.find( "name", "smtp-server-requires-auth" )[0];
        this.requiresAuth = field;
        this.directlyArray.push( field );

        field = this.panel.find( "name", "smtp-server-username" )[0];
        this.authArray.push( field );

        field = this.panel.find( "name", "smtp-server-password" )[0];
        this.authArray.push( field );

        this.onSetSendDirectly( true );

        this.emailTester = new Ung.SetupWizard.EmailTester();

        this.card = {
            title : i18n._( "Email" ),
            cardTitle : i18n._( "Email Configuration" ),
            panel : this.panel,
            onNext : this.saveSettings.createDelegate( this ),
            onLoad : this.setFromAddress.createDelegate( this ),
            onValidate : this.validateEmailConfiguration.createDelegate(this)
        };
    },
    emailTest : function()
    {
        if ( !this.validateEmailConfiguration()) {
            return;
        }

        this.saveSettings( this.emailTester.showTester.createDelegate( this.emailTester ));
    },

    setFromAddress : function( handler )
    {
        if ( !this.isInitialized ) {
            hostname = "example.com";
            this.panel.find( "name", "from-address-textfield" )[0].setValue( oemName.toLowerCase() + "@" + hostname );
        }

        this.isInitialized = true;

        handler();
    },

    validateEmailConfiguration : function()
    {
        var rv = true;
        if(this.showEmailConfig){//advanced email config checked
            if(!_validate(this.panel.find('name','from-address')[0].items.items)) {
                rv = !rv;
            }
            if(this.panel.find('name','smtp-send-directly')[1].getValue()){
                if(!_validate(this.directlyArray) && rv){
                    rv = !rv;
                }
            }
            if(this.panel.find('name','smtp-server-requires-auth')[0].getValue()){
                if(!_validate(this.authArray) && rv){
                    rv = !rv;
                }
            }

        }

        return rv;
    },

    onSetSendDirectly : function( isSendDirectly )
    {
        var length = this.directlyArray.length;
        for ( var c = 0 ; c < length ; c++ ) {
            this.directlyArray[c].setDisabled( isSendDirectly );

        }
        //if(!isSendDirectly){
        _invalidate(this.directlyArray);
        //}
        if ( isSendDirectly ) {
            this.onSetRequiresAuth( false );
        }
    },

    onSetRequiresAuth : function( requiresAuth )
    {
        var length = this.authArray.length;
        for ( var c = 0 ; c < length ; c++ ) {
            this.authArray[c].setDisabled( !requiresAuth );
        }
        if(!requiresAuth){
            _invalidate(this.authArray);
        }
    },

    onBeforeCollapse : function( panel, animate )
    {
        this.showEmailConfig = false;
        return true;
    },

    onBeforeExpand : function( panel, animate )
    {
        this.showEmailConfig = true;
        return true;
    },

    saveSettings : function( handler )
    {
        Ext.MessageBox.wait( i18n._( "Saving Email Configuration..." ), i18n._( "Please wait" ));

        Ung.SetupWizard.ReauthenticateHandler.reauthenticate( this.afterReauthenticate.createDelegate( this, [ handler ] ));
    },

    afterReauthenticate : function( handler )
        {
            var settings = Ung.SetupWizard.CurrentValues.mailSettings;

            settings.fromAddress = oemName.toLowerCase() + "@" + "example.com";
            settings.useMxRecords = true;
            settings.smtpHost = "";
            settings.smtpPort = 25;
            settings.authUser = "";
            settings.authPass = "";
            settings.reportEmail = "";
            
            if ( this.showEmailConfig ) {
                settings.fromAddress = this.panel.find( "name", "from-address-textfield" )[0].getValue();

                if ( this.panel.find( "name", "smtp-send-directly" )[0].getGroupValue() == "smtp-server" ) {
                    settings.useMxRecords = false;
                    settings.smtpHost = this.panel.find( "name", "smtp-server-addr" )[0].getValue();
                    settings.smtpPort = this.panel.find( "name", "smtp-server-port" )[0].getValue();

                    if ( this.panel.find( "name", "smtp-server-requires-auth" )[0].getValue()) {
                        settings.authUser = this.panel.find( "name", "smtp-server-username" )[0].getValue();
                        settings.authPass = this.panel.find( "name", "smtp-server-password" )[0].getValue();
                    }
                }
            }

            rpc.mailSender.setMailSettings(this.complete.createDelegate( this, [ handler ], true ), settings );
        },

    complete : function( result, exception, foo, handler )
    {
        if ( exception ) {
            Ext.MessageBox.alert(i18n._( "Mail Settings" ),i18n._( "Unable to save settings." ) + exception.message );
            return;
        }

        Ext.MessageBox.hide();

        handler();
    }
});

Ung.SetupWizard.Complete = Ext.extend( Object, {
    constructor : function( config )
    {
        var panel = new Ext.FormPanel({
            items : [{
                xtype : 'label',
                html : '<h2 class="wizard-title">'+i18n._( "Congratulations!" )+'</h2>'
            },{
                xtype : 'label',
                html : String.format(i18n._( '<b>The {0} Server is now configured.</b><br/><br/>You are now ready to login and download applications.' ),oemName),
                cls : 'noborder'
            }]
        });

        this.card = {
            title : i18n._( "Finished" ),
            cardTitle : i18n._( "Congratulations!" ),
            panel : panel,
            onNext : this.openUserInterface.createDelegate( this )
        };
    },

    openUserInterface : function( handler )
    {
        Ext.MessageBox.wait( i18n._( "Loading User Interface..." ), i18n._( "Please wait" ));

        //now that we are done, create the UID
        rpc.jsonrpc.RemoteUvmContext.createUID();

        //and set a flag so the wizard wont run again
        rpc.jsonrpc.RemoteUvmContext.wizardComplete();

        //now open the UI
        window.location.href="/webui/startPage.do?firstTimeRun=true";
    }
});

Ung.SetupWizard.TimeZoneStore = [];

Ung.Setup = {
    isInitialized : false,
    init : function()
    {
        if ( this.isInitialized == true ) {
            return;
        }
        this.isInitialized = true;

        Ext.WindowMgr.zseed = 20000;

        rpc = {};

        /* Initialize the timezone data */
        for ( var i = 0; i < Ung.TimeZoneData.length; i++) {
            Ung.SetupWizard.TimeZoneStore.push([Ung.TimeZoneData[i][2], "(" + Ung.TimeZoneData[i][0] + ") " + Ung.TimeZoneData[i][1]]);
        }

        /* Initialize the netmask data */
        Ung.SetupWizard.NetmaskData = [
            "255.0.0.0",       "255.128.0.0",     "255.192.0.0",     "255.224.0.0",
            "255.240.0.0",     "255.248.0.0",     "255.252.0.0",     "255.254.0.0",
            "255.255.0.0",     "255.255.128.0",   "255.255.192.0",   "255.255.224.0",
            "255.255.240.0",   "255.255.248.0",   "255.255.252.0",   "255.255.254.0",
            "255.255.255.0",   "255.255.255.128", "255.255.255.192", "255.255.255.224",
            "255.255.255.240", "255.255.255.248", "255.255.255.252"
        ];

        rpc.setup = new JSONRpcClient("/setup/JSON-RPC").SetupContext;
        
        oemName = rpc.setup.getOemName();

        i18n = new Ung.I18N( { "map" : Ung.SetupWizard.CurrentValues.languageMap });

        document.title = i18n._( "Setup Wizard" );

        var welcome = new Ung.SetupWizard.Welcome();
        var settings = new Ung.SetupWizard.Settings();
        var interfaces = new Ung.SetupWizard.Interfaces();
        var internet = new Ung.SetupWizard.Internet();
            var email = new Ung.SetupWizard.Email();
        var complete = new Ung.SetupWizard.Complete();

        var cards = [];
        cards.push( welcome.card );
        cards.push( settings.card );
        cards.push( interfaces.card );
        cards.push( internet.card );
        var internal = new Ung.SetupWizard.InternalNetwork();
        cards.push( internal.card );
        cards.push( email.card );
        cards.push( complete.card );

        this.wizard = new Ung.Wizard({
            height : 500,
            width : 800,
            cardDefaults : {
                labelWidth : Ung.SetupWizard.LabelWidth,
                cls : 'untangle-form-panel'
            },
            cards : cards,
            disableNext : false,
            el : "container"
        });

        this.wizard.render();
        Ext.QuickTips.init();

        if ( false ) {
            /* DEBUGGING CODE (Change to true to dynamically go to any page you want on load.) */
            var debugHandler = function() {
                this.wizard.goToPage( 2 );
            }.createDelegate( this );
            var ss = new Ung.SetupWizard.SettingsSaver( null, debugHandler );

            ss.password = "passwd";
            ss.authenticate( null, null );
            /* DEBUGGING CODE */
        } else {
            this.wizard.goToPage( 0 );
        }
    }
};

Ung.SetupWizard.ReauthenticateHandler = {
    username : "admin",
    password : "",

    /* Must reauthenticate in order to refresh the managers */
    reauthenticate : function( handler )
    {
        Ext.Ajax.request({
            params : {
                username : this.username,
                password : this.password
            },
            /* If it uses the default type then this will not work
             * because the authentication handler does not like utf8 */
            headers : {
                'Content-Type' : "application/x-www-form-urlencoded"
            },
            url : '/auth/login?url=/webui/setupSettings.js&realm=Administrator',
            callback : this.reloadManagers.createDelegate( this, [ handler ], 4 )
        });
    },

    reloadManagers : function( options, success, response, handler )
    {
        if ( success ) {
            /* It is very wrong to do this all synchronously */
            rpc.jsonrpc = new JSONRpcClient( "/webui/JSON-RPC" );
            rpc.adminManager = rpc.jsonrpc.RemoteUvmContext.adminManager();
            rpc.networkManager = rpc.jsonrpc.RemoteUvmContext.networkManager();
            rpc.connectivityTester = rpc.jsonrpc.RemoteUvmContext.getRemoteConnectivityTester();
            rpc.toolboxManager = rpc.jsonrpc.RemoteUvmContext.toolboxManager();
            rpc.mailSender = rpc.jsonrpc.RemoteUvmContext.mailSender();
            handler();
        } else {
            Ext.MessageBox.alert( i18n._( "Unable to save settings." ));
        }
    }
};


