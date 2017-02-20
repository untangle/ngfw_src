Ext.define('Ung.util.Util', {
    alternateClassName: 'Util',
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

    iconTitle: function (text, icon) {
        var icn = icon.split('-') [0],
            size = icon.split('-') [1] || 24;
        return '<i class="material-icons" style="font-size: ' + size + 'px">' +
                icn + '</i> <span style="vertical-align: middle;">' +
                text + '</span>';
    },

    successToast: function (message) {
        Ext.toast({
            html: '<i class="fa fa-check fa-lg"></i> ' + message,
            // minWidth: 200,
            bodyPadding: '12 12 12 40',
            baseCls: 'toast',
            border: false,
            bodyBorder: false,
            // align: 'b',
            align: 'br',
            autoCloseDelay: 5000,
            slideInAnimation: 'easeOut',
            slideInDuration: 300,
            hideDuration: 0,
            paddingX: 10,
            paddingY: 50
        });
    },

    exceptionToast: function (message) {
        var msg = [];
        if (typeof message === 'object') {
            if (message.name && message.code) {
                msg.push('<strong>Name:</strong> ' + message.name + ' (' + message.code + ')');
            }
            if (message.message) {
                msg.push('<strong>Error:</strong> ' + message.message);
            }
        } else {
            msg = [message];
        }
        Ext.toast({
            html: '<i class="fa fa-exclamation-triangle fa-lg"></i> <span style="font-weight: bold; color: yellow;">Exception!</span><br/>' + msg.join('<br/>'),
            bodyPadding: '10 10 10 45',
            baseCls: 'toast',
            cls: 'exception',
            border: false,
            bodyBorder: false,
            align: 'br',
            autoCloseDelay: 5000,
            slideInAnimation: 'easeOut',
            slideInDuration: 300,
            hideDuration: 0,
            paddingX: 10,
            paddingY: 50
        });
    },

    invalidFormToast: function (fields) {
        if (!fields || fields.length === 0) {
            return;
        }

        var str = [];
        fields.forEach(function (field) {
            str.push('<span class="field-name">' + field.label + '</span>: <br/> <span class="field-error">' + field.error.replace(/<\/?[^>]+(>|$)/g, '') + '</span>');
        });

        // var store = [];
        // fields.forEach(function (field) {
        //     console.log(field);
        //     store.push({ label: field.getFieldLabel(), error: field.getActiveError().replace(/<\/?[^>]+(>|$)/g, ''), field: field });
        // });

        Ext.toast({
            html: '<i class="fa fa-exclamation-triangle fa-lg"></i> <span style="font-weight: bold; font-size: 14px; color: yellow;">Check invalid fields!</span><br/><br/>' + str.join('<br/>'),
            bodyPadding: '10 10 10 45',
            baseCls: 'toast-invalid-frm',
            border: false,
            bodyBorder: false,
            align: 'br',
            autoCloseDelay: 5000,
            slideInAnimation: 'easeOut',
            slideInDuration: 300,
            hideDuration: 0,
            paddingX: 10,
            paddingY: 50
            // items: [{
            //     xtype: 'dataview',
            //     store: {
            //         data: store
            //     },
            //     tpl:     '<tpl for=".">' +
            //         '<div style="margin-bottom: 10px;">' +
            //         '<span class="field-name">{label}</span>:' +
            //         '<br/><span>{error}</span>' +
            //         '</div>' +
            //     '</tpl>',
            //     itemSelector: 'div',
            //     listeners: {
            //         select: function (el, field) {
            //             field.get('field').focus();
            //         }
            //     }
            // }]
        });
    },

    getInterfaceListSystemDev: function (wanMatchers, anyMatcher, systemDev) {
        var networkSettings = rpc.networkSettings,
            data = [], intf, i;

        // Note: using strings as keys instead of numbers, needed for the checkboxgroup column widget component to function

        for (i = 0; i < networkSettings.interfaces.list.length; i += 1) {
            intf = networkSettings.interfaces.list[i];
            data.push([systemDev ? intf.systemDev.toString() : intf.interfaceId.toString(), intf.name]);
        }

        if (systemDev) {
            data.push(['tun0', 'OpenVPN']);
        } else {
            data.push(['250', 'OpenVPN']); // 0xfa
            data.push(['251', 'L2TP']); // 0xfb
            data.push(['252', 'Xauth']); // 0xfc
            data.push(['253', 'GRE']); // 0xfd
        }
        if (wanMatchers) {
            data.unshift(['wan', 'Any WAN'.t()]);
            data.unshift(['non_wan', 'Any Non-WAN'.t()]);
        }
        if (anyMatcher) {
            data.unshift(['any', 'Any'.t()]);
        }
        return data;
    },
    getInterfaceList: function (wanMatchers, anyMatcher) {
        return Util.getInterfaceListSystemDev(wanMatchers, anyMatcher, false);
    },

    // used for render purposes
    interfacesListNamesMap: function () {
        var map = {
            'wan': 'Any WAN'.t(),
            'non_wan': 'Any Non-WAN'.t(),
            'any': 'Any'.t(),
            '250': 'OpenVPN',
            '251': 'L2TP',
            '252': 'Xauth',
            '253': 'GRE'
        };
        var i, intf;

        for (i = 0; i < rpc.networkSettings.interfaces.list.length; i += 1) {
            intf = rpc.networkSettings.interfaces.list[i];
            map[intf.systemDev] = intf.name;
            map[intf.interfaceId] = intf.name;
        }
        return map;
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

    getV4NetmaskList: function(includeNull) {
        var data = [];
        if (includeNull) {
            data.push( [null,'\u00a0'] );
        }
        data.push( [32,'/32 - 255.255.255.255'] );
        data.push( [31,'/31 - 255.255.255.254'] );
        data.push( [30,'/30 - 255.255.255.252'] );
        data.push( [29,'/29 - 255.255.255.248'] );
        data.push( [28,'/28 - 255.255.255.240'] );
        data.push( [27,'/27 - 255.255.255.224'] );
        data.push( [26,'/26 - 255.255.255.192'] );
        data.push( [25,'/25 - 255.255.255.128'] );
        data.push( [24,'/24 - 255.255.255.0'] );
        data.push( [23,'/23 - 255.255.254.0'] );
        data.push( [22,'/22 - 255.255.252.0'] );
        data.push( [21,'/21 - 255.255.248.0'] );
        data.push( [20,'/20 - 255.255.240.0'] );
        data.push( [19,'/19 - 255.255.224.0'] );
        data.push( [18,'/18 - 255.255.192.0'] );
        data.push( [17,'/17 - 255.255.128.0'] );
        data.push( [16,'/16 - 255.255.0.0'] );
        data.push( [15,'/15 - 255.254.0.0'] );
        data.push( [14,'/14 - 255.252.0.0'] );
        data.push( [13,'/13 - 255.248.0.0'] );
        data.push( [12,'/12 - 255.240.0.0'] );
        data.push( [11,'/11 - 255.224.0.0'] );
        data.push( [10,'/10 - 255.192.0.0'] );
        data.push( [9,'/9 - 255.128.0.0'] );
        data.push( [8,'/8 - 255.0.0.0'] );
        data.push( [7,'/7 - 254.0.0.0'] );
        data.push( [6,'/6 - 252.0.0.0'] );
        data.push( [5,'/5 - 248.0.0.0'] );
        data.push( [4,'/4 - 240.0.0.0'] );
        data.push( [3,'/3 - 224.0.0.0'] );
        data.push( [2,'/2 - 192.0.0.0'] );
        data.push( [1,'/1 - 128.0.0.0'] );
        data.push( [0,'/0 - 0.0.0.0'] );

        return data;
    },

    validateForms: function (view) {
        var invalidFields = [];

        view.query('form[withValidation]').forEach(function (form) {
            if (form.isDirty()) {
                form.query('field{isValid()==false}').forEach(function (field) {
                    invalidFields.push({ label: field.getFieldLabel(), error: field.getActiveError() });
                    // invalidFields.push(field);
                });
            }
        });

        if (invalidFields.length > 0) {
            Util.invalidFormToast(invalidFields);
            return false;
        }
        return true;
    }


});
