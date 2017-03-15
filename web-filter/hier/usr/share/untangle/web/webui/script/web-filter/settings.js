Ext.define('Webui.web-filter.settings',{
    extend:'Webui.web-filter-base.settings',
    helpSourceName: 'web_filter',
    getAppSummary: function() {
        return i18n._("Web Filter scans and categorizes web traffic to monitor and enforce network usage policies.");
    },
    buildPanelAdvanced: function() {
        this.callParent(arguments);
        var fieldSet = this.panelAdvanced.down('fieldset[name="fieldset_miscellaneous"]');
        //Insert new fields at the beginning of the default fieldset.
        fieldSet.insert(0,[{
            //HTTPS options
            xtype: "checkbox",
            boxLabel: i18n._("Process HTTPS traffic by SNI (Server Name Indication) information if present"),
            hideLabel: true,
            name: "scan_https_sni",
            checked: this.settings.enableHttpsSni,
            listeners: {
                "change": {
                    fn: Ext.bind(function(elem, checked) {
                        this.settings.enableHttpsSni = checked;
                        if (checked) {
                            Ext.getCmp('scan_https_sni_cert_fallback').enable();
                        }
                        else {
                            Ext.getCmp('scan_https_sni_cert_fallback').setValue(false);
                            Ext.getCmp('scan_https_sni_ip_fallback').setValue(false);
                            Ext.getCmp('scan_https_sni_cert_fallback').disable();
                            Ext.getCmp('scan_https_sni_ip_fallback').disable();
                        }
                    }, this)
                }
            }
        },{
            xtype: "checkbox",
            boxLabel: i18n._("Process HTTPS traffic by hostname in server certificate when SNI information not present"),
            hideLabel: true,
            margin: '0 0 5 20',
            id: "scan_https_sni_cert_fallback",
            name: "scan_https_sni_cert_fallback",
            checked: this.settings.enableHttpsSniCertFallback,
            disabled: !this.settings.enableHttpsSni,
            listeners: {
                "change": {
                    fn: Ext.bind(function(elem, checked) {
                        this.settings.enableHttpsSniCertFallback = checked;
                        if (checked) {
                            Ext.getCmp('scan_https_sni_ip_fallback').enable();
                        }
                        else {
                            Ext.getCmp('scan_https_sni_ip_fallback').setValue(false);
                            Ext.getCmp('scan_https_sni_ip_fallback').disable();
                        }
                    }, this)
                }
            }
        },{
            xtype: "checkbox",
            boxLabel: i18n._("Process HTTPS traffic by server IP if both SNI and certificate hostname information are not available"),
            hideLabel: true,
            margin: '0 0 5 40',
            id: "scan_https_sni_ip_fallback",
            name: "scan_https_sni_ip_fallback",
            checked: this.settings.enableHttpsSniIpFallback,
            disabled: !this.settings.enableHttpsSni,
            listeners: {
                "change": {
                    fn: Ext.bind(function(elem, checked) {
                        this.settings.enableHttpsSniIpFallback = checked;
                    }, this)
                }
            }
        },{
            //Safe search
            xtype: "checkbox",
            boxLabel: i18n._("Enforce safe search on popular search engines"),
            hideLabel: true,
            name: "Enforce safe search",
            checked: this.settings.enforceSafeSearch,
            listeners: {
                "change": {
                    fn: Ext.bind(function(elem, checked) {
                        this.settings.enforceSafeSearch = checked;
                    }, this)
                }
            }
        },{
            xtype: "checkbox",
            boxLabel: i18n._("Block QUIC (UDP port 443)"),
            hideLabel: true,
            name: 'Block QUIC',
            checked: this.settings.blockQuic,
            listeners: {
                "change": {
                    fn: Ext.bind(function(elem, checked) {
                        this.settings.blockQuic = checked;
                    }, this)
                }
            }
        }]);
        //Extend bypass to allow optional passworded access.
        var combo = this.panelAdvanced.down('combo[name="user_bypass"]');
        combo.addListener("select", this.onSelectBypass, this);
        var userByPass = this.panelAdvanced.down('combo[name="user_bypass"]');
        fieldSet.insert(
            userByPass.ownerCt.items.indexOf( userByPass) + 1, {
            xtype: 'fieldset',
            name: 'password',
            hidden: this.settings.unblockMode == "None",
            items: [{
                xtype: "checkbox",
                boxLabel: i18n._("Require Password"),
                hideLabel: true,
                checked: this.settings.unblockPasswordEnabled,
                listeners: {
                    "change": {
                        fn: this.onCheckPasswordEnable,
                        scope: this
                    }
                }
            },{
                xtype: "radio",
                disabled: !this.settings.unblockPasswordEnabled,
                passwordField: true,
                boxLabel: "Administrator Password",
                hideLabel: true,
                name: "unblockPasswordAdmin",
                checked: this.settings.unblockPasswordAdmin
            },{
                xtype: "radio",
                disabled: !this.settings.unblockPasswordEnabled,
                boxLabel: "Custom Password",
                passwordField: true,
                hideLabel: true,
                name: "unblockPasswordAdmin",
                checked: !this.settings.unblockPasswordAdmin,
                listeners: {
                    "change": {
                        fn: function(elem,checked) {
                            this.settings.unblockPasswordAdmin = !checked;
                            this.down('textfield[name="unblockPassword"]').setDisabled( !checked );
                        },
                        scope: this
                    }
                }
            },{
                xtype: "textfield",
                disabled: !this.settings.unblockPasswordEnabled || this.settings.unblockPasswordAdmin,
                fieldLabel: "Password",
                passwordField: true,
                name: "unblockPassword",
                value: this.settings.unblockPassword,
                listeners: {
                    "change": {
                        fn: function(elem,newValue,oldValue) {
                            this.settings.unblockPassword = newValue;
                        },
                        scope: this
                    }
                }
            }]
        });
        //Clear cache button at the end.
        fieldSet.add({
            xtype: "button",
            text: i18n._("Clear Category URL Cache."),
            handler: Ext.bind(this.clearHostCache, this )
        });
        return this.panelAdvanced;
    },

    buildPanelCategories: function() {
        this.callParent(arguments);
        this.categoriesPanel.add({
            xtype: 'container',
            layout: 'hbox',
            style: {
                padding: '5px'
            },
            items: [{
                xtype: 'button',
                text: i18n._("Site Lookup"),
                iconCls: 'test-icon',
                name: "sitelookup",
                handler: Ext.bind(function() {
                    this.onSiteLookup();
                }, this)
            },{
                xtype: 'displayfield',
                value: i18n._("Search for a site's category"),
                style: {
                    marginLeft: '10px'
                }
            }]
        });
        return this.categoriesPanel;
    },
    onSelectBypass: function(elem, record, options) {
        var newValue = record.get("field1");
        this.panelAdvanced.query('fieldset[name="password"]')[0].setVisible( newValue == "None" ? false : true );
    },

    onCheckPasswordEnable: function(elem, checked) {
        this.settings.unblockPasswordEnabled = checked;
        var fields = this.query('[passwordField=true]'), c;
        for ( c = 0 ; c < fields.length ; c++ ) {
            fields[c].setDisabled( !checked );
        }
        if ( checked ) {
            this.down('textfield[name="unblockPassword"]').setDisabled( this.settings.unblockPasswordAdmin );
        }
    },

    onSiteLookup: function( elem, checked ) {
        if( !this.sitelookupMessageBox ){
            //Build store for suggested category from the tabset's general settings storage.
            var categoriesData = [];
            for( var j = 0; j < this.settings.categories.list.length; j++){
                category = this.settings.categories.list[j];
                categoriesData.push({
                    'id': category.string,
                    'name': category.name
                });
            }
            var categoriesStore = Ext.create( 'Ext.data.Store',{
                    fields: [ 'id', 'name' ],
                    data: categoriesData
                }
            );

            this.sitelookupMessageBox = Ext.create('Ext.Window',{
                layout: 'fit',
                width: 500,
                height: 350,
                modal: true,
                title: i18n._('Site Lookup'),
                closeAction: 'hide',
                items: {
                    xtype: 'panel',
                    autoScroll: true,
                    items: [{
                        xtype: 'fieldset',
                        items: [{
                            xtype: 'displayfield',
                            style: { marginBottom: '10px' },
                            value: i18n._("Type the site's URL to find its category"),
                            width: 400
                        },{
                            xtype: 'textfield',
                            name: 'URL',
                            id: 'sitelookup-site',
                            allowBlank: false,
                            fieldLabel: i18n._('Site URL'),
                            width: 400,
                            listeners: {
                                "blur": {
                                    fn: Ext.bind( function( elem, The, eOpts ){
                                        Ext.getCmp( 'sitelookup-category').setVisible( false );
                                        Ext.getCmp( 'sitelookup-suggest')
                                            .setVisible( false )
                                            .setValue( false );
                                        Ext.getCmp( 'sitelookup-suggest-message').setVisible( false );
                                        Ext.getCmp( 'sitelookup-suggest-button').setVisible( false );
                                    }, this )
                                }
                            }
                        },{
                            id: 'sitelookup-category',
                            xtype: 'displayfield',
                            fieldLabel: i18n._("Category"),
                            hidden: true
                        },{
                            id: 'sitelookup-suggest',
                            xtype: 'checkbox',
                            boxLabel:  i18n._("Suggest a different category"),
                            hidden: true,
                            listeners: {
                                "change": {
                                    fn: Ext.bind(function(elem, checked ) {
                                        Ext.getCmp( 'sitelookup-suggest-fieldset').setVisible( checked );
                                        Ext.getCmp( 'sitelookup-suggest-button' ).setVisible( checked );
                                        Ext.getCmp('sitelookup-suggest-last').setVisible( false );
                                        var sitelookupSuggestions = Ext.state.Manager.get( 'siteLookup-sites' );
                                        if( typeof( sitelookupSuggestions) != 'undefined' ){
                                            var lastDate=sitelookupSuggestions[Ext.getCmp('sitelookup-site').getValue()];
                                            if( lastDate ){
                                                Ext.getCmp( 'sitelookup-suggest-last')
                                                    .setVisible( true )
                                                    .setValue(i18n._("Last suggested on") + ": " + i18n.timestampFormat(lastDate));
                                            }
                                        }
                                    }, this)
                                }
                            }
                        },{
                            id: 'sitelookup-suggest-fieldset',
                            xtype: 'fieldset',
                            hidden: true,
                            items: [{
                                xtype: 'displayfield',
                                value: i18n._("NOTE: This is only a suggestion and may not be accepted. If accepted it may take a few days to become active."),
                                style: {
                                    marginBottom: '10px'
                                }
                            },{
                                id: 'sitelookup-suggest-category',
                                xtype: 'combobox',
                                width: 300,
                                editable: false,
                                queryMode: 'local',
                                store: categoriesStore,
                                valueField: 'id',
                                displayField: 'name'
                            },{
                                id: 'sitelookup-suggest-last',
                                xtype: 'displayfield',
                                hidden: true
                            },{
                                id: 'sitelookup-suggest-message',
                                xtype: 'displayfield',
                                hidden: true
                             }]
                        }]
                    }]
                },

                buttons: [{
                    id: 'sitelookup-search-button',
                    text: i18n._("Search"),
                    disabled: false,
                    handler: Ext.bind(function() {
                        Ext.getCmp( 'sitelookup-suggest-button' ).setVisible( false );
                        Ext.getCmp('sitelookup-suggest').setValue(false).setVisible( false );
                        var url = Ext.getCmp('sitelookup-site');
                        if( url.getValue() == "" ){
                            return;
                        }
                        var result = this.getRpcNode().lookupSite( url.getValue() );
                        if( result.list){
                            var categoryList = [];
                            for( var i = 0; i < result.list.length; i++){
                                for( var j = 0 ; j < categoriesData.length; j++ ){
                                    category = categoriesData[j];
                                    if( result.list[i] == category.id ){
                                        categoryList.push( category.name );
                                        Ext.getCmp( 'sitelookup-suggest-category' ).setValue( category.id );
                                    }
                                }
                            }
                            Ext.getCmp('sitelookup-category')
                                .setVisible(true)
                                .setValue(categoryList.join(","));
                            Ext.getCmp('sitelookup-suggest').setVisible(true);
                        }
                    }, this)
                },{
                    id: 'sitelookup-suggest-button',
                    text: i18n._("Suggest"),
                    hidden: true,
                    handler: Ext.bind(function() {
                        var url = Ext.getCmp('sitelookup-site');
                        var category = Ext.getCmp( 'sitelookup-suggest-category' );
                        if( url.getValue() == "" ) {
                            return;
                        }
                        var result = this.getRpcNode().recategorizeSite( url.getValue(), category.getValue() );
                        Ext.getCmp( 'sitelookup-suggest-message')
                            .setVisible( true )
                            .setValue(
                                ( result == category.getValue() ) ?
                                    i18n._("Suggestion submitted.") :
                                    i18n._("Unable to submit suggestion.  Try again later.")
                            );
                        var sitelookupSuggestions = Ext.state.Manager.get( 'siteLookup-sites' );
                        if( typeof( sitelookupSuggestions) == 'undefined' ){
                            sitelookupSuggestions = {};
                        }
                        sitelookupSuggestions[url.getValue()] = new Date().getTime();
                        Ext.state.Manager.set( 'siteLookup-sites', sitelookupSuggestions );
                    }, this)
                },{
                    text: i18n._("Close"),
                    handler: Ext.bind(function() {
                        this.sitelookupMessageBox.destroy();
                        this.sitelookupMessageBox = null;
                    }, this)
                }]
            });
        } else {
            Ext.getCmp('sitelookup-site').setValue('').clearInvalid();
            Ext.getCmp('sitelookup-category').setVisible(false);
        }
        this.sitelookupMessageBox.show();
    },

    clearHostCache: function() {
        Ext.MessageBox.wait( i18n._( "Clearing Host Cache..." ), i18n._( "Please Wait" ));
        this.getRpcNode().clearCache( Ext.bind(this.completeClearHostCache, this ), true);
    },

    completeClearHostCache: function( result, exception ) {
        if( Ung.Util.handleException(exception, Ext.bind(function() {
                Ext.MessageBox.alert(i18n._("Error Clearing Host Cache."), i18n._("There was an error clearing the host cache, please try again."));
            }, this ))) {
            return;
        }

        Ext.MessageBox.show({
            title: i18n._( "Cleared Host Cache" ),
            msg: i18n._( "The Host Cache was cleared succesfully." ),
            buttons: Ext.MessageBox.OK,
            icon: Ext.MessageBox.INFO
        });
    }
});
//# sourceURL=web-filter-settings.js