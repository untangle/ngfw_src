Ext.define('Webui.config.deviceMonitor', {
    extend: 'Ung.StatusWin',
    helpSource: 'device_viewer',
    sortField:'bypassed',
    sortOrder: 'ASC',
    displayName: 'Device Viewer',
    hasReports: true,
    reportCategory: 'Device Viewer',
    defaultBandwidthColumns: false,
    enableBandwidthColumns: false,
    initComponent: function() {
        this.breadcrumbs = [{
            title: i18n._('Device Viewer')
        }];
        this.buildGridCurrentDevices();
        this.buildTabPanel([this.gridCurrentDevices]);
        this.callParent(arguments);
    },
    closeWindow: function() {
        this.gridCurrentDevices.stopAutoRefresh(true);
        this.hide();
    },
    getDevices: function(handler) {
        if (!this.isVisible()) {
            handler({javaClass:"java.util.LinkedList", list:[]});
            return;
        }
        rpc.deviceTable.getDevices(Ext.bind(function(result, exception) {
            if(testMode && result != null && result.list!=null ) {
                var testSize = 450 + Math.floor((Math.random()*100));
                for(var i=0;i<testSize;i++) {
                    var ii=i+Math.floor((Math.random()*10));
                    var d=new Date();
                    result.list.push({
                        "macAddress": "11:22:33:44:" + (ii%10) + (ii%10) + ":" + (ii%10) + (ii%10)
                    });
                }
            }
            handler(result, exception);
        }, this));
    },
    // Current Devices Grid
    buildGridCurrentDevices: function(columns, groupField) {
        var dateConvertFn = function(value) {
            if( value == 0 || value == "") {
                return " ";
            } else {
                var d=new Date();
                d.setTime(value);
                return d;
            }
        };
        this.gridCurrentDevices = Ext.create('Ung.MonitorGrid',{
            name: "deviceMonitorGrid",
            helpSource: 'device_viewer_current_devices',
            settingsCmp: this,
            height: 500,
            title: i18n._("Current Devices"),
            tooltip: i18n._("This shows all current devices."),
            dataFn: Ext.bind(this.getDevices, this),
            sortField: this.sortField,
            sortOrder: this.sortOrder,
            groupField: this.groupField,
            fields: [{
                name: "id"
            },{
                name: "macAddress",
                type: 'string',
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "macVendor",
                type: 'string',
                convert: Ung.Util.preventEmptyValueConverter
            }, {
                name: "lastSeenTimeDate",
                mapping: "lastSeenTime",
                convert: dateConvertFn
            },{
                name: "deviceUsername",
                type: 'string'
            }],
            columns: [{
                header: i18n._("MAC Address"),
                dataIndex: "macAddress",
                width: 100,
                filter: {
                    type: 'string'
                }
            }, {
                header: i18n._("MAC Vendor"),
                dataIndex: "macVendor",
                width: 150,
                filter: {
                    type: 'string'
                }
            }, {
                hidden: true,
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
            }, {
                header: i18n._("Device Username"),
                dataIndex: "deviceUsername",
                width: 150,
                filter: {
                    type: 'string'
                }
            }]
        });
    }
});
//# sourceURL=deviceMonitor.js