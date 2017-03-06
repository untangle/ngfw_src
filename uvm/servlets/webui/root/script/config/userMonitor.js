Ext.define('Webui.config.userMonitor', {
    extend: 'Ung.StatusWin',
    name: 'users',
    helpSource: 'users',
    sortField:'bypassed',
    sortOrder: 'ASC',
    displayName: 'Users',
    hasReports: true,
    reportCategory: 'Users',
    defaultBandwidthColumns: false,
    enableBandwidthColumns: false,
    initComponent: function() {
        this.breadcrumbs = [{
            title: i18n._('Users')
        }];
        this.buildGridCurrentUsers();
        this.buildTabPanel([this.gridCurrentUsers]);
        this.callParent(arguments);
    },
    closeWindow: function() {
        if (this.down('tabpanel')) {
            this.down('tabpanel').setActiveItem(0);
        }
        this.gridCurrentUsers.stopAutoRefresh(true);
        this.hide();
        Ext.destroy(this);
    },
    getUsers: function(handler) {
        if (!this.isVisible()) {
            handler({javaClass:"java.util.LinkedList", list:[]});
            return;
        }
        rpc.userTable.getUsers(Ext.bind(function(result, exception) {
            if(testMode && result != null && result.list!=null ) {
                var testSize = 450 + Math.floor((Math.random()*100));
                for(var i=0;i<testSize;i++) {
                    var ii=i+Math.floor((Math.random()*10));
                    var d=new Date();
                    result.list.push({
                        "username": i%3?("foo"+i):null,
                        "lastAccessTime": 0,//d.getTime()+(i*86400000),
                        "quotaSize": ii * 10000,
                        "quotaRemaining": ii * 5000,
                        "quotaIssueTime": 0,
                        "quotaExpirationTime": 0
                    });
                }
            }
            handler(result, exception);
        }, this));
    },
    megaByteRenderer: function(bytes) {
        var units = ["bytes","Kbytes","Mbytes","Gbytes"];
        var units_itr = 0;

        while ((bytes >= 1000 || bytes <= -1000) && units_itr < 3) {
            bytes = bytes/1000;
            units_itr++;
        }

        bytes = Math.round(bytes*100)/100;

        return "" + bytes + " " + units[units_itr];
    },
    // Current Users Grid
    buildGridCurrentUsers: function(columns, groupField) {
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
        this.gridCurrentUsers = Ext.create('Ung.MonitorGrid',{
            name: "userMonitorGrid",
            helpSource: 'user_viewer_current_users',
            settingsCmp: this,
            height: 500,
            title: i18n._("Current Users"),
            tooltip: i18n._("This shows all current users."),
            dataFn: Ext.bind(this.getUsers, this),
            sortField: this.sortField,
            sortOrder: this.sortOrder,
            groupField: this.groupField,
            fields: [{
                name: "id"
            },{
                name: "username",
                type: 'string',
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "tagsString",
                type: 'string',
                convert: Ung.Util.preventEmptyValueConverter
            }, {
                name: "lastAccessTime"
            }, {
                name: "lastAccessTimeDate",
                mapping: "lastAccessTime",
                convert: dateConvertFn
            },{
                name: "quotaSize",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "quotaRemaining",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "quotaIssueTime"
            },{
                name: "quotaIssueTimeDate",
                mapping: "quotaIssueTime",
                convert: dateConvertFn
            },{
                name: "quotaExpirationTime"
            },{
                name: "quotaExpirationTimeDate",
                mapping: "quotaExpirationTime",
                convert: dateConvertFn
            }],
            columns: [{
                header: i18n._("Username"),
                dataIndex: "username",
                width: 100,
                filter: {
                    type: 'string'
                }
            }, {
                header: i18n._("Last Access Time"),
                dataIndex: "lastAccessTimeDate",
                width: 150,
                renderer: function(value, metaData, record) {
                    var val=record.get("lastAccessTime");
                    return val == 0 || val == "" ? "" : i18n.timestampFormat(val);
                },
                filter: {
                    type: 'date'
                }
            },{
                header: i18n._("Quota Size"),
                dataIndex: "quotaSize",
                width: 100,
                renderer: function(value) {
                    return value == 0 || value == "" ? "" : value;
                },
                filter: {
                    type: 'numeric'
                }
            },{
                header: i18n._("Quota Remaining"),
                dataIndex: "quotaRemaining",
                width: 100,
                filter: {
                    type: 'numeric'
                }
            },{
                header: i18n._("Quota Issue Time"),
                dataIndex: "quotaIssueTimeDate",
                width: 100,
                renderer: function(value, metaData, record) {
                    var val=record.get("quotaIssueTime");
                    return val == 0 || val == "" ? "" : i18n.timestampFormat(val);
                },
                filter: {
                    type: 'date'
                }
            },{
                header: i18n._("Quota Expiration Time"),
                dataIndex: "quotaExpirationTimeDate",
                width: 100,
                renderer: function(value, metaData, record) {
                    var val=record.get("quotaExpirationTime");
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
            }]
        });
    }
});
//# sourceURL=userMonitor.js