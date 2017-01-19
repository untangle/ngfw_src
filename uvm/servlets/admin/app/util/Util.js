Ext.define('Ung.util.Util', {
    alternateClassName: 'Ung.Util',
    singleton: true,

    // node name to class mapping
    nodeClassMapping: {
        'untangle-node-web-filter': 'Ung.node.WebFilter',
        'untangle-node-web-filter-lite': 'Ung.node.WebFilterLite',
        'untangle-node-virus-blocker': 'Ung.node.VirusBlocker',
        'untangle-node-virus-blocker-lite': 'Ung.node.VirusBlockerLite',
        'untangle-node-spam-blocker': 'Ung.node.SpamBlocker',
        'untangle-node-spam-blocker-lite': 'Ung.node.SpamBlockerLite',
        'untangle-node-phish-blocker': 'Ung.node.PhishBlocker',
        'untangle-node-web-cache': 'Ung.node.WebCache',
        'untangle-node-bandwidth-control': 'Ung.node.BandwidthControl',
        'untangle-casing-ssl-inspector': 'Ung.node.SslInspector',
        'untangle-node-application-control': 'Ung.node.ApplicationControl',
        'untangle-node-application-control-lite': 'Ung.node.ApplicationControlLite',
        'untangle-node-captive-portal': 'Ung.node.CaptivePortal',
        'untangle-node-firewall': 'Ung.node.Firewall',
        'untangle-node-ad-blocker': 'Ung.node.AdBlocker',

        'untangle-node-reports': 'Ung.node.Reports'
    },

    iconTitle: function (text, icon) {
        var icn = icon.split('-') [0],
            size = icon.split('-') [1] || 24;
        return '<i class="material-icons" style="font-size: ' + size + 'px">' +
                icn + '</i> <span style="vertical-align: middle;">' +
                text + '</span>';
    },

    iconReportTitle: function (report) {
        var icon;
        switch (report.get('type')) {
        case 'TEXT':
            icon = 'subject';
            break;
        case 'EVENT_LIST':
            icon = 'format_list_bulleted';
            break;
        case 'PIE_GRAPH':
            icon = 'pie_chart';
            if (report.get('pieStyle') === 'COLUMN' || report.get('pieStyle') === 'COLUMN_3D') {
                icon = 'insert_chart';
            } else {
                if (report.get('pieStyle') === 'DONUT' || report.get('pieStyle') === 'DONUT_3D') {
                    icon = 'donut_large';
                }
            }
            break;
        case 'TIME_GRAPH':
        case 'TIME_GRAPH_DYNAMIC':
            if (report.get('timeStyle').indexOf('BAR') >= 0) {
                icon = 'insert_chart';
            } else {
                icon = 'show_chart';
            }
            break;
        default:
            icon = 'subject';
        }
        return '<i class="material-icons" style="font-size: 18px">' + icon + '</i>';
    },

    bytesToHumanReadable: function (bytes, si) {
        var thresh = si ? 1000 : 1024;
        if(Math.abs(bytes) < thresh) {
            return bytes + ' B';
        }
        var units = si ? ['kB','MB','GB','TB','PB','EB','ZB','YB'] : ['KiB','MiB','GiB','TiB','PiB','EiB','ZiB','YiB'];
        var u = -1;
        do {
            bytes /= thresh;
            ++u;
        } while(Math.abs(bytes) >= thresh && u < units.length - 1);
        return bytes.toFixed(1)+' '+units[u];
    },

    formatBytes: function (bytes, decimals) {
        if (bytes === 0) {
            return '0';
        }
        //bytes = bytes * 1000;
        var k = 1000, // or 1024 for binary
            dm = decimals || 3,
            sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'],
            i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
    },

    successToast: function (message) {
        Ext.toast({
            html: this.iconTitle(message, 'check-20'),
            bodyPadding: '8 10',
            baseCls: 'toast',
            border: false,
            bodyBorder: false,
            align: 'br',
            autoCloseDelay: 5000,
            slideInAnimation: 'easeOut',
            slideInDuration: 300,
            hideDuration: 0,
            paddingX: 30,
            paddingY: 50
        });
    },

    exceptionToast: function (message) {
        Ext.toast({
            html: this.iconTitle('<span style="color: #FFF; font-weight: bold;">Exception!</span> ' + message, 'error-20'),
            bodyPadding: '10',
            baseCls: 'toast',
            cls: 'exception',
            border: false,
            bodyBorder: false,
            align: 'br',
            autoCloseDelay: 5000,
            slideInAnimation: 'easeOut',
            slideInDuration: 300,
            hideDuration: 0,
            paddingX: 30,
            paddingY: 50
        });
    },

    urlValidator: function (val) {
        var res = val.match(/(http(s)?:\/\/.)?(www\.)?[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,6}\b([-a-zA-Z0-9@:%_\+.~#?&//=]*)/g);
        return res ? true : 'Url missing or in wrong format!'.t();
    },

    /**
     * Helper method that lists the order in which classes are loaded
     */
    getClassOrder: function () {
        var classes = [], extClasses = [];

        Ext.Loader.history.forEach(function (cls) {
            if (cls.indexOf('Ung') === 0) {
                classes.push(cls.replace('Ung', 'app').replace(/\./g, '/') + '.js');
            } else {
                extClasses.push(cls);
            }
        });

        classes.pop();

        Ext.create('Ext.Window', {
            title: 'Untangle Classes Load Order',
            width: 600,
            height: 600,

            // Constraining will pull the Window leftwards so that it's within the parent Window
            modal: true,
            draggable: false,
            resizable: false,
            layout: {
                type: 'hbox',
                align: 'stretch',
                pack: 'end'
            },
            items: [
            // {
            //     xtype: 'component',
            //     padding: 10,
            //     html: 'Copy this list into <strong>.buildorder</strong> file!'
            // }, {
                {
                xtype: 'textarea',
                border: false,
                flex: 1,
                editable: false,
                fieldStyle: {
                    background: '#FFF',
                    fontSize: '11px'
                },
                value: classes.join('\r\n')
            }, {
                xtype: 'textarea',
                border: false,
                flex: 1,
                editable: false,
                fieldStyle: {
                    background: '#FFF',
                    fontSize: '11px'
                },
                value: extClasses.join('\r\n')
            }]
        }).show();
    },

    getInterfaceListSystemDev: function(wanMatchers, anyMatcher, systemDev) {
        var data = [];
        // var networkSettings = Ung.Main.getNetworkSettings();
        // for ( var c = 0 ; c < networkSettings.interfaces.list.length ; c++ ) {
        //     var intf = networkSettings.interfaces.list[c];
        //     var name = intf.name;
        //     var key = systemDev?intf.systemDev:intf.interfaceId;
        //     data.push( [ key, name ] );
        // }


        if (systemDev) {
            data.push( [ 'tun0', 'OpenVPN' ] );
        } else {
            data.push( [ 250, 'OpenVPN' ] ); // 0xfa
            data.push( [ 251, 'L2TP' ] ); // 0xfb
            data.push( [ 252, 'Xauth' ] ); // 0xfc
            data.push( [ 253, 'GRE' ] ); // 0xfd
        }
        if (wanMatchers) {
            data.unshift( ['wan', 'Any WAN'.t()] );
            data.unshift( ['non_wan', 'Any Non-WAN'.t()] );
        }
        if (anyMatcher) {
            data.unshift( ['any', 'Any'.t()] );
        }
        return data;
    },

    getInterfaceList: function (wanMatchers, anyMatcher) {
        return this.getInterfaceListSystemDev(wanMatchers, anyMatcher, false);
    },

});