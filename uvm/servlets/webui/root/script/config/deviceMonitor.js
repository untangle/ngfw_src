Ext.define('Webui.config.deviceMonitor', {
    extend: 'Ung.ConfigWin',
    helpSource: 'device_list',
    displayName: 'Device List',
    hasReports: true,
    reportCategory: 'Device List',
    initComponent: function() {
        this.breadcrumbs = [{
            title: i18n._('Device List')
        }];
        this.buildGridCurrentDevices();
        this.buildTabPanel([this.gridCurrentDevices]);
        this.callParent(arguments);
    },
    closeWindow: function() {
        this.hide();
    },
    // Current Devices Grid
    buildGridCurrentDevices: function() {
        this.gridCurrentDevices = Ext.create('Ung.grid.Panel',{
            helpSource: 'device_list_current_devices',
            settingsCmp: this,
            hasRefresh: true,
            title: i18n._("Current Devices"),
            qtip: i18n._("This shows all current devices."),
            dataFn: Ext.bind(rpc.deviceTable.getDevices, this),
            recordJavaClass: "com.untangle.uvm.DeviceTableEntry",
            emptyRow: {
                "macAddress": "",
                "lastSeenTime": 0
            },
            fields: [{
                name: "macAddress",
                type: 'string'
            },{
                name: "macVendor",
                type: 'string'
            },{
                name: "hostname",
                type: 'string'
            },{
                name: "deviceUsername",
                type: 'string'
            },{
                name: "httpUserAgent",
                type: 'string'
            }, {
                name: "lastSeenTime",
                sortType: 'asTimestamp'
            }],
            columns: [{
                header: i18n._("MAC Address"),
                dataIndex: "macAddress",
                width: 120,
                filter: {
                    type: 'string'
                }
            }, {
                header: i18n._("MAC Vendor"),
                dataIndex: "macVendor",
                width: 190,
                filter: {
                    type: 'string'
                },
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[no MAC Vendor]")
                }
            }, {
                header: i18n._("Hostname"),
                dataIndex: "hostname",
                width: 120,
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[no hostname]")
                }
            }, {
                header: i18n._("Device Username"),
                dataIndex: "deviceUsername",
                width: 150,
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[no device username]")
                }
            }, {
                header: "HTTP" + " - " + i18n._("User Agent"),
                dataIndex: "httpUserAgent",
                width: 200,
                flex: 1,
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[no HTTP user agent]")
                }
            }, {
                header: i18n._("Last Seen Time"),
                dataIndex: "lastSeenTime",
                width: 150,
                renderer: function(value) {
                    return value == 0 || value == "" ? "" : i18n.timestampFormat(value);
                }
            }],
            reload: function() {
                if(this.isDirty()) {
                    Ext.MessageBox.confirm(i18n._("Warning"),
                        i18n._("This will clear the current changes.") + "<br/><br/>" +
                        i18n._("Do you want to continue anyway?"),
                        Ext.bind(function(btn, text) {
                            if (btn == 'yes') {
                                this.clearDirty();
                            }
                        }, this)
                    );
                }
                
            }
        });
        this.gridCurrentDevices.setRowEditor( Ext.create('Ung.RowEditorWindow',{
            rowEditorLabelWidth: 150,
            inputLines: [{
                xtype:'textfield',
                dataIndex: "macAddress",
                fieldLabel: i18n._("MAC Address"),
                emptyText: i18n._("[enter MAC address]"),
                allowBlank:false,
                vtype:"macAddress",
                maskRe: /[a-fA-F0-9:]/
            }, {
                xtype:'textfield',
                dataIndex: "macVendor",
                fieldLabel: i18n._("MAC Vendor"),
                emptyText: i18n._("[no MAC Vendor]"),
                width: 500
            }, {
                xtype:'textfield',
                dataIndex: "hostname",
                fieldLabel: i18n._("Hostname"),
                emptyText: i18n._("[no hostname]"),
                width: 500
            }, {
                xtype:'textfield',
                dataIndex: "deviceUsername",
                fieldLabel: i18n._("Device Username"),
                emptyText: i18n._("[no device username]"),
                width: 500
            }, {
                xtype:'textfield',
                dataIndex: "httpUserAgent",
                fieldLabel: "HTTP" + " - " + i18n._("User Agent"),
                emptyText: i18n._("[no HTTP user agent]"),
                width: 500
            }],
            syncComponents: function () {
                this.down('textfield[dataIndex=macAddress]').setDisabled(this.record.get("internalId")>=0);
            }
        }));
    },
    save: function(isApply) {
        Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
        var deviceList = this.gridCurrentDevices.getList();
        rpc.deviceTable.setDevices(Ext.bind(function(result, exception) {
            Ext.MessageBox.hide();
            if(Ung.Util.handleException(exception)) return;
            if (!isApply) {
                this.closeWindow();
            } else {
                this.clearDirty();
            }
        }, this), { javaClass:"java.util.LinkedList", list: deviceList });

    }
});
//# sourceURL=deviceMonitor.js