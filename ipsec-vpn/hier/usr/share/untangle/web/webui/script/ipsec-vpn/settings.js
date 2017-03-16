Ext.define('Webui.ipsec-vpn.settings', {
    extend:'Ung.AppWin',
    panelOptions: null,
    gridTunnels: null,
    panelVPNConfig: null,
    panelGRENetworks: null,
    pageStateInfo: null,
    pagePolicyInfo: null,
    pageLogFile: null,
    pageVirtualLog: null,
    warningDisplayed: false,
    getAppSummary: function() {
        return i18n._("IPsec VPN provides secure network access and tunneling to remote users and sites using IPsec, GRE, L2TP, Xauth, and IKEv2 protocols.");
    },
    initComponent: function() {
        try {
            this.rpc.netSettings = Ung.Main.getNetworkManager().getNetworkSettings();
            this.rpc.intStatus = Ung.Main.getNetworkManager().getInterfaceStatus();
            this.rpc.dirConLicenseValid = Ung.Main.getLicenseManager().isLicenseValid("directory-connector");
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }

        this.buildPanelOptions();
        this.buildGridTunnels();
        this.buildPanelVPNConfig();
        this.buildPanelGRENetworks();
        this.buildPageStateInfo();
        this.buildPagePolicyInfo();
        this.buildPageLogFile();
        this.buildPageVirtualLog();
        this.buildTabPanel([this.panelOptions, this.gridTunnels, this.panelVPNConfig, this.panelGRENetworks, this.pageStateInfo, this.pagePolicyInfo, this.pageLogFile, this.pageVirtualLog]);
        this.callParent(arguments);
    },

    buildStatus: function() {
        this.buildActiveTunnelsGrid();
        this.buildVirtualUsersGrid();

        this.panelStatus = Ext.create('Ung.panel.Status', {
            settingsCmp: this,
            helpSource: 'ipsec_vpn_ipsec_status',
            itemsAfterLicense: [this.gridActiveTunnels,this.gridVirtualUsers]
        });
    },

    buildActiveTunnelsGrid: function() {
        this.gridActiveTunnels = Ext.create('Ung.grid.Panel',{
            name: "gridActiveTunnels",
            margin: '0 10 20 10',
            height: 220,
            hasAdd: false,
            hasEdit: false,
            hasDelete: false,
            hasRefresh: true,
            title: i18n._("Enabled IPsec Tunnels"),
            dataFn: this.getRpcNode().getTunnelStatus,
            recordJavaClass: "com.untangle.app.ipsec_vpn.ConnectionStatusRecord",
            fields: [{
                name: "type"
            },{
                name: "src",
                sortType: 'asIp'
            },{
                name: "dst",
                sortType: 'asIp'
            },{
                name: "tmplSrc"
            },{
                name: "tmplDst"
            },{
                name: "proto"
            },{
                name: "mode"
            }],
            columns: [{
                header: i18n._("Local IP"),
                dataIndex:'src',
                width: 140
            },{
                header: i18n._("Remote Host"),
                dataIndex:'dst',
                width: 140
            },{
                header: i18n._("Local Network"),
                dataIndex:'tmplSrc',
                width: 140
            },{
                header: i18n._("Remote Network"),
                dataIndex:'tmplDst',
                width: 140
            },{
                header: i18n._("Description"),
                dataIndex:'proto',
                width: 200,
                flex: 1
            },{
                header: i18n._("Bytes In"),
                dataIndex:'inBytes',
                width: 100
            },{
                header: i18n._("Bytes Out"),
                dataIndex:'outBytes',
                width: 100
            },{
                header: i18n._("Status"),
                dataIndex: 'mode',
                renderer: Ext.bind(function(value) {
                    showtxt = i18n._("Inactive");
                    showico = "ua-cell-disabled";
                    if (value.toLowerCase() === "active") {
                        showtxt = i18n._("Active");
                        showico = "ua-cell-enabled";
                    }
                    if (value.toLowerCase() === "unknown") {
                        showtxt = i18n._("Unknown");
                    }
                    return ("<div class='" + showico + "'>" + showtxt + "</div>");
                }, this)
            }]
        });
    },

    buildVirtualUsersGrid: function() {
        var elapsedFormat = function(value) {
            var total = parseInt(value / 1000,10);
            var hours = (parseInt(total / 3600,10) % 24);
            var minutes = (parseInt(total / 60,10) % 60);
            var seconds = parseInt(total % 60,10);
            var result = (hours < 10 ? "0" + hours : hours) + ":" + (minutes < 10 ? "0" + minutes : minutes) + ":" + (seconds  < 10 ? "0" + seconds : seconds);
            return result;
        };
        this.gridVirtualUsers = Ext.create('Ung.grid.Panel',{
            name: "gridVirtualUsers",
            margin: '0 10 20 10',
            height: 220,
            hasAdd: false,
            hasEdit: false,
            hasDelete: false,
            hasRefresh: true,
            title: i18n._("Active VPN Sessions"),
            dataFn: this.getRpcNode().getVirtualUsers,
            recordJavaClass: "com.untangle.app.ipsec_vpn.VirtualUserEntry",
            fields: [{
                name: "clientAddress",
                sortType: 'asIp'
            },{
                name: "clientProtocol"
            },{
                name: "clientUsername"
            },{
                name: "netInterface"
            },{
                name: "netProcess"
            },{
                name: "sessionCreation"
            },{
                name: "sessionElapsed"
            }],
            columns: [{
                header: i18n._("IP Address"),
                dataIndex:'clientAddress',
                width: 150
            },{
                header: i18n._("Protocol"),
                dataIndex:'clientProtocol',
                width: 80
            },{
                header: i18n._("Username"),
                dataIndex:'clientUsername',
                width: 200,
                flex: 1
            },{
                header: i18n._("Interface"),
                dataIndex:'netInterface',
                width: 150
            },{
                header: i18n._("Connect Time"),
                dataIndex:'sessionCreation',
                width: 180,
                renderer: function(value) { return i18n.timestampFormat(value); }
            },{
                header: i18n._("Elapsed Time"),
                dataIndex:'sessionElapsed',
                width: 180,
                renderer: elapsedFormat
            },{
                header: i18n._("Disconnect"),
                xtype: 'actioncolumn',
                width: 80,
                items: [{
                    iconCls: 'icon-delete-row',
                    tooltip: i18n._("Click to disconnect client"),
                    handler: Ext.bind(function(view, rowIndex, colIndex, item, e, record) {
                        this.gridVirtualUsers.setLoading(i18n._("Disconnecting..."));
                        this.getRpcNode().virtualUserDisconnect(Ext.bind(function(result, exception) {
                            this.gridVirtualUsers.setLoading(false);
                            if(Ung.Util.handleException(exception)) return;
                            // it takes a second or two for the node to HUP the pppd daemon and the ip-down script
                            // to call the node to remove the client from the active user list so instead of
                            // calling reload here we just remove the disconnected row from the grid
                            this.gridVirtualUsers.getStore().remove(record);
                        }, this), record.get("clientAddress"), record.get("clientUsername"));
                    }, this)
                }]
            }]
        });
    },

    buildPanelOptions: function() {
        this.panelOptions = Ext.create('Ext.panel.Panel',{
            name: 'panelOptions',
            helpSource: 'ipsec_vpn_ipsec_options',
            title: i18n._("IPsec Options"),
            cls: 'ung-panel',
            autoScroll: true,
            trackResetOnLoad: true,
            defaults: {
                xtype: 'fieldset'
            },
            items: [{
                title: i18n._("Description"),
                html: i18n._("The IPsec Options tab contains common settings that apply to all IPsec traffic.")
            },{
                title: i18n._("Traffic Processing"),
                items: [{
                    xtype: 'checkbox',
                    fieldLabel: i18n._("Bypass all IPsec traffic"),
                    labelWidth: 220,
                    name: 'bypassflag',
                    checked: this.getSettings().bypassflag,
                    handler: Ext.bind(function(elem, checked) {
                        this.getSettings().bypassflag = checked;
                    }, this)
                }]
            }]
        });
    },

    buildGridTunnels: function() {
        var leftDefault = "0.0.0.0";
        var leftSubnetDefault = "0.0.0.0/0";
        var x;
        var networks = [];
        networks.push(['', '- ' + i18n._("Custom") + ' -']);

        // build the list of active WAN networks for the interface combo box
        for( x = 0 ; x < this.rpc.intStatus.list.length ; x++) {
            var status = this.rpc.intStatus.list[x];
            if ( ! status.v4Address ) continue;
            if ( ! status.interfaceId ) continue;
            var intf = Ung.Util.getInterface( this.rpc.netSettings, status.interfaceId );
            if ( ! intf ) continue;
            if ( ! intf.isWan ) continue;
            if ( intf.disabled ) continue;
            networks.push([ status.v4Address, intf.name]);
        }

        for( x = 0 ; x < this.rpc.netSettings.interfaces.list.length ; x++) {
            var intfSettings = this.rpc.netSettings.interfaces.list[x];
            if (intfSettings.v4StaticAddress === null) { continue; }
            if (intfSettings.v4StaticPrefix === null) { continue; }

            // save the first WAN address to use as the default for new tunnels
            if ( leftDefault == "0.0.0.0" && intfSettings.v4ConfigType == "STATIC" && intfSettings.isWan ) {
                leftDefault = intfSettings.v4StaticAddress;
            }

            // use the first non-WAN to calculate internal network defaults for new tunnels
            if ( leftSubnetDefault == "0.0.0.0/0" && intfSettings.v4ConfigType == "STATIC" && ! intfSettings.isWan ) {
                var addrval = intfSettings.v4StaticAddress;
                var prefix = intfSettings.v4StaticPrefix;
                leftSubnetDefault = (addrval + "/" + prefix);
            }
        }

        var P1CipherStore = Ext.create('Ext.data.Store', {
            fields: [ 'name', 'value' ],
            data: [
                { name: '3DES', value: '3des' },
                { name: 'AES128', value: 'aes128' },
                { name: 'AES256', value: 'aes256' },
                { name: 'Blowfish', value: 'blowfish' },
                { name: 'Twofish', value: 'twofish' },
                { name: 'Serpent', value: 'serpent' }
            ]});

        var P1HashStore = Ext.create('Ext.data.Store', {
            fields: [ 'name', 'value' ],
            data: [
                { name: 'MD5', value: 'md5' },
                { name: 'SHA-1', value: 'sha1' },
                { name: 'SHA-256', value: 'sha2_256' },
                { name: 'SHA-512', value: 'sha2_512' }
            ]});

        var P1GroupStore = Ext.create('Ext.data.Store', {
            fields: [ 'name', 'value' ],
            data: [
                { name: '2 (1024 bit)', value: 'modp1024' },
                { name: '5 (1536 bit)', value: 'modp1536' },
                { name: '14 (2048 bit)', value: 'modp2048' },
                { name: '15 (3072 bit)', value: 'modp3072' },
                { name: '16 (4096 bit)', value: 'modp4096' },
                { name: '17 (6144 bit)', value: 'modp6144' },
                { name: '18 (8192 bit)', value: 'modp8192' }
            ]});

        var P2CipherStore = Ext.create('Ext.data.Store', {
            fields: [ 'name', 'value' ],
            data: [
                { name: '3DES', value: '3des' },
                { name: 'AES128', value: 'aes128' },
                { name: 'AES256', value: 'aes256' },
                { name: 'Camellia', value: 'camellia' },
                { name: 'Blowfish', value: 'blowfish' },
                { name: 'Twofish', value: 'twofish' },
                { name: 'Serpent', value: 'serpent' }
            ]});

        var P2HashStore = Ext.create('Ext.data.Store', {
            fields: [ 'name', 'value' ],
            data: [
                { name: 'MD5', value: 'md5' },
                { name: 'SHA-1', value: 'sha1' },
                { name: 'SHA-256', value: 'sha2_256' },
                { name: 'SHA-512', value: 'sha2_512' },
                { name: 'RIPEMD', value: 'ripemd' }
            ]});

        var P2GroupStore = Ext.create('Ext.data.Store', {
            fields: [ 'name', 'value' ],
            data: [
                { name: '0 (disabled)', value: 'disabled' },
                { name: '2 (1024 bit)', value: 'modp1024' },
                { name: '5 (1536 bit)', value: 'modp1536' },
                { name: '14 (2048 bit)', value: 'modp2048' },
                { name: '15 (3072 bit)', value: 'modp3072' },
                { name: '16 (4096 bit)', value: 'modp4096' },
                { name: '17 (6144 bit)', value: 'modp6144' },
                { name: '18 (8192 bit)', value: 'modp8192' }
            ]});

        this.gridTunnels = Ext.create('Ung.grid.Panel', {
            settingsCmp: this,
            name: 'gridTunnels',
            helpSource: 'ipsec_vpn_ipsec_tunnels',
            title: i18n._("IPsec Tunnels"),
            dataProperty:'tunnels',
            recordJavaClass: 'com.untangle.app.ipsec_vpn.IpsecVpnTunnel',
            emptyRow: {
                'active': true,
                'ikeVersion': 1,
                'conntype': 'tunnel',
                'runmode': 'start',
                'dpddelay': '30',
                'dpdtimeout': '120',
                'phase1Cipher': '3des',
                'phase1Hash': 'md5',
                'phase1Group': 'modp1024',
                'phase1Lifetime' : '28800',
                'phase2Cipher': '3des',
                'phase2Hash': 'md5',
                'phase2Group': 'modp1024',
                'phase2Lifetime' : '3600',
                'left': leftDefault,
                'leftId': '',
                'leftSubnet': leftSubnetDefault,
                'right': '',
                'rightId': '',
                'rightSubnet': '',
                'description': '',
                'secret': ''
            },
            sortField: 'description',
            fields: [{
                name: 'id'
            },{
                name: 'active'
            },{
                name: 'ikeVersion'
            },{
                name: 'conntype'
            },{
                name: 'runmode'
            },{
                name: 'dpddelay'
            },{
                name: 'dpdtimeout'
            },{
                name: 'phase1Manual'
            },{
                name: 'phase1Cipher'
            },{
                name: 'phase1Hash'
            },{
                name: 'phase1Group'
            },{
                name: 'phase1Lifetime'
            },{
                name: 'phase2Manual'
            },{
                name: 'phase2Cipher'
            },{
                name: 'phase2Hash'
            },{
                name: 'phase2Group'
            },{
                name: 'phase2Lifetime'
            },{
                name: 'left',
                sortType: 'asIp'
            },{
                name: 'leftId'
            },{
                name: 'leftSubnet',
                sortType: 'asIp'
            },{
                name: 'right',
                sortType: 'asIp'
            },{
                name: 'rightId'
            },{
                name: 'rightSubnet',
                sortType: 'asIp'
            },{
                name: 'description'
            },{
                name: 'secret'
            }],
            columns: [{
                xtype: 'checkcolumn',
                header: i18n._("Enabled"),
                dataIndex: 'active',
                resizable: false,
                width: 80
            },{
                header: i18n._("Local IP"),
                width: 150,
                dataIndex: 'left'
            },{
                header: i18n._("Remote Host"),
                width: 150,
                dataIndex: 'right'
            },{
                header: i18n._("Local Network"),
                width: 200,
                dataIndex: 'leftSubnet'
            },{
                header: i18n._("Remote Network"),
                width: 200,
                dataIndex: 'rightSubnet'
            },{
                header: i18n._("Description"),
                width: 100,
                dataIndex: 'description',
                flex: 1
            }]
        });

        this.gridTunnels.setRowEditor( Ext.create('Ung.RowEditorWindow',{
            inputLines:[{
                xtype:'checkbox',
                name: 'active',
                dataIndex: 'active',
                fieldLabel: i18n._("Enable"),
                labelWidth: 120,
            },{
                xtype:'textfield',
                name: 'Description',
                dataIndex: 'description',
                fieldLabel: i18n._("Description"),
                labelWidth: 120,
                emptyText: i18n._("[enter description]"),
                allowBlank: false,
                width: 400
            },{
                xtype: 'container',
                layout: 'column',
                margin: '0 0 5 0',
                items: [{
                    xtype:'combo',
                    fieldLabel: i18n._("Connection Type"),
                    labelWidth: 120,
                    editable: false,
                    dataIndex: 'conntype',
                    store:[['tunnel','Tunnel'],['transport','Transport']]
                },{
                    xtype: 'label',
                    html: i18n._("(We recommended selecting <B>Tunnel</B> unless you have a specific reason to use <B>Transport</B>)"),
                    margin: '0 0 0 10',
                    cls: 'boxlabel'
                }]
            },{
                xtype: 'container',
                layout: 'column',
                margin: '0 0 5 0',
                items: [{
                    xtype:'combo',
                    fieldLabel: i18n._("IKE Version"),
                    labelWidth: 120,
                    editable: false,
                    name: 'ikeVersion',
                    dataIndex: 'ikeVersion',
                    store:[['1','IKEv1'],['2','IKEv2']],
                    listeners: {
                        "change": Ext.bind(function(field, newValue, oldValue, eOpts) {
                            this.gridTunnels.rowEditor.syncComponents();
                        }, this)
                    }
                },{
                    xtype: 'label',
                    html: i18n._("Both sides of the tunnel must be configured to use the same IKE version."),
                    margin: '0 0 0 10',
                    cls: 'boxlabel'
                }]
            },{
                xtype: 'container',
                layout: 'column',
                margin: '0 0 5 0',
                items: [{
                    xtype:'combo',
                    fieldLabel: i18n._("Auto Mode"),
                    labelWidth: 120,
                    dataIndex: 'runmode',
                    editable: false,
                    store:[['start','Start'],['add','Add']]
                },{
                    xtype: 'label',
                    html: i18n._("(Select <B>Start</B> for an always connected tunnel, or <B>Add</B> for an on-demand tunnel)"),
                    margin: '0 0 0 10',
                    cls: 'boxlabel'
                }]
            },{
                xtype: "combo",
                name: 'interfaceCombo',
                fieldLabel: i18n._("Interface"),
                labelWidth: 120,
                editable: false,
                queryMode: 'local',
                store: networks,
                listeners: {
                    'change': Ext.bind(function(field, newValue, oldValue, eOpts) {
                        var left = this.gridTunnels.rowEditor.down('textfield[dataIndex=left]');
                        left.setValue(newValue);
                    }, this)
                }
            },{
                xtype: 'container',
                layout: 'column',
                margin: '0 0 5 0',
                items: [{
                    xtype: "textfield",
                    dataIndex: 'left',
                    fieldLabel: i18n._("External IP"),
                    labelWidth: 120,
                    emptyText: i18n._("[enter external IP]"),
                    width: 350,
                    allowBlank: false,
                    vtype: 'ipAddress',
                    listeners: {
                        "change": Ext.bind(function(field, newValue, oldValue, eOpts) {
                            this.gridTunnels.rowEditor.syncComponents();
                        }, this)
                    }
                },{
                    xtype: 'label',
                    html: i18n._("(The external IP address of this server)"),
                    margin: '0 0 0 10',
                    cls: 'boxlabel'
                }]
            },{
                xtype: 'container',
                layout: 'column',
                margin: '0 0 5 0',
                items: [{
                    xtype:'textfield',
                    name: 'right',
                    dataIndex: 'right',
                    fieldLabel: i18n._("Remote Host"),
                    labelWidth: 120,
                    emptyText: i18n._("[enter remote host]"),
                    width: 350,
                    allowBlank: false
                },{
                    xtype: 'label',
                    html: i18n._("(The public hostname or IP address of the remote IPsec gateway)"),
                    margin: '0 0 0 10',
                    cls: 'boxlabel'
                }]
            },{
                xtype: 'container',
                layout: 'column',
                margin: '0 0 5 0',
                items: [{
                    xtype:'textfield',
                    dataIndex: 'leftId',
                    fieldLabel: i18n._("Local Identifier"),
                    labelWidth: 120,
                    emptyText: i18n._("[leave blank for default]"),
                    width: 350,
                    allowBlank: true
                },{
                    xtype: 'label',
                    html: i18n._("(The authentication ID of the local IPsec gateway. Default = same as <B>External IP</B>)"),
                    margin: '0 0 0 10',
                    cls: 'boxlabel'
                }]
            },{
                xtype: 'container',
                layout: 'column',
                margin: '0 0 5 0',
                items: [{
                    xtype:'textfield',
                    dataIndex: 'rightId',
                    fieldLabel: i18n._("Remote Identifier"),
                    labelWidth: 120,
                    emptyText: i18n._("[leave blank for default]"),
                    width: 350,
                    allowBlank: true
                },{
                    xtype: 'label',
                    html: i18n._("(The authentication ID of the remote IPsec gateway. Default = same as <B>Remote Host</B>)"),
                    margin: '0 0 0 10',
                    cls: 'boxlabel'
                }]
            },{
                xtype: 'container',
                layout: 'column',
                margin: '0 0 5 0',
                items: [{
                    xtype:'textfield',
                    name: 'leftSubnet',
                    dataIndex: 'leftSubnet',
                    fieldLabel: i18n._("Local Network"),
                    labelWidth: 120,
                    emptyText: i18n._("[enter local network]"),
                    width: 350,
                    allowBlank: false,
                    vtype: 'cidrBlock'
                },{
                    xtype: 'label',
                    html: i18n._("(The private network attached to the local side of the tunnel)"),
                    margin: '0 0 0 10',
                    cls: 'boxlabel'
                }]
            },{
                xtype: 'container',
                layout: 'column',
                margin: '0 0 5 0',
                items: [{
                    xtype:'textfield',
                    name: 'rightSubnet',
                    dataIndex: 'rightSubnet',
                    fieldLabel: i18n._("Remote Network"),
                    labelWidth: 120,
                    emptyText: i18n._("[enter remote network]"),
                    width: 350,
                    allowBlank: false,
                    vtype: 'cidrBlock'
                },{
                    xtype: 'label',
                    html: i18n._("(The private network attached to the remote side of the tunnel)"),
                    margin: '0 0 0 10',
                    cls: 'boxlabel'
                }]
            },{
                xtype:'textfield',
                name: 'secret',
                dataIndex: 'secret',
                fieldLabel: i18n._("Shared Secret"),
                labelWidth: 120,
                emptyText: i18n._("[enter shared secret]"),
                style: "font-family: monospace",
                width: 700,
                maxLength: 1000,
                allowBlank: false,
                autoCreate: { tag: 'textarea', autocomplete: 'off', spellcheck: 'false' }
            },{
                xtype: 'container',
                layout: 'column',
                margin: '0 0 5 0',
                items: [{
                    xtype:'numberfield',
                    name: 'dpddelay',
                    dataIndex: 'dpddelay',
                    fieldLabel: i18n._("DPD Interval"),
                    labelWidth: 120,
                    width: 250,
                    allowBlank: false,
                    allowDecimals: false,
                    minValue: 0,
                    maxValue: 3600
                },{
                    xtype: 'label',
                    html: i18n._("The number of seconds between R_U_THERE messages.  Enter 0 to disable."),
                    margin: '0 0 0 10',
                    cls: 'boxlabel'
                }]
            },{
                xtype: 'container',
                layout: 'column',
                margin: '0 0 5 0',
                items: [{
                    xtype:'numberfield',
                    name: 'dpdtimeout',
                    dataIndex: 'dpdtimeout',
                    fieldLabel: i18n._("DPD Timeout"),
                    labelWidth: 120,
                    width: 250,
                    allowBlank: false,
                    allowDecimals: false,
                    minValue: 0,
                    maxValue: 3600
                },{
                    xtype: 'label',
                    html: i18n._("The number of seconds for a dead peer tunnel to be restarted"),
                    margin: '0 0 0 10',
                    cls: 'boxlabel'
                }]
            },{
                xtype: 'displayfield',
                margin: '10 0 10 0',
                value: i18n._("<B>Authentication and SA/Key Exchange</B>")
            },{
                xtype: 'displayfield',
                margin: '5 0 10 0',
                value: i18n._("Usually IPsec will automatically negotiate the encryption protocol with the remote peer when creating the tunnel.") + "<br/>" +
                       i18n._("However, some peers require specific settings. To configure specific settings, enable Manual Configuration and set the appropriate values below.")
            },{
                xtype:'checkbox',
                dataIndex: 'phase1Manual',
                boxLabel: i18n._("Phase 1 IKE/ISAKMP Manual Configuration"),
                labelWidth: 250,
                margin: '20 0 10 0',
                hideLabel: true,
                handler: Ext.bind(function(elem, checked) {
                    this.gridTunnels.rowEditor.syncComponents();
                }, this)
            },{
                xtype: 'container',
                layout: 'column',
                margin: '0 0 5 50',
                name: 'phase1CipherContainer',
                items: [{
                    xtype:'combobox',
                    name: 'phase1Cipher',
                    dataIndex: 'phase1Cipher',
                    fieldLabel: i18n._("Encryption"),
                    labelWidth: 120,
                    width: 250,
                    editable: false,
                    queryMode: 'local',
                    store: P1CipherStore,
                    displayField: 'name',
                    valueField: 'value'
                },{
                    xtype: 'label',
                    html: i18n._("Default = 3DES"),
                    margin: '0 0 0 10',
                    cls: 'boxlabel'
                }]
            },{
                xtype: 'container',
                layout: 'column',
                margin: '0 0 5 50',
                name: 'phase1HashContainer',
                items: [{
                    xtype:'combobox',
                    name: 'phase1Hash',
                    dataIndex: 'phase1Hash',
                    fieldLabel: i18n._("Hash"),
                    labelWidth: 120,
                    width: 250,
                    editable: false,
                    queryMode: 'local',
                    store: P1HashStore,
                    displayField: 'name',
                    valueField: 'value'
                },{
                    xtype: 'label',
                    html: i18n._("Default = MD5"),
                    margin: '0 0 0 10',
                    cls: 'boxlabel'
                }]
            },{
                xtype: 'container',
                layout: 'column',
                margin: '0 0 5 50',
                name: 'phase1GroupContainer',
                items: [{
                    xtype:'combobox',
                    name: 'phase1Group',
                    dataIndex: 'phase1Group',
                    fieldLabel: i18n._("DH Key Group"),
                    labelWidth: 120,
                    width: 250,
                    editable: false,
                    queryMode: 'local',
                    store: P1GroupStore,
                    displayField: 'name',
                    valueField: 'value'
                },{
                    xtype: 'label',
                    html: i18n._("Default = 2 (1024 bit)"),
                    margin: '0 0 0 10',
                    cls: 'boxlabel'
                }]
            },{
                xtype: 'container',
                layout: 'column',
                margin: '0 0 5 50',
                name: 'phase1LifetimeContainer',
                items: [{
                    xtype:'numberfield',
                    name: 'phase1Lifetime',
                    dataIndex: 'phase1Lifetime',
                    fieldLabel: i18n._("Lifetime"),
                    labelWidth: 120,
                    width: 250,
                    allowBlank: false,
                    allowDecimals: false,
                    minValue: 3600,
                    maxValue: 86400
                },{
                    xtype: 'label',
                    html: i18n._("Default = 28800 seconds, min = 3600, max = 86400"),
                    margin: '0 0 0 10',
                    cls: 'boxlabel'
                }]
            },{
                xtype:'checkbox',
                dataIndex: 'phase2Manual',
                boxLabel: i18n._("Phase 2 ESP Manual Configuration"),
                labelWidth: 250,
                margin: '20 0 10 0',
                hideLabel: true,
                handler: Ext.bind(function(elem, checked) {
                    this.gridTunnels.rowEditor.syncComponents();
                }, this)
            },{
                xtype: 'container',
                layout: 'column',
                margin: '0 0 5 50',
                name: 'phase2CipherContainer',
                items: [{
                    xtype:'combobox',
                    name: 'phase2Cipher',
                    dataIndex: 'phase2Cipher',
                    fieldLabel: i18n._("Encryption"),
                    labelWidth: 120,
                    width: 250,
                    editable: false,
                    queryMode: 'local',
                    store: P2CipherStore,
                    displayField: 'name',
                    valueField: 'value'
                },{
                    xtype: 'label',
                    html: i18n._("Default = 3DES"),
                    margin: '0 0 0 10',
                    cls: 'boxlabel'
                }]
            },{
                xtype: 'container',
                layout: 'column',
                margin: '0 0 5 50',
                name: 'phase2HashContainer',
                items: [{
                    xtype:'combobox',
                    name: 'phase2Hash',
                    dataIndex: 'phase2Hash',
                    fieldLabel: i18n._("Hash"),
                    labelWidth: 120,
                    width: 250,
                    editable: false,
                    queryMode: 'local',
                    store: P2HashStore,
                    displayField: 'name',
                    valueField: 'value'
                },{
                    xtype: 'label',
                    html: i18n._("Default = MD5"),
                    margin: '0 0 0 10',
                    cls: 'boxlabel'
                }]
            },{
                xtype: 'container',
                layout: 'column',
                margin: '0 0 5 50',
                name: 'phase2GroupContainer',
                items: [{
                    xtype:'combobox',
                    name: 'phase2Group',
                    dataIndex: 'phase2Group',
                    fieldLabel: i18n._("PFS Key Group"),
                    labelWidth: 120,
                    width: 250,
                    editable: false,
                    queryMode: 'local',
                    store: P2GroupStore,
                    displayField: 'name',
                    valueField: 'value'
                },{
                    xtype: 'label',
                    html: i18n._("Default = 2 (1024 bit)"),
                    margin: '0 0 0 10',
                    cls: 'boxlabel'
                }]
            },{
                xtype: 'container',
                layout: 'column',
                margin: '0 0 5 50',
                name: 'phase2LifetimeContainer',
                items: [{
                    xtype:'numberfield',
                    name: 'phase2Lifetime',
                    dataIndex: 'phase2Lifetime',
                    fieldLabel: i18n._("Lifetime"),
                    labelWidth: 120,
                    width: 250,
                    allowBlank: false,
                    allowDecimals: false,
                    minValue: 3600,
                    maxValue: 86400
                },{
                    xtype: 'label',
                    html: i18n._("Default = 3600 seconds, min = 3600, max = 86400"),
                    margin: '0 0 0 10',
                    cls: 'boxlabel'
                }]
            }],
            syncComponents: function() {
                if(!this.cmps) {
                    this.cmps = {
                        phase1Manual: this.down('checkbox[dataIndex=phase1Manual]'),
                        phase1CipherContainer: this.down('container[name=phase1CipherContainer]'),
                        phase1HashContainer: this.down('container[name=phase1HashContainer]'),
                        phase1GroupContainer: this.down('container[name=phase1GroupContainer]'),
                        phase1LifetimeContainer: this.down('container[name=phase1LifetimeContainer]'),

                        phase2Manual: this.down('checkbox[dataIndex=phase2Manual]'),
                        phase2CipherContainer: this.down('container[name=phase2CipherContainer]'),
                        phase2HashContainer: this.down('container[name=phase2HashContainer]'),
                        phase2GroupContainer: this.down('container[name=phase2GroupContainer]'),
                        phase2LifetimeContainer: this.down('container[name=phase2LifetimeContainer]'),

                        interfaceCombo: this.down('combo[name=interfaceCombo]'),
                        left: this.down('textfield[dataIndex=left]')
                    };
                }
                var leftValue = this.cmps.left.getValue();

                this.cmps.interfaceCombo.suspendEvent("change");
                this.cmps.interfaceCombo.setValue('');

                for(var x = 0; x < networks.length;x++) {
                    if (networks[x][0] === leftValue) {
                        this.cmps.interfaceCombo.setValue(leftValue);
                        break;
                    }
                }

                this.cmps.interfaceCombo.resumeEvent("change");

                var phase1Manual = this.cmps.phase1Manual.getValue();
                this.cmps.phase1CipherContainer.setVisible(phase1Manual);
                this.cmps.phase1HashContainer.setVisible(phase1Manual);
                this.cmps.phase1GroupContainer.setVisible(phase1Manual);
                this.cmps.phase1LifetimeContainer.setVisible(phase1Manual);

                var phase2Manual = this.cmps.phase2Manual.getValue();
                this.cmps.phase2CipherContainer.setVisible(phase2Manual);
                this.cmps.phase2HashContainer.setVisible(phase2Manual);
                this.cmps.phase2GroupContainer.setVisible(phase2Manual);
                this.cmps.phase2LifetimeContainer.setVisible(phase2Manual);
            }
        }));
    },

    buildPanelVPNConfig: function() {
        this.buildListenGrid();

        var onUpdateRadioButton = Ext.bind(function( elem, checked ) {
            if ( checked ) {
                this.getSettings().authenticationType = elem.inputValue;
            }
        }, this);

        var onRenderRadioButton = Ext.bind(function( elem ) {
            elem.setValue(this.getSettings().authenticationType);
            elem.clearDirty();
        }, this);

        this.panelVPNConfig = Ext.create('Ext.panel.Panel', {
            settingsCmp: this,
            name: 'panelVPNConfig',
            helpSource: 'ipsec_vpn_l2tp_options',
            title: i18n._("VPN Config"),
            cls: 'ung-panel',
            autoScroll: true,
            reserveScrollbar: true,
            layout: { type: 'vbox', pack: 'start', align: 'stretch' },
            defaults: {
                xtype: 'fieldset'
            },
            items: [{
                title: i18n._("Description"),
                html: i18n._("The VPN Config tab contains settings used to configure the server to support IPsec L2TP, Xauth, and IKEv2 VPN client connections.")
            },{
                title: i18n._("Server Configuration"),
                labelWidth: 300,
                defaults: {
                    labelWidth: 160
                },
                items: [{
                    xtype: 'checkbox',
                    fieldLabel: i18n._("Enable L2TP/Xauth/IKEv2 Server"),
                    labelWidth: 240,
                    padding: '3 0 3 0',
                    name: 'vpnflag',
                    checked: this.getSettings().vpnflag,
                    handler: Ext.bind(function(elem, checked) {
                        this.getSettings().vpnflag = checked;
                    }, this)
                },{
                    xtype: 'textfield',
                    name: 'virtualAddressPool',
                    width: 300,
                    padding: '3 0 3 0',
                    dataIndex: 'virtualAddressPool',
                    fieldLabel: i18n._("L2TP Address Pool"),
                    value: this.getSettings().virtualAddressPool,
                    vtype: 'cidrBlock',
                    allowBlank: false,
                    listeners: {
                        "change": Ext.bind(function( elem, newValue ) {
                            this.getSettings().virtualAddressPool = newValue;
                        }, this)
                    }
                },{
                    xtype: 'textfield',
                    name: 'virtualXauthPool',
                    width: 300,
                    padding: '3 0 3 0',
                    dataIndex: 'virtualXauthPool',
                    fieldLabel: i18n._("Xauth/IKEv2 Address Pool"),
                    value: this.getSettings().virtualXauthPool,
                    vtype: 'cidrBlock',
                    allowBlank: false,
                    listeners: {
                        "change": Ext.bind(function( elem, newValue ) {
                            this.getSettings().virtualXauthPool = newValue;
                        }, this)
                    }
                },{
                    xtype: 'container',
                    layout: 'column',
                    items: [{
                        xtype: 'textfield',
                        name: 'virtualDnsOne',
                        labelWidth: 160,
                        width: 300,
                        padding: '3 0 3 0',
                        dataIndex: 'virtualDnsOne',
                        fieldLabel: i18n._("Custom DNS Server 1"),
                        value: this.getSettings().virtualDnsOne,
                        vtype: 'ipAddress',
                        allowBlank: true,
                        listeners: {
                            "change": Ext.bind(function( elem, newValue ) {
                                this.getSettings().virtualDnsOne = newValue;
                            }, this)
                        }
                    },{
                        xtype: 'textfield',
                        name: 'virtualDnsTwo',
                        labelWidth: 160,
                        width: 300,
                        padding: '3 0 3 20',
                        dataIndex: 'virtualDnsTwo',
                        fieldLabel: i18n._("Custom DNS Server 2"),
                        value: this.getSettings().virtualDnsTwo,
                        vtype: 'ipAddress',
                        allowBlank: true,
                        listeners: {
                            "change": Ext.bind(function( elem, newValue ) {
                                this.getSettings().virtualDnsTwo = newValue;
                            }, this)
                        }
                    }]
                },{
                    xtype: 'displayfield',
                    padding: '10 10 10 10',
                    value:  i18n._("<STRONG>NOTE:</STRONG> Leave the Custom DNS fields empty to have clients automatically configured to use this server for DNS resolution.")
                },{
                    xtype: 'textfield',
                    name: 'virtualSecret',
                    allowBlank: false,
                    width: 500,
                    padding: '0 0 0 0',
                    dataIndex: 'virtualSecret',
                    fieldLabel: i18n._("IPsec Secret"),
                    value: this.getSettings().virtualSecret,
                    listeners: {
                        "change": Ext.bind(function( elem, newValue ) {
                            this.getSettings().virtualSecret = newValue;
                        }, this)
                    }
                }]
            },{
                title: i18n._( "User Authentication" ),
                labelWidth: 300,
                items: [{
                    xtype: 'displayfield',
                    value: i18n._("In addition to the IPsec Secret configured above, VPN clients must authenticate with a valid username and password.  User authentication can be configured to use either the internal local directory, or an external RADIUS server configured in the Directory Connector application.")
                },{
                    xtype: "radio",
                    boxLabel: i18n._("Local Directory"),
                    hideLabel: true,
                    padding: '10 0 0 0',
                    name: "authenticationType",
                    inputValue: "LOCAL_DIRECTORY",
                    listeners: {
                        "change": onUpdateRadioButton,
                        "afterrender": onRenderRadioButton
                    }
                },{
                    xtype: "button",
                    name: "configureLocalDirectory",
                    text: i18n._("Configure Local Directory"),
                    handler: Ext.bind(this.configureLocalDirectory, this )
                },{
                    xtype: "radio",
                    boxLabel: Ext.String.format( i18n._("RADIUS {0}(requires Directory Connector) {1}"),"<i>", "</i>" ),
                    hideLabel: true,
                    padding: '10 0 0 0',
                    disabled: !this.rpc.dirConLicenseValid,
                    name: "authenticationType",
                    inputValue: "RADIUS_SERVER",
                    listeners: {
                        "change": onUpdateRadioButton,
                        "afterrender": onRenderRadioButton
                    }
                },{
                    xtype: "button",
                    disabled: !this.rpc.dirConLicenseValid,
                    name: "configureRadiusServer",
                    text: i18n._("Configure RADIUS"),
                    handler: Ext.bind(this.configureRadius, this )
                }]
            },{
                title: i18n._( "Server Listen Addresses" ),
                labelWidth: 300,
                items: [{
                    xtype: 'component',
                    margin: '0 0 10 0',
                    html:  i18n._("This is the list of IP addresses where the server will listen for incomming VPN connections. If you have a single WAN connection, you will normally only add that single address to this list.  If you have multiple WAN interfaces, you can configure the server to listen on any, or all of them.")
                }, this.gridListenList ]
            }]
        });
    },

    buildPanelGRENetworks: function() {
        var localDefault = "0.0.0.0";
        var x;
        var networks = [];
        networks.push(['', '- ' + i18n._("Custom") + ' -']);

        // build the list of active WAN networks for the interface combo box
        for( x = 0 ; x < this.rpc.intStatus.list.length ; x++) {
            var status = this.rpc.intStatus.list[x];
            if ( ! status.v4Address ) continue;
            if ( ! status.interfaceId ) continue;
            var intf = Ung.Util.getInterface( this.rpc.netSettings, status.interfaceId );
            if ( ! intf ) continue;
            if ( ! intf.isWan ) continue;
            if ( intf.disabled ) continue;
            networks.push([ status.v4Address, intf.name]);
        }

        for( x = 0 ; x < this.rpc.netSettings.interfaces.list.length ; x++) {
            var intfSettings = this.rpc.netSettings.interfaces.list[x];
            if (intfSettings.v4StaticAddress === null) { continue; }
            if (intfSettings.v4StaticPrefix === null) { continue; }

            // save the first WAN address to use as the default for new tunnels
            if ( localDefault == "0.0.0.0" && intfSettings.v4ConfigType == "STATIC" && intfSettings.isWan ) {
               localDefault = intfSettings.v4StaticAddress;
            }
        }

        this.gridNetworks = Ext.create('Ung.grid.Panel', {
            settingsCmp: this,
            name: 'gridNetworks',
            title: i18n._('Remote Networks'),
            height: 600,
            dataProperty:'networks',
            recordJavaClass: 'com.untangle.app.ipsec_vpn.IpsecVpnNetwork',
            emptyRow: {
                'active': true,
                'localAddress': localDefault,
                'remoteAddress': '',
                'remoteNetworks': '',
                'description': ''
            },
            sortField: 'description',
            fields: [{
                name: 'id'
            },{
                name: 'active'
            },{
                name: 'localAddress',
                sortType: 'asIp'
            },{
                name: 'remoteAddress',
                sortType: 'asIp'
            },{
                name: 'remoteNetworks'
            },{
                name: 'description'
            }],
            columns: [{
                xtype: 'checkcolumn',
                header: i18n._("Enabled"),
                dataIndex: 'active',
                resizable: false,
                width: 80
            },{
                header: i18n._("Local IP"),
                width: 150,
                dataIndex: 'localAddress'
            },{
                header: i18n._("Remote Host"),
                width: 150,
                dataIndex: 'remoteAddress'
            },{
                header: i18n._("Remote Networks"),
                width: 200,
                dataIndex: 'remoteNetworks'
            },{
                header: i18n._("Description"),
                width: 100,
                dataIndex: 'description',
                flex: 1
            }]
        });

        this.gridNetworks.setRowEditor( Ext.create('Ung.RowEditorWindow',{
            inputLines:[{
                xtype:'checkbox',
                name: 'active',
                dataIndex: 'active',
                fieldLabel: i18n._("Enable")
            },{
                xtype:'textfield',
                name: 'Description',
                dataIndex: 'description',
                fieldLabel: i18n._("Description"),
                emptyText: i18n._("[enter description]"),
                allowBlank: false,
                width: 400
            },{
                xtype: "combo",
                name: 'interfaceCombo',
                fieldLabel: i18n._("Interface"),
                editable: false,
                queryMode: 'local',
                store: networks,
                listeners: {
                    'change': Ext.bind(function(field, newValue, oldValue, eOpts) {
                        var left = this.gridNetworks.rowEditor.down('textfield[dataIndex=localAddress]');
                        left.setValue(newValue);
                    }, this)
                }
            },{
                xtype: 'container',
                layout: 'column',
                margin: '0 0 5 0',
                items: [{
                    xtype: "textfield",
                    dataIndex: 'localAddress',
                    fieldLabel: i18n._("External IP"),
                    emptyText: i18n._("[enter external IP]"),
                    width: 350,
                    allowBlank: false,
                    vtype: 'ipAddress',
                    listeners: {
                        "change": Ext.bind(function(field, newValue, oldValue, eOpts) {
                            this.gridNetworks.rowEditor.syncComponents();
                        }, this)
                    }
                },{
                    xtype: 'label',
                    html: i18n._("The external IP address of this server"),
                    cls: 'boxlabel'
                }]
            },{
                xtype: 'container',
                layout: 'column',
                margin: '0 0 5 0',
                items: [{
                    xtype:'textfield',
                    name: 'right',
                    dataIndex: 'remoteAddress',
                    fieldLabel: i18n._("Remote Host"),
                    emptyText: i18n._("[enter remote host]"),
                    width: 350,
                    allowBlank: false,
                    vtype: 'ipAddress'
                },{
                    xtype: 'label',
                    html: i18n._("The public IP address of the remote GRE gateway"),
                    cls: 'boxlabel'
                }]
            },{
                xtype: 'container',
                layout: 'column',
                margin: '0 0 5 0',
                items: [{
                    xtype:'textarea',
                    name: 'remoteNetworks',
                    dataIndex: 'remoteNetworks',
                    fieldLabel: i18n._("Remote Networks"),
                    emptyText: i18n._("[enter remote networks]"),
                    width: 300,
                    height: 300,
                    allowBlank: false,
                    vtype: 'cidrBlockArea'
                },{
                    xtype: 'label',
                    html: i18n._("The private networks attached to the remote GRE gateway<br>One network per line in CIDR (192.168.123.0/24) format"),
                    cls: 'boxlabel'
                }]
            }],
            syncComponents: function() {
                if(!this.cmps) {
                    this.cmps = {
                        interfaceCombo: this.down('combo[name=interfaceCombo]'),
                        localAddress: this.down('textfield[dataIndex=localAddress]')
                    };
                }
                var localValue = this.cmps.localAddress.getValue();
                this.cmps.interfaceCombo.suspendEvent("change");
                this.cmps.interfaceCombo.setValue('');

                for(var x = 0; x < networks.length;x++) {
                    if (networks[x][0] === localValue) {
                        this.cmps.interfaceCombo.setValue(localValue);
                        break;
                    }
                }
                this.cmps.interfaceCombo.resumeEvent("change");
            }
        }));

        this.panelGRENetworks = Ext.create('Ext.panel.Panel',{
            name: 'panelGRENetworks',
            helpSource: 'ipsec_vpn_gre_networks',
            title: i18n._("GRE Networks"),
            settingsCmp: this,
            cls: 'ung-panel',
            autoScroll: true,
            trackResetOnLoad: true,
            defaults: {
                xtype: 'fieldset'
            },
            items: [{
                title: i18n._("Description"),
                html: i18n._("The GRE Networks tab contains configuration options for connecting this server to other servers and networks using the GRE protocol.")
            },{
                title: i18n._("Server Configuration"),
                items: [{
                    xtype: 'textfield',
                    name: 'virtualNetworkPool',
                    width: 300,
                    padding: '3 0 3 0',
                    dataIndex: 'virtualNetworkPool',
                    fieldLabel: i18n._("GRE Address Pool"),
                    labelWidth: 150,
                    allowBlank: false,
                    value: this.getSettings().virtualNetworkPool,
                    vtype: 'cidrBlock',
                    listeners: {
                        "change": Ext.bind(function( elem, newValue ) {
                            this.getSettings().virtualNetworkPool = newValue;
                        }, this)
                    }
                },{
                    xtype: 'displayfield',
                    padding: '10 10 10 10',
                    value:  i18n._("Each Remote Network will have a corresponding GRE interface created on this server, with each interface being assigned an IP address from this pool.")
                }]
            }, this.gridNetworks ]
        });
    },

    buildListenGrid: function() {
        this.gridListenList = Ext.create('Ung.grid.Panel',{
            name: 'gridListen',
            settingsCmp: this,
            width: 300,
            height: 200,
            hasEdit: false,
            hasImportExport: false,
            dataProperty: 'virtualListenList',
            addHandler: Ext.bind(this.showInterfacePicker, this),
            recordJavaClass: 'com.untangle.app.ipsec_vpn.VirtualListen',
            emptyRow: {
                "address": "1.2.3.4"
            },
            fields: [{
                name: 'address'
            }],
            columns: [{
                header: i18n._("Address"),
                width: 200,
                name: 'address',
                dataIndex: 'address',
                sortable: true,
                flex: 1,
                editor: {
                    xtype: 'textfield',
                    emptyText: i18n._("[enter IP address]"),
                    vtype: 'ip4Address',
                    allowBlank: false
                }
            }]
        });
    },

    showInterfacePicker: function(button, e) {
        if(!this.menuInterfacePicker) {
            this.menuInterfacePicker = Ext.create('Ext.menu.Menu', {
                floating: true,
                plain: true,
                listeners: {
                    'click': Ext.bind(function(menu, item, e, opts) {
                        if (! item) return;
                        var record = Ext.create(Ext.ClassManager.getName(this.gridListenList.getStore().getProxy().getModel()), Ext.decode(Ext.encode(this.gridListenList.emptyRow)));
                        record.set({
                            "address": item.ipaddr,
                            "internalId": this.gridListenList.genAddedId()
                        });
                        this.gridListenList.getStore().add([record]);
                        this.gridListenList.updateChangedData(record, "added");
                    }, this)
                }
            });

            this.menuInterfacePicker.add([{ text: i18n._('Manual Address Input'), ipaddr: '1.2.3.4' }]);
            var x, status, intf, disabled;
            for( x = 0 ; x < this.rpc.intStatus.list.length ; x++) {
                status = this.rpc.intStatus.list[x];
                if ( !status.v4Address  || !status.interfaceId) continue;
                intf = Ung.Util.getInterface( this.rpc.netSettings, status.interfaceId );
                if ( !intf || !intf.isWan || intf.disabled) continue;

                this.menuInterfacePicker.add([{ text: (intf.name + " - " + status.v4Address), ipaddr: status.v4Address}]);
            }
            this.subCmps.push(this.menuInterfacePicker);
        }
        var me = this;
        var virtualListenList = me.gridListenList.getList();
        Ext.Array.each(this.menuInterfacePicker.items.items, function(item, index, menuItems) {
            if(index > 0) {
                item.setDisabled(me.searchItem(item.ipaddr, virtualListenList));
            }
        });
        this.menuInterfacePicker.showAt(button.getXY());
    },

    searchItem: function(needle, haystack) {
        for(i = 0; i < haystack.length; i++) {
            if(haystack[i].address === needle)
                return true;
        }
        return false;
    },
    configureLocalDirectory: function() {
        Ung.Main.openConfig(Ung.Main.configMap["localDirectory"]);
    },
    configureRadius: function() {
        var node = Ung.Main.getNode("directory-connector");
        if (node != null) {
            var nodeCmp = Ung.Node.getCmp(node.nodeId);
            if (nodeCmp != null) {
                Ung.Main.target="node.directory-connector.RADIUS Connector";
                nodeCmp.loadSettings();
            }
        }
    },
    buildInfoPanel: function(config) {
        return Ext.create('Ext.panel.Panel', {
            helpSource: config.helpSource,
            layout: 'fit',
            title: config.title,
            items: [{
                xtype: 'component',
                padding: 0,
                border: false,
                data: {},
                tpl: '<textarea style="width: 100%; height: 100%; border: 0; resize: none;" readonly>{log}</textarea>'
            }],
            bbar: [{
                xtype: 'button',
                text: '<i class="material-icons" style="font-size: 20px;">refresh</i> <span style="vertical-align: middle;">' + config.text + '</span>',
                handler: function(button) {
                    button.up('panel').loadInfo();
                }
            }],
            isDirty: function() {
                return false;
            },
            loadInfo: function() {
                var logger = this.down('component');
                logger.update({ log: i18n._("Loading information... Please wait...") });
                config.dataFn(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    logger.update({ log: result });
                });
            },
            listeners: {
                'activate': function(panel) {
                    panel.loadInfo();
                }
            }
        });
    },
    buildPageStateInfo: function() {
        this.pageStateInfo = this.buildInfoPanel({
            helpSource: 'ipsec_vpn_ipsec_state',
            title: i18n._("IPsec State"),
            text: i18n._("Refresh"),
            dataFn: this.getRpcNode().getStateInfo
        });
    },
    buildPagePolicyInfo: function() {
        this.pagePolicyInfo = this.buildInfoPanel({
            helpSource: 'ipsec_vpn_ipsec_policy',
            title: i18n._("IPsec Policy"),
            text: i18n._("Refresh"),
            dataFn: this.getRpcNode().getPolicyInfo
        });
    },
    buildPageLogFile: function() {
        this.pageLogFile = this.buildInfoPanel({
            helpSource: 'ipsec_vpn_ipsec_log',
            title: i18n._("IPsec Log"),
            text: i18n._("Refresh"),
            dataFn: this.getRpcNode().getLogFile
        });
    },
    buildPageVirtualLog: function() {
        this.pageVirtualLog = this.buildInfoPanel({
            helpSource: 'ipsec_vpn_l2tp_log',
            title: i18n._("L2TP Log"),
            text: i18n._("Refresh"),
            dataFn: this.getRpcNode().getVirtualLogFile
        });
    },
    beforeSave: function(isApply, handler) {
        this.getSettings().tunnels.list = this.gridTunnels.getList();
        this.getSettings().networks.list = this.gridNetworks.getList();
        this.getSettings().virtualListenList.list = this.gridListenList.getList();
        handler.call(this, isApply);
    },
    validate: function(isApply) {
        var garbage = 0;

        chk = this.panelVPNConfig.down('textfield[dataIndex=virtualAddressPool]');
        if (chk.isValid() === false) garbage++;

        chk = this.panelVPNConfig.down('textfield[dataIndex=virtualXauthPool]');
        if (chk.isValid() === false) garbage++;

        chk = this.panelVPNConfig.down('textfield[dataIndex=virtualSecret]');
        if (chk.isValid() === false) garbage++;

        chk = this.panelGRENetworks.down('textfield[dataIndex=virtualNetworkPool]');
        if (chk.isValid() === false) garbage++;

        if (garbage != 0)
        {
            Ext.MessageBox.alert(i18n._('IPsec Configuration Warning'), i18n._("One or more fields have empty or invalid values. Check the VPN Config and GRE Networks tabs for missing or incorrect data."));
            return(false);
        }

        if (this.warningDisplayed == true) return(true);
        var tlist = this.gridTunnels.getList();
        var regexp = /^[0-9\.]+$/;
        var problem = false;

            for(i=0; i<tlist.length; i++)
            {
                checker = tlist[i].right.match(regexp);
                if (checker != null) continue;
                problem = true;
            }

        if (problem == false) return(true);

        Ext.MessageBox.alert(i18n._('IPsec Configuration Warning'), i18n._("One or more IPsec tunnels is configured with a hostname in the <B>Remote Host</B> field.  The use of IP addresses instead of hostname for the remote host is strongly recommended."));
        this.warningDisplayed = true;
        return(false);
    }
});
//# sourceURL=ipsec-vpn-settings.js
