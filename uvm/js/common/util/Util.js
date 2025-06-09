Ext.define('Ung.util.Util', {
    alternateClassName: 'Util',
    singleton: true,
    ignoreExceptions: false,

    // defaultColors: ['#7cb5ec', '#434348', '#90ed7d', '#f7a35c', '#8085e9', '#f15c80', '#e4d354', '#2b908f', '#f45b5b', '#91e8e1'],
    defaultColors: ['#00b000', '#3030ff', '#009090', '#00ffff', '#707070', '#b000b0', '#fff000', '#b00000', '#ff0000', '#ff6347', '#c0c0c0'], // from old UI

    baseCategories: [
        { name: 'hosts', type: 'system', displayName: 'Hosts', viewPosition: 1 },
        { name: 'devices', type: 'system', displayName: 'Devices', viewPosition: 2 },
        { name: 'users', type: 'system', displayName: 'Users', viewPosition: 3 },
        { name: 'network', type: 'system', displayName: 'Network', viewPosition: 4 },
        { name: 'administration', type: 'system', displayName: 'Administration', viewPosition: 5 },
        { name: 'events', type: 'system', displayName: 'Events', viewPosition: 6 },
        { name: 'system', type: 'system', displayName: 'System',  viewPosition: 7 },
        { name: 'shield', type: 'system', displayName: 'Shield', viewPosition: 8 }
    ],

    appStorage: { },

    appDescription: {
        'web-filter': 'Web Filter scans and categorizes web traffic to monitor and enforce network usage policies.'.t(),
        'web-monitor': 'Web monitor scans and categorizes web traffic to monitor and enforce network usage policies.'.t(),
        'virus-blocker': 'Virus Blocker detects and blocks malware before it reaches users desktops or mailboxes.'.t(),
        'virus-blocker-lite': 'Virus Blocker Lite detects and blocks malware before it reaches users desktops or mailboxes.'.t(),
        'spam-blocker': 'Spam Blocker detects, blocks, and quarantines spam before it reaches users mailboxes.'.t(),
        'spam-blocker-lite': 'Spam Blocker Lite detects, blocks, and quarantines spam before it reaches users mailboxes.'.t(),
        'phish-blocker': 'Phish Blocker detects and blocks phishing emails using signatures.'.t(),
        'web-cache': 'Web Cache stores and serves web content from local cache for increased speed and reduced bandwidth usage.'.t(),
        'bandwidth-control': 'Bandwidth Control monitors, manages, and shapes bandwidth usage on the network'.t(),
        'ssl-inspector': 'SSL Inspector allows for full decryption of HTTPS and SMTPS so that other applications can process the encrypted streams.'.t(),
        'application-control': 'Application Control scans sessions and identifies the associated applications allowing each to be flagged and/or blocked.'.t(),
        'application-control-lite': 'Application Control Lite identifies, logs, and blocks sessions based on the session content using custom signatures.'.t(),
        'captive-portal': 'Captive Portal allows administrators to require network users to complete a defined process, such as logging in or accepting a network usage policy, before accessing the internet.'.t(),
        'firewall': 'Firewall is a simple application that flags and blocks sessions based on rules.'.t(),
        'threat-prevention': 'Threat Prevention flags and blocks sessions based on rules that match IP address and URL historical statistics.'.t(),
        'ad-blocker': 'Ad Blocker blocks advertising content and tracking cookies for scanned web traffic.'.t(),
        'reports': 'Reports records network events to provide administrators the visibility and data necessary to investigate network activity.'.t(),
        'policy-manager': 'Policy Manager enables administrators to create different policies and handle different sessions with different policies based on rules.'.t(),
        'directory-connector': 'Directory Connector allows integration with external directories and services, such as Active Directory, RADIUS, or Google.'.t(),
        'wan-failover': 'WAN Failover detects WAN outages and re-routes traffic to any other available WANs to maximize network uptime.'.t(),
        'wan-balancer': 'WAN Balancer spreads network traffic across multiple internet connections for better performance.'.t(),
        'ipsec-vpn': 'IPsec VPN provides secure network access and tunneling to remote users and sites using IPsec, GRE, L2TP, Xauth, and IKEv2 protocols.'.t(),
        'wireguard-vpn': 'WireGuard VPN provides secure network access and tunneling to remote users and sites using the WireGuard VPN protocol.'.t(),
        'openvpn': 'OpenVPN provides secure network access and tunneling to remote users and sites using the OpenVPN protocol.'.t(),
        'tunnel-vpn': 'Tunnel VPN provides connectivity through encrypted tunnels to remote VPN servers and services.'.t(),
        'dynamic-blocklists': 'Dynamic Blocklists provides blocking on added urls which have IPS.'.t(),
        'intrusion-prevention': 'Intrusion Prevention blocks scans, detects, and blocks attacks and suspicious traffic using signatures.'.t(),
        'configuration-backup': 'Configuration Backup automatically creates backups of settings uploads them to My Account and Google Drive.'.t(),
        'branding-manager': 'The Branding Settings are used to set the logo and contact information that will be seen by users (e.g. reports).'.t(),
        'live-support': 'Live Support provides on-demand help for any technical issues.'.t()
    },

    // adds timezone computation to ensure dates showing in UI are showing actual server date
    serverToClientDate: function (serverDate) {
        if (!serverDate) { return null; }
        return Ext.Date.add(serverDate, Ext.Date.MINUTE, new Date().getTimezoneOffset() + rpc.timeZoneOffset / 60000);
    },

    // extracts the timezone computation from UI dates before requesting new data from server
    clientToServerDate: function (clientDate) {
        if (!clientDate) { return null; }
        return Ext.Date.subtract(clientDate, Ext.Date.MINUTE, new Date().getTimezoneOffset() + rpc.timeZoneOffset / 60000);
    },

    getDecryptedPassword: function(encryptedPassword){ 
        return rpc.systemManager.getDecryptedPassword(encryptedPassword);
    },

    // returns milliseconds depending of the servlet ADMIN or REPORTS
    getMilliseconds: function () {
        // UvmContext
        if(Rpc.exists('rpc.systemManager')){
            return Rpc.directData('rpc.systemManager.getMilliseconds');
        }
        // ReportsContext
        if(Rpc.exists('rpc.ReportsContext')){
            return Rpc.directData('rpc.ReportsContext.getMilliseconds');
        }
        // otherwise return client millis
        return new Date().getTime();
    },

    humanReadableMap: {
        'P': 1125899906842624,
        'T': 1099511627776,
        'G': 1073741824,
        'M': 1048576,
        'K': 1024
    },
    regexHumanReadable: /([-+]?[0-9]*\.?[0-9]+)\s*(.)/,
    humanReadabletoBytes: function(value){
        var bytes = 0;
        if(this.regexHumanReadable.test(value)){
            var matches = this.regexHumanReadable.exec(value);
            matches[2] = matches[2].toUpperCase();
            if(matches[2] in this.humanReadableMap){
                bytes = parseFloat(matches[1]) * this.humanReadableMap[matches[2].toUpperCase()];
            }
        }
        return bytes;
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

    bytesRenderer: function(bytes, perSecond) {
        var units = (!perSecond) ? ['bytes'.t(), 'Kbytes'.t(), 'Mbytes'.t(), 'Gbytes'.t(), 'Tbytes'.t(), 'Pbytes'.t(), 'Ebytes'.t(), 'Zbytes'.t(), 'Ybytes'.t()] :
            ['bytes/s'.t(), 'Kbytes/s'.t(), 'Mbytes/s'.t(), 'Gbytes/s'.t(), 'Tbytes/s'.t(), 'Pbytes/s'.t(), 'Ebytes/s'.t(), 'Zbytes/s'.t(), 'Ybytes/s'.t()];
        var units_itr = 0;
        while ((bytes >= 1000 || bytes <= -1000) && units_itr < 8) {
            bytes = bytes/1000;
            units_itr++;
        }
        bytes = Math.round(bytes*100)/100;
        return bytes + ' ' + units[units_itr];
    },
    bytesRendererCompact: function(bytes) {
        var units = ['', 'K', 'M', 'G'];
        var units_itr = 0;
        while ((bytes >= 1000 || bytes <= -1000) && units_itr < 3) {
            bytes = bytes/1000;
            units_itr++;
        }
        bytes = Math.round(bytes*100)/100;
        return bytes + ' ' + units[units_itr];
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

    showWarningMessage:function(message, details, errorHandler) {
        var wnd = Ext.create('Ext.window.Window', {
            title: 'Warning'.t(),
            modal:true,
            closable:false,
            layout: "fit",
            setSizeToRack: function () {
                if(Ung.Main && Ung.Main.viewport) {
                    var objSize = Ung.Main.viewport.getSize();
                    objSize.height = objSize.height - 66;
                    this.setPosition(0, 66);
                    this.setSize(objSize);
                } else {
                    this.maximize();
                }
            },
            doSize: function() {
                var detailsComp = this.down('fieldset[name="details"]');
                if(!detailsComp.isHidden()) {
                    this.setSizeToRack();
                } else {
                    this.center();
                }
            },
            items: {
                xtype: "panel",
                minWidth: 350,
                autoScroll: true,
                defaults: {
                    border: false
                },
                items: [{
                    xtype: "fieldset",
                    padding: 10,
                    items: [{
                        xtype: "label",
                        html: message,
                    }]
                }, {
                    xtype: "fieldset",
                    hidden: (typeof(interactiveMode) != "undefined" && interactiveMode == false),
                    items: [{
                        xtype: "button",
                        name: "details_button",
                        text: "Show details".t(),
                        hidden: details==null,
                        handler: function() {
                            var detailsComp = wnd.down('fieldset[name="details"]');
                            var detailsButton = wnd.down('button[name="details_button"]');
                            if(detailsComp.isHidden()) {
                                wnd.initialHeight = wnd.getHeight();
                                wnd.initialWidth = wnd.getWidth();
                                detailsComp.show();
                                detailsButton.setText('Hide details'.t());
                                wnd.setSizeToRack();
                            } else {
                                detailsComp.hide();
                                detailsButton.setText('Show details'.t());
                                wnd.restore();
                                wnd.setHeight(wnd.initialHeight);
                                wnd.setWidth(wnd.initialWidth);
                                wnd.center();
                            }
                        },
                        scope : this
                    }]
                }, {
                    xtype: "fieldset",
                    name: "details",
                    hidden: true,
                    html: details!=null ? details : ''
                }]
            },
            buttons: [{
                text: 'OK'.t(),
                hidden: (typeof(interactiveMode) != "undefined" && interactiveMode == false),
                handler: function() {
                    if ( errorHandler) {
                        errorHandler();
                    } else {
                        wnd.close();
                    }
                }
            }]
        });
        wnd.show();
        if(Ext.MessageBox.rendered) {
            Ext.MessageBox.hide();
        }
    },

    goToStartPage: function () {
        Ext.MessageBox.wait("Redirecting to the start page...".t(), "Please wait".t());
        location.reload();
    },

    isServerConnectionLost: function (exception) {
        return exception.code == 550 || exception.code == 12029 || exception.code == 12019 || exception.code == 0 ||
        /* handle connection lost (this happens on windows only for some reason) */
        (exception.name == 'JSONRpcClientException' && exception.fileName != null && exception.fileName.indexOf('jsonrpc') != -1) ||
        /* special text for "method not found" and "Service Temporarily Unavailable" */
        (exception.message && exception.message.indexOf('method not found') != -1) ||
        (exception.message && exception.message.indexOf('Service Unavailable') != -1) ||
        (exception.message && exception.message.indexOf('Service Temporarily Unavailable') != -1) ||
        (exception.message && exception.message.indexOf('This application is not currently available') != -1);
    },

    handleException: function (exception) {
        if (Util.ignoreExceptions)
            return;

        var message = null;
        var details = "";

        if ( !exception ) {
            console.error("Null Exception!");
            return;
        } else {
            console.error(exception);
            if( typeof exception == "object" ){
                if(Rpc.exists('rpc.UvmContext')){
                    // This is the best way to log exceptions.  Sending through the Rpc object
                    // would require a special case to not show the exception again.
                    rpc.UvmContext.logJavascriptException(function (result, ex) {}, JSON.parse(JSON.stringify(exception, Object.getOwnPropertyNames(exception))));
                }
            }
        }

        if ( exception.javaStack )
            exception.name = exception.javaStack.split('\n')[0]; //override poor jsonrpc.js naming
        if ( exception.name )
            details += "<b>" + "Exception name".t() +":</b> " + exception.name + "<br/><br/>";
        if ( exception.code )
            details += "<b>" + "Exception code".t() +":</b> " + exception.code + "<br/><br/>";
        if ( exception.message )
            details += "<b>" + "Exception message".t() + ":</b> " + exception.message.replace(/\n/g, '<br/>') + "<br/><br/>";
        if ( exception.javaStack )
            details += "<b>" + "Exception java stack".t() +":</b> " + exception.javaStack.replace(/\n/g, '<br/>') + "<br/><br/>";
        if ( exception.stack )
            details += "<b>" + "Exception js stack".t() +":</b> " + exception.stack.replace(/\n/g, '<br/>') + "<br/><br/>";
        if ( rpc.fullVersionAndRevision != null )
            details += "<b>" + "Build".t() +":&nbsp;</b>" + rpc.fullVersionAndRevision + "<br/><br/>";
        details +="<b>" + "Timestamp".t() +":&nbsp;</b>" + (new Date()).toString() + "<br/><br/>";
        if ( exception.response )
            details += "<b>" + "Exception response".t() +":</b> " + Ext.util.Format.stripTags(exception.response).replace(/\s+/g,'<br/>') + "<br/><br/>";

        /* handle authorization lost */
        if( exception.response && exception.response.includes("loginPage") ) {
            message  = "Session timed out.".t() + "<br/>";
            message += "Press OK to return to the login page.".t() + "<br/>";
            Util.ignoreExceptions = true;
            Util.showWarningMessage(message, details, Util.goToStartPage);
            return;
        }

        /* handle connection lost */
        if( Util.isServerConnectionLost(exception)) {
            message  = "The connection to the server has been lost.".t() + "<br/>";
            message += "Press OK to return to the login page.".t() + "<br/>";
            Util.ignoreExceptions = true;
            Util.showWarningMessage(message, details, Util.goToStartPage);
            return;
        }

        Util.exceptionToast(exception);
    },

    exceptionToast: function (ex) {
        var msg = [];
        if (typeof ex === 'object') {
            if (ex.name && ex.code) {
                msg.push('<strong>Name:</strong> ' + ex.name + ' (' + ex.code + ')');
            }
            if (ex.ex) {
                msg.push('<strong>Error:</strong> ' + ex.ex);
            }
            if (ex.message) {
                msg.push('<strong>Message:</strong> ' + ex.message);
            }
        } else {
            msg = [ex];
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

    // This is called a lot of times when initializing condition sets for rules.
    // Previously we loaded network settings for each call.  Now we do it once and
    // only refresh after 30 seconds since the last build.
    interfaceList: null,
    interfaceLastUpdated: null,
    interfaceMaxAge: 30 * 1000,
    getInterfaceList: function (wanMatchers, anyMatcher) {

        var currentTime = new Date().getTime();
        if (this.interfaceList === null ||
            this.interfaceLastUpdated === null ||
            ( ( this.interfaceLastUpdated + this.interfaceMaxAge ) < currentTime ) ){
            this.interfaceLastUpdated = currentTime;
            var networkSettings = Rpc.directData('rpc.networkSettings'),
                data = [];

            // Note: using strings as keys instead of numbers, needed for the checkboxgroup column widget component to function
            networkSettings.interfaces.list.forEach( function(intf){
                data.push([intf.interfaceId.toString(), intf.name]);
            });
            networkSettings.virtualInterfaces.list.forEach( function(intf){
                data.push([intf.interfaceId.toString(), intf.name]);
            });
            data.push(['ipsec', "IPsec VPN".t()]);
            this.interfaceList = data;
        }
        var interfaces = Ext.clone(this.interfaceList);

        if (wanMatchers) {
            interfaces.unshift(['wan', 'Any WAN'.t()]);
            interfaces.unshift(['non_wan', 'Any Non-WAN'.t()]);
        }
        if (anyMatcher) {
            interfaces.unshift(['any', 'Any'.t()]);
        }
        return interfaces;
    },

    /**
     * Method to get list of LAN Interface IP's
     * @return List of LAN IP's from network settings based on onlyTcpUdp flag value.
     */
    lanIpList: null,
    lanIpLastUpdated: null,
    lanIpMaxAge: 30 * 1000,
    getLanIpAddrs: function() {
        var currentTime = new Date().getTime();
        
        if (this.lanIpList === null ||
            this.lanIpLastUpdated === null ||
            ((this.lanIpLastUpdated + this.lanIpMaxAge) < currentTime)){
                this.lanIpLastUpdated = currentTime;
                var networkSettings = Rpc.directData('rpc.networkSettings'),
                    data = [];

                networkSettings.interfaces.list.forEach( function(intf){
                    if(!intf.isWan && intf.v4StaticAddress) {
                        data.push(intf.v4StaticAddress);
                    }
                });
                networkSettings.virtualInterfaces.list.forEach( function(intf){
                    if(!intf.isWan && intf.v4StaticAddress) {
                        data.push(intf.v4StaticAddress);
                    }
                });
                this.lanIpList = data;
        }
        var lanIps = Ext.clone(this.lanIpList);
        return lanIps;
    },

    bytesToMBs: function(value) {
        return Math.round(value/10000)/100;
    },

    urlValidator: function (val) {
        var res = val.match(/(http(s)?:\/\/.)?(www\.)?[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,63}\b([-a-zA-Z0-9@:%_\+.~#?&//=]*)/g);
        return res ? true : 'Url missing or in wrong format!'.t();
    },

    ipValidator: function (val) {
        var res = val.match(/^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/);
        return res ? true : 'Url missing or in wrong format!'.t();
    },

    networkValidator: function (value){
        return value == '0.0.0.0/0' ? "Invalid Network".t() : true;
    },

    urlIpValidator: function (val) {
        if (Util.urlValidator(val) === true) {
            return true;
        } else {
            if (Util.ipValidator(val) === true) {
                return true;
            }
        }
        return 'Url missing or in wrong format!'.t();
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
            items: [{
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

    getV4NetmaskList: function(includeNull, excludeZero) {
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
        if(!excludeZero) {
            data.push( [0,'/0 - 0.0.0.0'] );
        }

        return data;
    },

    getV4NetmaskMap: function() {
        var map = {}, data = this.getV4NetmaskList();
        data.forEach(function(element) {
            map[element[0]] = element[1].split('-')[1].trim();
        });
        return map;
    },

    validateForms: function (view) {
        var invalidFields = [];

        view.query('[withValidation=true]').forEach(function (form) {
            form.query('field').forEach(function (field) {
                if(field.isHidden()){
                    return;
                }
                if(field.up('*{isHidden()==true}')){
                    return;
                }
                if(field.up().tab && field.up().tab.isHidden() == true ){
                    return;
                }
                if(field.initialConfig.bind && field.$hasBinds == undefined){
                    return;
                }
                if( field.isValid() == false){
                    invalidFields.push({ label: field.getFieldLabel(), error: field.getActiveError() });
                }
            });
        });

        if (invalidFields.length > 0) {
            Util.invalidFormToast(invalidFields);
            return false;
        }
        return true;
    },

    urlValidator2: function (url) {
        if (url.match(/^([^:]+):\/\// ) !== null) {
            return 'Site cannot contain URL protocol.'.t();
        }
        if (url.match(/^([^:]+):\d+\// ) !== null) {
            return 'Site cannot contain port.'.t();
        }
        // strip "www." from beginning of rule
        if (url.indexOf('www.') === 0) {
            url = url.substr(4);
        }
        // strip "*." from beginning of rule
        if (url.indexOf('*.') === 0) {
            url = url.substr(2);
        }
        // strip "/" from the end
        if (url.indexOf('/') === url.length - 1) {
            url = url.substring(0, url.length - 1);
        }
        if (url.trim().length === 0) {
            return 'Invalid URL specified'.t();
        }
        return true;
    },

    // formats a timestamp - expects a timestamp integer or an onject literal with 'time' property
    timestampFormat: function(v) {
        if (!v || typeof v === 'string') {
            return 0;
        }
        var date = new Date();
        if (typeof v === 'object' && v.time) {
            date.setTime(v.time);
        } else {
            date.setTime(v);
        }
        return Ext.util.Format.date(date, 'timestamp_fmt'.t());
    },

    // formats a timestamp as a date
    dateFormat: function(v) {
        if (!v || typeof v === 'string') {
            return 0;
        }
        var date = new Date();
        if (typeof v === 'object' && v.time) {
            date.setTime(v.time);
        } else {
            date.setTime(v);
        }
        return Ext.util.Format.date(date, 'date_fmt'.t());
    },

    getStoreUrl: function(){
        // non API store URL used for links like: My Account, Forgot Password
        return Rpc.directData('rpc.storeUrl').replace('/api/v1', '/store/open.php');
    },

    about: null,
    getAbout: function (forceReload) {
        if (this.about === null) {
            this.about = [
                'uid=' + Rpc.directData('rpc.serverUID'),
                "version=" + Rpc.directData('rpc.fullVersion'),
                "webui=true",
                "lang=" + Rpc.directData('rpc.languageSettings.language'),
                "applianceModel=" + Rpc.directData('rpc.applianceModel'),
                "installType=" + Rpc.directData('rpc.installType')
            ].join('&');
        }
        return this.about;
    },

    weekdaysMap: {
        '1': 'Sunday'.t(),
        '2': 'Monday'.t(),
        '3': 'Tuesday'.t(),
        '4': 'Wednesday'.t(),
        '5': 'Thursday'.t(),
        '6': 'Friday'.t(),
        '7': 'Saturday'.t()
    },

    keyStr : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",

    base64encode: function(input) {
        if (typeof(base64encode) === 'function') {
            return base64encode(input);
        }
        var output = "";
        var chr1, chr2, chr3, enc1, enc2, enc3, enc4;
        var i = 0;
        input = Util.utf8Encode(input);
        while (i < input.length) {
            chr1 = input.charCodeAt(i++);
            chr2 = input.charCodeAt(i++);
            chr3 = input.charCodeAt(i++);
            enc1 = chr1 >> 2;
            enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
            enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
            enc4 = chr3 & 63;
            if (isNaN(chr2)) {
                enc3 = enc4 = 64;
            } else if (isNaN(chr3)) {
                enc4 = 64;
            }
            output = output +
            Util.keyStr.charAt(enc1) + Util.keyStr.charAt(enc2) +
            Util.keyStr.charAt(enc3) + Util.keyStr.charAt(enc4);
        }
        return output;
    },

    utf8Encode : function (string) {
        string = string.replace(/\r\n/g,"\n");
        var utftext = "";
        for (var n = 0; n < string.length; n++) {
            var c = string.charCodeAt(n);
            if (c < 128) {
                utftext += String.fromCharCode(c);
            }
            else if((c > 127) && (c < 2048)) {
                utftext += String.fromCharCode((c >> 6) | 192);
                utftext += String.fromCharCode((c & 63) | 128);
            }
            else {
                utftext += String.fromCharCode((c >> 12) | 224);
                utftext += String.fromCharCode(((c >> 6) & 63) | 128);
                utftext += String.fromCharCode((c & 63) | 128);
            }
        }
        return utftext;
    },

    getLicenseMessage: function (license) {
        var message = '';
        if (!license) {
            return message;
        }
        if (license.trial) {
            if(license.expired) {
                message = 'Free trial expired!'.t();
            } else if (license.daysRemaining < 2) {
                message = 'Free trial.'.t() + ' ' + 'Expires today.'.t();
            } else if (license.daysRemaining < 32) {
                message = 'Free trial.'.t() + ' ' + Ext.String.format('{0} ', license.daysRemaining) + 'days remain.'.t();
            } else {
                message = 'Free trial.'.t();
            }
        } else if (!license.valid) {
            message = license.status;
        }
        return message;
    },

    reloadLicenses: function() {
        Rpc.directData('rpc.UvmContext.licenseManager.reloadLicenses', true);

        Ext.fireEvent('loadUserLicenseMessages');
    },

    activeClone: function(source) {
        // clone the source record which will usually be emptyRow
        var target = Ext.clone(source);
        // look at each item in the source record
        Ext.iterate(source, function(key, value) {
            // look for items in the record that are arrays
            if ( (value !== null) && (typeof(value) === 'object') && (value.length) && (value.length === 2) && (typeof(value[0] === 'string')) ) {
                // found an array so evaluate first string as the function with the second string as the argument and put result in target record
                target[key] = eval(value[0] + "('" + value[1] + "')");
            }
        });
        return(target);
    },

    getAppStorageValue: function(itemName) {
        var data = Ung.util.Util.appStorage[itemName];
        return(data);
    },

    setAppStorageValue: function(itemName, itemValue) {
        Ung.util.Util.appStorage[itemName] = itemValue;
    },

    /**
     * When we make an delayed or async call and then try to manipulate an object in the post-call
     * method, there's a possibility the object will exist but will have been destroyed.
     * For example, go to a form and quickly click away while an RPC call is going on but before
     * the resulting method has been called.  Or just very long backend calls that the user clicks away
     * from out of impatience.  Without this check, subsequent calls to the scoped variables will fail
     * and won't be apparent unless the user has a developer console enabled.
     * This method us used to test any number of objects for the destroyed proprty and returns true if any are.
     */
    isDestroyed: function(){
        for( var i = 0; i < arguments.length; i++){
            if(typeof(arguments[i]) == 'object'){
                if(arguments[i].destroyed){
                    return true;
                }
            }
        }
        return false;
    },

    /**
     * We'd like to have url components encoded so that as much of the original value remains in ASCII
     * format with the following exceptions:
     * 
     * *    spaces replaced with dashes.  Yes, we know this means collisions with legit dashes.
     * *    All other non-ASCII characters url encoded.
     *
     * These are typically specialized cases like report category and titles.
     * 
     * @param  url Url component to ncode.
     * @return url returned as described above.
     */
    urlEncode: function(url){
        var encodedUrl = url.replace(/\s+/g, '-').toLowerCase();
        encodedUrl = Ext.Object.toQueryString({'':encodedUrl}).substr(1);
        return encodedUrl;
    },

    /**
     * Determine if passed IP matches network & netmask.
     * @param  string ip      IP address to test.
     * @param  string network Network in CIDR notation or prefix
     * @param  string netmask Netmask in CIDR notation.
     * @return boolean         true if IP is on network, otherwise false.
     */
    ipMatchesNetwork: function(ip, network, netmask){
        var dots;
        var netmaskInteger = 0;
        if(Number.isInteger(netmask)){
            netmaskInteger = Math.pow(2, 32) - Math.pow(2, 32 - netmask);
        }else{
            dots = netmask.split('.');
            netmaskInteger = ((((((+dots[0])*256)+(+dots[1]))*256)+(+dots[2]))*256)+(+dots[3]);
        }
        dots = network.split('.');
        var networkInteger = ((((((+dots[0])*256)+(+dots[1]))*256)+(+dots[2]))*256)+(+dots[3]);
        dots = ip.split('.');
        var ipInteger = ((((((+dots[0])*256)+(+dots[1]))*256)+(+dots[2]))*256)+(+dots[3]);
        return ((ipInteger & netmaskInteger) == (networkInteger & netmaskInteger) );
    },

    /**
     * Define the styling for global entries. 
     *
     * @param record  The grid record to evaluate.
     * @return Style to apply on global fields.
     */
    getGlobalRowClass: function(record) {
        if (record.phantom === false && record.dirty && !record.get('markedForNew') && record.get('markedForDelete')) {
            return 'mark-delete';
        }
        if (record.get('isGlobal') === true) {
            return 'row-global-locked';
        }
    },

    /**
     * Determines whether the global checkbox for a given grid record should be clickable.
     *
     * @param column  checkBox global column
     * @param rowIndex index of global field
     * @return boolean  Returns false if the 'isGlobal' field is true (i.e., the record is global and should not be edited).
     *                  Returns true if 'isGlobal' is false or not set (i.e., the checkbox can be clicked).
     */
    canToggleGlobalCheckbox: function (column, rowIndex) {
        var grid = column.up('grid');
        var store = column.getGridStore ? column.getGridStore() : grid.getStore();
        var record = store.getAt(rowIndex);
        if (!record) return true;
    
        var modified = record.modified || {};
        var originalValue = modified.hasOwnProperty('isGlobal') ? !record.get('isGlobal') : record.get('isGlobal');
        var canToggleIsGlobal = !(originalValue === true && !modified.hasOwnProperty('isGlobal'));
        if (!canToggleIsGlobal && !record.get('markedForNew')) {
            Ext.MessageBox.alert('Info', '<strong> Global Field </strong> cannot be edited!');
            return false;
        } else {
            return true;
        }
    },

    /**
     * Checks whether a given CIDR block belongs to a private IP address space.
     *
     * Private IP ranges (as defined in RFC 1918) include:
     * - 10.0.0.0/8
     * - 172.16.0.0/12
     * - 192.168.0.0/16
     *
     * @param {string} cidr - The CIDR notation string (e.g., "192.168.1.0/24") to validate.
     * @returns {boolean} - Returns true if the CIDR is within a private IP range; false otherwise.
     *
     * @example
     * isPrivateCIDR("192.168.1.0/24"); // true
     * isPrivateCIDR("8.8.8.0/24");     // false
     */
    isPrivateCIDR: function(cidr) {
        try {
            var parts = cidr.split('/');
            var ip = parts[0].split('.');
            if (ip.length !== 4) return false;
    
            for (var i = 0; i < ip.length; i++) {
                var num = parseInt(ip[i], 10);
                if (isNaN(num) || num < 0 || num > 255) return false;
                ip[i] = num; // Convert to integer for next checks
            }
    
            if (ip[0] === 10) return true;
            if (ip[0] === 172 && ip[1] >= 16 && ip[1] <= 31) return true;
            if (ip[0] === 192 && ip[1] === 168) return true;
    
            return false; // Not private
        } catch (e) {
            return false;
        }
    },
    

    isIPInRange: function (ip, network, netmask) {
        // Split the IP address into octets
        var nextPoolOctets = ip.split('.');
        var networkOctets = network.split('.');
        var netMaskOctets = netmask.split('.');

        // Convert octets to integers
        var networkInt = networkOctets.map(function(octet) { return parseInt(octet, 10); });
        var netmaskInt = netMaskOctets.map(function(octet) { return parseInt(octet, 10); });

        // Calculate the the broadcast address
        var broadcastAddr = networkInt.map(function(octet, index) { return (octet & netmaskInt[index]) | (~netmaskInt[index] & 255); }).join('.');

        // Convert IP addresses to decimals
        var ipInt = Util.convertIPIntoDecimalForEachOctet(ip);
        var broadcastAddress =  Util.convertIPIntoDecimalForEachOctet(broadcastAddr);
        var networkAddress =  Util.convertIPIntoDecimalForEachOctet(network);

        return ipInt > networkAddress && ipInt < broadcastAddress;
    },

    convertIPIntoDecimalForEachOctet: function(ip) {
    
        var octets = ip.split(".");
    
        var decimalValue = 0;
        var powers = [16777216, 65536, 256, 1]; // Powers of 256
    
        for (var i = 0; i < 4; i++) {
            var octet = parseInt(octets[i], 10);
           
            decimalValue += octet * powers[i];
        }
    
        return decimalValue;
    },

    getUnusedPoolAddr: function(addressPool, store, field){
        netMask = Util.getV4NetmaskMap()[addressPool.split('/')[1] ? addressPool.split('/')[1] : 32];
        network = Util.getNetwork(addressPool.split('/')[0], netMask); 
        var nextPoolAddr = Util.incrementIpAddr(addressPool.split('/')[0], 2);
        if(store !==undefined){
            while (Util.isAddrUsed(nextPoolAddr, store, field)) {
                nextPoolAddr = Util.incrementIpAddr(nextPoolAddr, 1);
            }         
        }
        return Util.isIPInRange(nextPoolAddr, network, netMask)? nextPoolAddr : '';
    },

    

    /**
     * From the specified IP address and netmask, return the network.
     * For example, 192.168.1.1/255.255.255.0 returns 192.168.1.0
     * @param {*} ip
     * @param {*} netmask
     */
    getNetwork: function(ip, netmask){
        var dots = netmask.split('.');
        var netmaskInteger = ((((((+dots[0])*256)+(+dots[1]))*256)+(+dots[2]))*256)+(+dots[3]);
        dots = ip.split('.');
        var ipInteger = ((((((+dots[0])*256)+(+dots[1]))*256)+(+dots[2]))*256)+(+dots[3]) & netmaskInteger;
        return ( (ipInteger>>>24) +'.' + (ipInteger>>16 & 255) +'.' + (ipInteger>>8 & 255) +'.' + (ipInteger & 255) );
    },

    /**
     * From the specified IP address and netmask, return the broadcast.
     * For example, 192.168.1.1/255.255.255.0 returns 192.168.1.255
     * @param {*} ip
     * @param {*} netmask
     */
    getBroadcast: function(ip, netmask){
        var network = this.getNetwork(ip, netmask);
        var networkOctets = network.split('.');
        var netMaskOctets = netmask.split('.');

        // Convert octets to integers
        var networkInt = networkOctets.map(function(octet) { return parseInt(octet, 10); });
        var netmaskInt = netMaskOctets.map(function(octet) { return parseInt(octet, 10); });

        // Calculate the the broadcast address
        var broadcastOctets = networkInt.map(function(octet, index) { return (octet & netmaskInt[index]) | (~netmaskInt[index] & 255); });
    
        // Format the broadcast address
        var broadcastAddr = broadcastOctets.join('.');
        return broadcastAddr;
    },

    /**
     * Increment the passed IP
     * @param  string ip      IP address to increment.
     * @param  int inc        Number to increment by.
     * @return string         Incremented IP address.
     */
    incrementIpAddr: function(ip, inc){
        var dots = ip.split('.');
        var ipInteger = ((((((+dots[0])*256)+(+dots[1]))*256)+(+dots[2]))*256)+(+dots[3])+inc;
        dots = [];
        dots.push(ipInteger >>> 24);
        dots.push((ipInteger >>> 16) % 256);
        dots.push((ipInteger >>> 8) % 256);
        dots.push(ipInteger % 256);
        return dots.join('.');
    },

    /**
     * Loop through the stored records to see if the passed ip address is already used
     * @param  string ip      IP address to be checked.
     * @param  int store      ExtJs store of records
     * @param  int dataIndex  ExtJs dataIndex property of records
     * @return boolean        Returns true if ip already present in dataIndex field of store record.
     */
    isAddrUsed: function(ip, store, dataIndex) {
        var ret = false;
        store.each(function(record,idx) {
            if( record.get(dataIndex) == ip ){
                ret = true;
            }
        });
        return ret;
    },

    /**
     * From the specified store, get modified records and sort any into
     * a changes object that tracks added/deleted records using the specified
     * idField.  Also uses enabledField value to determine.
     * @param store ExtJs store of records.
     * @param string idField Name of identifier field.  Defaults to 'id'
     * @param string enabledField Name of enabled field.  Defaults to 'enabled'.
     * @return object containing arrays of added and deleted records as json strings
     */
    storeGetChangedRecords: function(store, idField, enabledField){
        if(idField == undefined){
            enabledField = 'id';
        }
        if(enabledField == undefined){
            enabledField = 'enabled';
        }
        var changes = {
            added: [],
            deleted: []
        };

        store.getModifiedRecords().forEach(function(record){
            // Get changed and added entries
            var id = record.get(idField);
            var jsonRecord = JSON.stringify(record.data);
            // Build previous record using previous values.
            var jsonPreviousRecord = JSON.parse(JSON.stringify(record.data));
            Ext.Object.each(record.previousValues, function(key, value){
                jsonPreviousRecord[key] = value;
            });
            jsonPreviousRecord = JSON.stringify(jsonPreviousRecord);

            if(id != -1 && changes.deleted.indexOf(jsonPreviousRecord) == -1){
                changes.deleted.push(jsonPreviousRecord);
            }
            if( record.get(enabledField) &&
                ( id == -1 ||
                  changes.added.indexOf(jsonRecord) == -1)){
                changes.added.push(jsonRecord);
            }
        });
        store.getRemovedRecords().forEach(function(record){
            // Deleted entries
            var jsonRecord = JSON.stringify(record.data);
            if(changes.deleted.indexOf(jsonRecord) == -1){
                changes.deleted.push(jsonRecord);
            }
        });
        return changes;
    },

    /**
     * Analyze changes from components with stores (like grids) and return list of 
     * changed values into keys by the components listProperty value.
     * @param Array components  Array of components to review.
     * @param Object viewModel  Object listProperty grouping with objects that contain result from storeGetChangedRecords.
     * @param listFields Object of list key to override id and enabled like {'settings.tunnels.list': { 'enabledField': 'active' } }
     */
    updateListStoresToSettings: function( components, viewModel, listFields){
        var changes = {};
        components.forEach(function(component) {
            var store = component.getStore();
            var listId = component.listProperty;
            // Clear any filters applied on store
            var filters = store.getFilters().clone();
            store.clearFilter(true);
            if(listId == null){
                return;
            }
            var idField = 'id';
            var enabledField = 'enabled';
            if(listFields != undefined){
                if(listId in listFields){
                    if('idField' in listFields[listId]){
                        idField = listFields[listId]['idField'];
                    }
                    if('enabledField' in listFields[listId]){
                        enabledField = listFields[listId]['enabledField'];
                    }
                }
            }
            var values = Ext.Array.pluck(component.getBind().store.getDataObject().getRange(), 'data');
            if (store.getModifiedRecords().length > 0 ||
                store.getNewRecords().length > 0 ||
                store.getRemovedRecords().length > 0 ||
                store.isReordered) {
                var deleted = false;
                store.each(function(record) {
                    if(record.get('peerAddress') === '') {
                        record.set('markedForDelete', true);
                    }

                    if (record.get('markedForDelete')) {
                        record.drop();
                        deleted = true;
                    }
                });
                if(deleted){
                    // Need to pull values again if records were dropped.
                    values = Ext.Array.pluck(component.getBind().store.getDataObject().getRange(), 'data');
                }
                store.isReordered = undefined;
                if(listId == 'settings.tunnels.list' ){
                    // Determine what tunnels have changed.
                    changes[listId] = Util.storeGetChangedRecords(store, idField, enabledField);
                }
            }
            // Strip out ExtJs fields for comparision.
            values.forEach(function(row){
                Ext.Object.each(row, function(key){
                    if(key == '_id'){
                        delete row[key];
                    }
                });
            });
            viewModel.set(listId, values);
            // restore filters after data is processed
            filters.each( function(filter){
                store.addFilter(filter);
            });
        });
        return changes;
    },

    /**
     * From original and current settings objects, determine if settings have changed.
     * If ignoreKeys is non-empty, ignore these fields for the purpsoes of considering the change.
     * @param object originalSettings Original settings.
     * @param object settings current settings
     * @return true if changes, false if nto.
     */
    isSettingsChanged: function(originalSettings, settings, ignoreKeys){
        if(ignoreKeys == undefined){
            ignoreKeys = [];
        }
        var changed = true;
        var compareOriginalSettings = {};
        Ext.Object.each(originalSettings, function(key, value){
            if(ignoreKeys.indexOf(key) == -1){
                compareOriginalSettings[key] = value;
            }
        });
        var compareSettings = {};
        Ext.Object.each(settings, function(key, value){
            if(ignoreKeys.indexOf(key) == -1){
                compareSettings[key] = value;
            }
        });
        return JSON.stringify(compareSettings) != JSON.stringify(compareOriginalSettings);
    },

    /**
     * To compare two IP addresses we need to convert into decimal values,
     * Maximum it can go upto 255 so we will be converting it based upon that
    */
    convertIPIntoDecimal: function (ip) {
        if (!ip) return 0;
        var total = 0;
        var ipElements = ip.split(".");

        for (var i = 0; i < ipElements.length; i++) {
            var octet = parseInt(ipElements[i], 10); // Parse each octet as an integer
            total = total * 256 + octet;
        }

        return total >>> 0; // Ensure the result is an unsigned 32-bit integer
    },

    /**
     * This method finds if any conflict is present in currentIp 
     * with all the existing Ip addresses and returns true or error msg
    */
    findIpPoolConflict:function(currentIpAddress, ipAddressPool, context, currentIpDirtyCheck){
        try{
            var currentFieldIp = currentIpAddress,
                localNetworkStore = ipAddressPool,
                netSpaceAddr = currentFieldIp.split('/')[0],
                netSpacePrefix = currentFieldIp.split('/')[1] ? currentFieldIp.split('/')[1] : 32,
                recValue, recAddr, recPrefix, network, netMask;

            var index = Ext.Array.findBy(localNetworkStore,function(networkRecord) {
                recValue = networkRecord.split('/');
                recAddr = recValue[0]; 
                recPrefix = recValue[1];

                if(netSpacePrefix == recPrefix && netSpaceAddr == recAddr) return true;
                else if(Number(recPrefix) > Number(netSpacePrefix)) {
                    netMask = Util.getV4NetmaskMap()[netSpacePrefix];
                    network = Util.getNetwork(netSpaceAddr, netMask);
                    return Util.ipMatchesNetwork(recAddr, network, netMask);
                } 
                else {
                    netMask = Util.getV4NetmaskMap()[recPrefix];
                    network = Util.getNetwork(recAddr, netMask); 
                    return Util.ipMatchesNetwork(netSpaceAddr, network, netMask);
                }
            });

            if(index !== null) return "Address pool conflict".t();
            //NGFW-14533
            // validation as per fields should be:
            // Peer IP Address Field : Address should not be conflicts with any existing network registrations
            // Local Network Address : Address should not be conflicts with existing address present in list and with Peer IP Address.        
            // Added additional check  (context.xtype === "textarea" ) to verify import, remote networks field 
            if((context.dirty && context.ui === "default") || context.xtype === "textarea" ) {
                var ntwkSpace=null;
                if(currentIpDirtyCheck){
                    ntwkSpace = rpc.UvmContext.netspaceManager().isNetworkAvailable('wireguard-vpn', currentFieldIp.trim());   
                    return !ntwkSpace ? true : "Address pool conflict".t();
                }else{
                    ntwkSpace = null;
                    for(var i=0;i<ipAddressPool.length;i++){
                        ntwkSpace = rpc.UvmContext.netspaceManager().isNetworkAvailable('wireguard-vpn', ipAddressPool[i].trim());
                        if(ntwkSpace){
                            break;
                        }   
                    }
                    return !ntwkSpace ? true : "Address pool conflict".t();
                }
            } else return true;
        }catch(err){
            throw err;
        }
    },

    /**
     * Method to get list of supported protocols
     * @param boolean onlyTcpUdp True if only TCP and UDP traffic is supported.
     * @return List of protocols based on onlyTcpUdp flag value.
     */
    getProtocolList: function(onlyTcpUdp) {
        if(onlyTcpUdp) {
            return [[
                "TCP","TCP"
            ],[
                "UDP","UDP"
            ]];
        } else {
            return [[
                "TCP","TCP"
            ],[
                "UDP","UDP"
            ],[
                "ICMP","ICMP"
            ],[
                "GRE","GRE"
            ],[
                "ESP","ESP"
            ],[
                "AH","AH"
            ],[
                "SCTP","SCTP"
            ],[
                "OSPF","OSPF"
            ]];
        }
    },

    /**
     * Method to check if new value is not exists in current store
     * @param value that has to check for uniqueness.
     * @param form tab for which error has to be shown.
     * @param field against which value has to be validated.
     * @param component current object.
     * @param query grid name to fetch the value of grid.
     * @return boolean if value is validated else error for invalid value.
     */
    isUnique: function(value, form, field, component, query) {
        var currentRecord ;
        if(component.getId().indexOf('textfield') !== -1){
        if(component.up('window')!=undefined)
            currentRecord = component.up('window').getViewModel().data.record.get(field);
        if (value === currentRecord) {
            return true;
        }
        //Return true if editable field peerAddress in grid is not modified
        if(!component.dirty && field === 'peerAddress') {
            return true; 
        }
        }
        var grid = Ext.ComponentQuery.query(query)[0];
        var store = grid.getStore();

        var isNameUnique = store.findBy(function(record) {
            return record.get(field) === value;
        }) === -1;
        
        return isNameUnique? true : Ext.String.format('A {0} with this {1} already exists.'.t(), form, field);
    },

    /**
     * Method to get list of ip addresses and netmasks from remote networks seprated by comma
     * @param remoteNetworks networks in CIDR format seprated by comma.
     * @return List of objects containg Ip and subnetMask of individual network.
     */
    getParsedAddresses: function(remoteNetworks){
        var parsedAddresses = [];
        // Split remoteNetworks by commas to handle multiple networks
        var addresses = remoteNetworks.split(',');
        addresses.forEach(function (address) {
            // Trim each address to remove leading/trailing spaces
            address = address.trim();
            // Split the address into IP and subnet mask
            var parts = address.split('/');
            var ip = parts[0];
            var subnetMask = parseInt(parts[1], 10); // Specify radix as 10

            parsedAddresses.push({
                ip: ip,
                subnetMask: subnetMask
            });
        });

        return parsedAddresses;
    },

    /**
     * Method to check if two IP ranges overlap
     * @param address1 first object containg ip address and subnet mask.
     * @param address2 second object containg ip address and subnet mask.
     * @return boolean, true if both value overlap else false.
     */
    doRangesOverlap: function(address1, address2) {

        // Extract network from addresses and subnet masks
        var network1 = Util.convertIPIntoDecimalForEachOctet(address1.ip) & (0xFFFFFFFF << (32 - address1.subnetMask));
        var network2 = Util.convertIPIntoDecimalForEachOctet(address2.ip) & (0xFFFFFFFF << (32 - address2.subnetMask));
    
        // Check if the network addresses are the same and if they intersect
        return network1 === network2;
    },

    /**
     * Method to validate network intersection
     * @param value that has to check for uniqueness.
     * @param form tab for which error has to be shown.
     * @param field against which value has to be validated.
     * @param component current object.
     * @param query grid name to fetch the value of grid.
     * @return boolean if value is validated else error for invalid value.
     */
    isIpIntersects: function(value, form, field, component, query) {
        var grid = Ext.ComponentQuery.query(query)[0];
        var store = grid.getStore();
    
        var parsedAddresses;
        // Array to store parsed new IP addresses with subnet masks
        if(value !== null && value !== "")
            parsedAddresses = Util.getParsedAddresses(value);
       
        // Fetch already stored values
        var allStoredAddresses = [];

        store.each(function(record) {
            var fieldValue = record.get(field);
            if (fieldValue != null) {
                allStoredAddresses.push(Util.getParsedAddresses(fieldValue));
            }
        });
        // For edit grid, check only for newly added values
        if (component.getId().indexOf('textfield') !== -1 && component.bind !== null && component.bind.value.lastValue !== undefined) {

            if(component.bind.value.lastValue !== null){
                // Fetch previous value stored in grid
                var currentStoredValueInGrid = component.bind.value.lastValue;
                // Current parsed addresses with subnet masks in editor grid
                var currentParsedAddresses = Util.getParsedAddresses(currentStoredValueInGrid);
        
                // Check for intersection between new IP ranges and already stored values in current record
                var newlyAddedValue = parsedAddresses.filter(function(parsedAddress) {
                    return !currentParsedAddresses.some(function(currentAddress) {
                        return Util.doRangesOverlap(parsedAddress, currentAddress);
                    });
                });
        
                // Check for intersection between new IP ranges within the same record
                if (parsedAddresses.some(function(parsedAddress, k) {
                    return parsedAddresses.slice(k + 1).some(function(nextParsedAddress) {
                        return Util.doRangesOverlap(parsedAddress, nextParsedAddress);
                    });
                })) {
                    return Ext.String.format('The {0} values intersect with each other.'.t(), form);
                }
                parsedAddresses = newlyAddedValue;
            }
        }

        // Check if parsedAddresses is defined and not empty
        if (parsedAddresses && parsedAddresses.length > 0) {
            // Check for intersection between new IP ranges and stored values
            if (allStoredAddresses.some(function(storedAddressesArray) {
                return storedAddressesArray.some(function(storedAddress) {
                    return parsedAddresses.some(function(parsedAddress) {
                        return Util.doRangesOverlap(storedAddress, parsedAddress);
                    });
                });
            })) {
                return Ext.String.format('The {0} values intersect with already stored values in {1} records.'.t(), field, form);
            }
        }
    
        // If no intersection found, return true
        return true;
    }
});
