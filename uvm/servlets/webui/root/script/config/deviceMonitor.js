Ext.define('Webui.config.deviceMonitor', {
    extend: 'Ung.ConfigWin',
    name: 'devices',
    helpSource: 'devices',
    displayName: 'Devices',
    hasReports: true,
    reportCategory: 'Devices',
    initComponent: function() {
        this.breadcrumbs = [{
            title: i18n._('Devices')
        }];
        this.buildGridCurrentDevices();
        this.buildTabPanel([this.gridCurrentDevices]);
        this.callParent(arguments);
    },
    closeWindow: function() {
        if (this.down('tabpanel')) {
            this.down('tabpanel').setActiveItem(0);
        }
        this.hide();
        Ext.destroy(this);
    },
    // Current Devices Grid
    buildGridCurrentDevices: function() {
        var intfList = Ung.Util.getInterfaceList(false, false);
        var interfaceMap = {};
        for(var i=0; i<intfList.length; i++) {
            interfaceMap[intfList[i][0]]=intfList[i][1];
        }
        this.fieldConvertInterface = function( value, record){
            if (value == null || value < 0) {
                return '';
            }
            if (!interfaceMap[value]) {
                return Ext.String.format(i18n._('Interface [{0}]'), value);
            }
            return interfaceMap[value] + ' [' + value + ']';

        };
        var dateConvertFn = function(value) {
            if( value == 0 || value == "") {
                return " ";
            } else {
                var d=new Date();
                d.setTime(value);
                return d;
            }
        };
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
                name: "tagsString",
                type: 'string'
            },{
                name: "lastSeenInterfaceId",
                convert: this.fieldConvertInterface
            },{
                name: "deviceUsername",
                type: 'string'
            },{
                name: "httpUserAgent",
                type: 'string'
            }, {
                name: "lastSeenTime"
            }, {
                name: "lastSeenTimeDate",
                mapping: "lastSeenTime",
                convert: dateConvertFn
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
                header: i18n._("Interface"),
                dataIndex: "lastSeenInterfaceId",
                width: 100,
                filter: {
                    type: 'number'
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
                dataIndex: "lastSeenTimeDate",
                width: 150,
                renderer: function(value, metaData, record) {
                    var val=record.get("lastSeenTime");
                    return val == 0 || val == "" ? "" : i18n.timestampFormat(val);
                },
                filter: {
                    type: 'date'
                }
            },{
                header: i18n._("Tags"),
                dataIndex: "tagsString",
                width: 100,
                filter: {
                    type: 'string'
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