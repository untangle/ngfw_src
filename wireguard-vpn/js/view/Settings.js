Ext.define('Ung.apps.wireguard-vpn.view.Settings', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-wireguard-vpn-settings',
    itemId: 'settings',
    title: 'Settings'.t(),
    scrollable: true,

    withValidation: true,
    padding: '8 5',

    defaults: {
        labelWidth: 200
    },

    items: [{
        xtype: 'fieldcontainer',
        layout: 'hbox',
        items: [{
            fieldLabel: 'Listen port'.t(),
            xtype: 'textfield',
            vtype: 'isSinglePortValid',
            maxLength: 5,
            enforceMaxLength :true,
            bind: {
                value: '{settings.listenPort}'
            },
            allowBlank: false,
            labelWidth: 200,
            padding: '8 5 5 0',
            listeners: {
                change: function(field, newValue, oldValue) {
                    this.up('app-wireguard-vpn').getController().settingsChangeListener(field, newValue, 'listenPort');
                }
            }
        },{
            xtype: 'label',
            cls: 'warningLabel',
            hidden: true,
            margin: '18 0 5 5'
        }]
    },{
        fieldLabel: 'Keepalive interval'.t(),
        xtype: 'textfield',
        maxLength: 5,
        bind: {
            value: '{settings.keepaliveInterval}'
        },
        regex: RegExp("^[1-9]\\d*$"),
        regexText: 'Only accepts valid positive integer.'.t(),
        allowBlank: false
    },{
        xtype: 'fieldcontainer',
        layout: 'hbox',
        items: [{
            fieldLabel: 'MTU'.t(),
            xtype: 'textfield',
            vtype: 'mtu',
            bind: {
                value: '{settings.mtu}'
            },
            allowBlank: false,
            labelWidth: 200,
            padding: '8 5 5 0',
            listeners: {
                change: function(field, newValue, oldValue) {
                    this.up('app-wireguard-vpn').getController().settingsChangeListener(field, newValue, 'mtu');
                }
            }
        },{
            xtype: 'label',
            cls: 'warningLabel',
            hidden: true,
            margin: '18 0 5 5'
        }],
    }, {
        xtype: "checkbox",
        bind: '{settings.mapTunnelDescUser}',
        fieldLabel: 'Set authenticated username as tunnel description'.t(),
        padding: '8 5 5 0',
        autoEl: {
            tag: 'div',
            'data-qtip': "If enabled, tunnel description will be set as authenticated user".t()
        },
        checked: false
    }, {
        xtype: 'fieldset',
        title: 'Remote Client Configuration'.t(),
        layout: {
            type: 'vbox'
        },
        defaults: {
            labelWidth: 190,
            padding: "0 0 10 0"
        },
        items:[{
            xtype: 'textfield',
            fieldLabel: 'DNS Server'.t(),
            vtype: 'ipMatcher',
            bind: {
                value: '{settings.dnsServer}'
            }
        },{
            xtype: 'textfield',
            fieldLabel: 'DNS Search Domains'.t(),
            bind: {
                value: '{settings.dnsSearchDomain}'
            },
            emptyText: '[no domain]'.t()
        },{
            xtype: 'fieldcontainer',
            layout: 'hbox',
            items: [{
                xtype: 'label',
                text: 'Local Network Profiles:'.t(),
                width: 195
            },{
                xtype: 'ungrid',
                itemId: 'localNetworkGrid',
                tbar: ['@addInline'],
                recordActions: ['delete'],
                listProperty: 'settings.networkProfiles.list',
                width: 562,
                bind: '{networkProfiles}',
                restrictedRecords: {
                    keyMatch: 'reserved',
                    valueMatch: true
                },
                listeners: {
                    cellclick: function(grid,td,cellIndex,record,tr,rowIndex,e,eOpts)  {
                        if (record.data.profileName === 'Full Tunnel') { 
                            record.set('reserved', true);
                            return false; 
                        }
                    },
                    afterrender: function(grid) {
                        Ext.Function.defer(function() { 
                            var store = grid.getStore();
                            store.each(function(record) {
                                if(record.get('profileName') === 'Full Tunnel')
                                    record.set('reserved', true);
                            });
                        }, 2000);
                    }
                },
                emptyRow: {
                    javaClass: 'com.untangle.app.wireguard_vpn.WireGuardVpnNetworkProfile',
                    profileName: 'DefaultProfileName',
                    subnetsAsString: '10.0.0.0/24,10.1.0.0/24',
                },
                columns: [{
                    dataIndex: 'profileName',
                    header: 'Profile Name',
                    width: 200,
                    editor:{
                        xtype: 'textfield',
                        allowBlank: false,
                        emptyText: '[enter profile name]'.t(),
                        blankText: 'Invalid profile name'.t(),
                        validator: function(value) {
                            var grid = this.up('grid[itemId=localNetworkGrid]'),
                                store = grid.getStore(),
                                record = grid.getSelectionModel().getSelection()[0];
                            var me = this,
                                defaultProfileName = me.up('#settings').down("#localNetworkGrid").initialConfig.emptyRow.profileName;
                            if(value == defaultProfileName) return 'Change default profile name.'.t();
                            // Check if a record with the same name exists in the store
                            var isNameUnique = store.findBy(function(profile) {
                                if(record === profile) return false;
                                return profile.get('profileName') === value;
                            });
                            return isNameUnique === -1 ? true : 'Profile name already exists.'.t();
                        }
                    }
                }, {
                    dataIndex: 'subnetsAsString',
                    header: 'Network Addresses',
                    width: 300,
                    flex: 1,
                    renderer: function(value) {
                        var qtip = value ? value.replace(/,\s*/g, ', ') : '';
                        return '<span data-qtip="' + Ext.String.htmlEncode(qtip) + '">' +
                               Ext.String.htmlEncode(value) +
                               '</span>';
                    },
                    editor:{
                        xtype: 'textfield',
                        vtype: 'cidrBlockList',
                        allowBlank: false,
                        emptyText: '[enter comma seperated networks]'.t(),
                        blankText: 'Invalid address specified'.t(),
                        validator: function(value) {
                            try{
                                var isValidVtypeField = Ext.form.field.VTypes[this.vtype](value);
                                if(!isValidVtypeField) return true;

                                networks = value.split(',');
                                profileNets = [];
                                for(var i=0; i < networks.length; i++) {
                                    if(profileNets.indexOf(networks[i]) !== -1) return 'Duplicate networks in the profile'.t();
                                    if(profileNets.length > 0) {
                                        res = Util.findIpPoolConflict(networks[i], profileNets, this, true);
                                        if(res != true) return res; 
                                    } 
                                    profileNets.push(networks[i]);

                                    var res = Util.networkValidator(networks[i]);
                                    if(res != true) return res;

                                    var me = this,
                                        settings = me.up('#settings'),
                                        peerNetworkIp = settings.down('#peerNetworkIp').getValue(),
                                        localNetworkStore = [];

                                    if(peerNetworkIp) localNetworkStore.push(peerNetworkIp);

                                    res = Util.findIpPoolConflict(networks[i], localNetworkStore, this, true);
                                    if(res != true) return res;
                                }    
                                return true;                          
                            } catch(err) {
                                console.log(err);
                                return true;
                            }                        
                        }
                    },
                }]
            }, {
                xtype: 'label',
                cls: 'warningLabel',
                margin: '13 0 5 25',
                html: '<span class="fa fa-exclamation-triangle" style="color: orange;" data-qtip="On profile name edit clients will need to configure their connections!"></span>'
            }]
        }]
    }, {
        xtype: 'fieldset',
        title: 'Peer IP Address Pool'.t(),
        layout: {
            type: 'vbox'
        },
        defaults: {
            labelWidth: 190
        },
        items:[{
            fieldLabel: 'Assignment'.t(),
            xtype: 'combobox',
            bind: {
                value: '{settings.autoAddressAssignment}'
            },
            editable: false,
            queryMode: 'local',
            store: [
                [true, 'Automatic'.t()],
                [false, 'Self-assigned'.t()]
            ],
            forceSelection: true
        },{
            xtype: 'fieldcontainer',
            layout: 'hbox',
            defaults: {
                labelWidth: 190
            },
            items: [{
                    fieldLabel: 'Network Space'.t(),
                    xtype: 'textfield',
                    vtype: 'cidrAddr',
                    itemId: 'peerNetworkIp',
                    bind: {
                        value: '{settings.addressPool}',
                        disabled: '{settings.autoAddressAssignment}',
                        editable: '{!settings.autoAddressAssignment}'
                    },
                    listeners: {
                        change: function(field, newValue, oldValue) {
                            this.up('app-wireguard-vpn').getController().settingsChangeListener(field, newValue, 'addressPool');
                        }
                    },
                    validator: function(value) {
                        try{
                            var isValidVtypeField = Ext.form.field.VTypes[this.vtype](value);
                            if(!isValidVtypeField){
                                return true;
                            }

                            // Ensure the network is in a private IP range
                            if (!Util.isPrivateCIDR(value)) {
                                return "Only private IP ranges are allowed (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16)";
                            }
                            var localNetworkStoreFn = this.up('#settings').down('#localNetworkGrid').getStore();

                            var localNetworkStore = [];
                            
                            localNetworkStoreFn.each(function (profile){
                                if(profile.get("subnetsAsString")){
                                    var subnets = profile.get("subnetsAsString").split(',');
                                    for (var i = 0; i < subnets.length; i++) {
                                        var subnet = subnets[i].trim();
                                        if (subnet !== "0.0.0.0/0" && localNetworkStore.indexOf(subnet) === -1) {
                                            localNetworkStore.push(subnet);
                                        }
                                    }
                                }
                            });
                            return Util.findIpPoolConflict(value, localNetworkStore, this, true);

                        } catch(err) {
                            console.log(err);
                            return true;
                        }                        
                    }
                },
                {
                    xtype:'button',
                    text: 'New Network Space'.t(),
                    bind:{
                        disabled: '{!settings.autoAddressAssignment}'
                    },
                    listeners: {
                        click: 'getNewAddressSpace'
                    }
                },{
                    xtype: 'label',
                    cls: 'warningLabel',
                    hidden: true,
                    margin: '10 0 5 10'
                }
            ]
        }]
    }]
});
