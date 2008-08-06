if (!Ung.hasResource["Ung.OpenVPN"]) {
    Ung.hasResource["Ung.OpenVPN"] = true;
    Ung.Settings.registerClassName('untangle-node-openvpn', "Ung.OpenVPN");

    Ung.OpenVPN = Ext.extend(Ung.Settings, {
    	panelStatus: null,
    	statusLabel: null,
        // called when the component is rendered
        onRender : function(container, position) {
            // call superclass renderer first
            Ung.OpenVPN.superclass.onRender.call(this, container, position);
            this.getRpcNode().getConfigState(function(result, exception) {
                Ext.MessageBox.hide();
                if (exception) {
                    Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                    return;
                }
                var configState=result;
                this.statusLabel="";
                if(configState=="UNCONFIGURED") {
                    this.statusLabel="Unconfigured: Use buttons below.";
                } else if(configState=="CLIENT") {
                    this.statusLabel="VPN Client: Connected to " + this.formatHostAddress(this.getVpnServerAddress());
                } else if(configState=="SERVER_ROUTE") {
                    this.statusLabel="VPN Server";
                }
                this.buildStatus();
                var tabs=[this.panelStatus];
                
                this.buildTabPanel(tabs);
                
            }.createDelegate(this))
        },
        getVpnServerAddress : function(forceReload) {
        	if (forceReload || this.rpc.vpnServerAddress === undefined) {
                this.rpc.vpnServerAddress = this.getRpcNode().getVpnServerAddress();
            }
            return this.rpc.vpnServerAddress;
        },
        formatHostAddress : function (hostAddress) {
        	return hostAddress==null?"":(hostAddress.hostName==null || hostAddress.hostName=="")?hostAddress.hostName :hostAddress.ip
        },
        // Block lists panel
        buildStatus : function() {
            this.panelStatus = new Ext.Panel({
                name : 'Status',
                title : this.i18n._("Status"),
                parentId : this.getId(),

                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    title : this.i18n._('Current Mode'),
                    items : [{
                        xtype : "textfield",
                        name : "Status",
                        readOnly : true,
                        fieldLabel : this.i18n._("Status"),
                        width : 250,
                        value: this.statusLabel
                    }]
                }, {
                    title : this.i18n._('Wizard'),
                    layout:'table',
                    layoutConfig: {
                        columns: 2
                    },
                    items : [{
                        xtype: "button",
                        name : 'Configure as VPN Server',
                        text : this.i18n._("Configure as VPN Server"),
                        handler : function() {
                            //TODO
                        }.createDelegate(this)
                    },{
                        html: this.i18n._("This configures OpenVPN so remote users and networks can connect and access exported hosts and networks."),
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        border : false
                    }, {
                        xtype: "button",
                        name : 'Configure as VPN Client',
                        text : this.i18n._("Configure as VPN Client"),
                        handler : function() {
                            //TODO
                        }.createDelegate(this)
                    },{
                        html: this.i18n._("This configures OpenVPN so it connects to a remote OpenVPN Server and can access exported hosts and networks."),
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        border : false
                    }]
                }]
            });
        },

        // save function
        save : function() {
            // validate first
            if (this.validate()) {
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
//                this.getRpcNode().updateAll(function(result, exception) {
//                    Ext.MessageBox.hide();
//                    if (exception) {
//                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
//                        return;
//                    }
//                    // exit settings screen
//                    this.cancelAction();
//                }.createDelegate(this), this.getBaseSettings());
            }
        }
    });
}
