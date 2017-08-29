Ext.define('Ung.view.extra.HostsController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.hosts',

    control: {
        '#': {
            deactivate: 'onDeactivate',
            refresh: 'getHosts'
        },
        '#hostsgrid': {
            afterrender: 'getHosts'
        }
    },

    onDeactivate: function (view) {
        view.destroy();
    },

    setAutoRefresh: function (btn) {
        var me = this,
            vm = this.getViewModel();
        vm.set('autoRefresh', btn.pressed);

        if (btn.pressed) {
            me.getHosts();
            this.refreshInterval = setInterval(function () {
                me.getHosts();
            }, 5000);
        } else {
            clearInterval(this.refreshInterval);
        }

    },

    resetView: function( btn ){
        var grid = this.getView().down('#hostsgrid');
        Ext.state.Manager.clear(grid.stateId);
        grid.reconfigure(null, grid.initialConfig.columns);
    },

    getHosts: function () {
        var me = this,
            v = me.getView(),
            grid = me.getView().down('#hostsgrid'),
            filters = grid.getStore().getFilters(),
            store = grid.getStore('hosts');

        var existingRouteFilter = filters.findBy( function( filter ){
            if(filter.config.source == "route"){
                return true;
            }
        } );
        if( existingRouteFilter != null ){
            filters.remove(existingRouteFilter);
        }
        if( v.routeFilter ){
            filters.add(v.routeFilter);
        }

        if( !store.getFields() ){
            store.setFields(grid.fields);
        }

        grid.getView().setLoading(true);
        Rpc.asyncData('rpc.hostTable.getHosts')
            .then(function(result) {
                grid.getView().setLoading(false);
                store.loadData(JSON.parse('[{"usernameIpsecVpn":null,"entitled":true,"quotaExpirationTime":1503471599632,"creationTime":1503043004775,"javaClass":"com.untangle.uvm.HostTableEntry","hostnameDhcp":null,"lastSessionTime":1503416712442,"usernameDirectoryConnector":null,"quotaRemaining":107373556570,"hostnameSource":"Device","hostname":"","quotaIssueTime":1503385239632,"hostnameDns":"SonosZP","usernameSource":"Device","usernameOpenVpn":null,"address":"172.16.2.144","lastAccessTime":1503429300032,"usernameDevice":"sonos","lastCompletedTcpSessionTime":1503416708403,"active":false,"httpUserAgent":"Sonos","tags":{"javaClass":"java.util.LinkedList","list":[{"valid":true,"expired":false,"javaClass":"com.untangle.uvm.Tag","expirationTime":0,"name":"sonos"}]},"hostnameDeviceLastKnown":"SonosZP","macAddress":"00:0e:58:31:b3:92","hostnameDevice":"","macVendor":"Sonos, Inc.","hostnameDirectoryConnector":null,"interfaceId":2,"quotaSize":107374182400,"usernameCaptivePortal":null,"hostnameReports":null,"captivePortalAuthenticated":false,"hostnameOpenVpn":null,"tagsString":"sonos","username":"sonos"},{"usernameIpsecVpn":null,"entitled":true,"quotaExpirationTime":1503471599379,"creationTime":1495779172876,"javaClass":"com.untangle.uvm.HostTableEntry","hostnameDhcp":null,"lastSessionTime":1503418535096,"usernameDirectoryConnector":null,"quotaRemaining":107341297766,"hostnameSource":"Device","hostname":"","quotaIssueTime":1503385239379,"hostnameDns":"SonosZP","usernameSource":"Device","usernameOpenVpn":null,"address":"172.16.2.113","lastAccessTime":1503429300030,"usernameDevice":"sonos","lastCompletedTcpSessionTime":1503418535527,"active":false,"httpUserAgent":"Sonos","tags":{"javaClass":"java.util.LinkedList","list":[{"valid":true,"expired":false,"javaClass":"com.untangle.uvm.Tag","expirationTime":0,"name":"sonos"}]},"hostnameDeviceLastKnown":"SonosZP","macAddress":"00:0e:58:f8:d7:58","hostnameDevice":"","macVendor":"Sonos, Inc.","hostnameDirectoryConnector":null,"interfaceId":2,"quotaSize":107374182400,"usernameCaptivePortal":null,"hostnameReports":null,"captivePortalAuthenticated":false,"hostnameOpenVpn":null,"tagsString":"sonos","username":"sonos"},{"usernameIpsecVpn":null,"entitled":true,"quotaExpirationTime":1503471599487,"creationTime":1495805290680,"javaClass":"com.untangle.uvm.HostTableEntry","hostnameDhcp":null,"lastSessionTime":1503416711983,"usernameDirectoryConnector":null,"quotaRemaining":107373542661,"hostnameSource":"Device","hostname":"","quotaIssueTime":1503385239487,"hostnameDns":"SonosZP","usernameSource":"Device","usernameOpenVpn":null,"address":"172.16.2.145","lastAccessTime":1503428820025,"usernameDevice":"sonos","lastCompletedTcpSessionTime":1503416708409,"active":false,"httpUserAgent":"Sonos","tags":{"javaClass":"java.util.LinkedList","list":[{"valid":true,"expired":false,"javaClass":"com.untangle.uvm.Tag","expirationTime":0,"name":"sonos"}]},"hostnameDeviceLastKnown":"SonosZP","macAddress":"00:0e:58:32:63:4e","hostnameDevice":"","macVendor":"Sonos, Inc.","hostnameDirectoryConnector":null,"interfaceId":2,"quotaSize":107374182400,"usernameCaptivePortal":null,"hostnameReports":null,"captivePortalAuthenticated":false,"hostnameOpenVpn":null,"tagsString":"sonos","username":"sonos"},{"usernameIpsecVpn":null,"entitled":true,"quotaExpirationTime":1503471599023,"creationTime":1496104838851,"javaClass":"com.untangle.uvm.HostTableEntry","hostnameDhcp":"iPhone","lastSessionTime":1503407504004,"usernameDirectoryConnector":null,"quotaRemaining":107306497954,"hostnameSource":"Device","hostname":"","quotaIssueTime":1503385951023,"hostnameDns":"DenaZinssiPhone","usernameSource":"Device","usernameOpenVpn":null,"address":"172.16.2.146","lastAccessTime":1503418217413,"usernameDevice":"dena","lastCompletedTcpSessionTime":1503407401232,"active":false,"httpUserAgent":"NewsToday/1000 CFNetwork/811.5.4 Darwin/16.7.0","tags":{"javaClass":"java.util.LinkedList","list":[{"valid":true,"expired":false,"javaClass":"com.untangle.uvm.Tag","expirationTime":0,"name":"iphone"}]},"hostnameDeviceLastKnown":"iPhone","macAddress":"38:ca:da:73:f4:c4","hostnameDevice":"","macVendor":"Apple, Inc.","hostnameDirectoryConnector":null,"interfaceId":3,"quotaSize":107374182400,"usernameCaptivePortal":null,"hostnameReports":null,"captivePortalAuthenticated":false,"hostnameOpenVpn":null,"tagsString":"iphone","username":"dena"},{"usernameIpsecVpn":null,"entitled":true,"quotaExpirationTime":0,"creationTime":1496363089665,"javaClass":"com.untangle.uvm.HostTableEntry","hostnameDhcp":null,"lastSessionTime":1503295316186,"usernameDirectoryConnector":null,"quotaRemaining":0,"hostnameSource":"Device","hostname":"","quotaIssueTime":0,"hostnameDns":"dmorris-laptop","usernameSource":"Device","usernameOpenVpn":null,"address":"172.16.2.147","lastAccessTime":1503418217414,"usernameDevice":"dmorris","lastCompletedTcpSessionTime":1503295316211,"active":false,"httpUserAgent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.101 Safari/537.36","tags":{"javaClass":"java.util.LinkedList","list":[{"valid":true,"expired":false,"javaClass":"com.untangle.uvm.Tag","expirationTime":0,"name":"windows"}]},"hostnameDeviceLastKnown":"dmorris-laptop","macAddress":"28:b2:bd:1c:c5:f0","hostnameDevice":"","macVendor":"Intel Corporate","hostnameDirectoryConnector":null,"interfaceId":3,"quotaSize":0,"usernameCaptivePortal":null,"hostnameReports":null,"captivePortalAuthenticated":false,"hostnameOpenVpn":null,"tagsString":"windows","username":"dmorris"},{"usernameIpsecVpn":null,"entitled":true,"quotaExpirationTime":0,"creationTime":1500880495484,"javaClass":"com.untangle.uvm.HostTableEntry","hostnameDhcp":null,"lastSessionTime":1502900826396,"usernameDirectoryConnector":null,"quotaRemaining":0,"hostnameSource":"Device","hostname":"","quotaIssueTime":0,"hostnameDns":"SonosCR","usernameSource":"Device","usernameOpenVpn":null,"address":"172.16.2.181","lastAccessTime":1503418217415,"usernameDevice":"sonos","lastCompletedTcpSessionTime":1502900826483,"active":false,"httpUserAgent":"Sonos","tags":{"javaClass":"java.util.LinkedList","list":[{"valid":true,"expired":false,"javaClass":"com.untangle.uvm.Tag","expirationTime":0,"name":"sonos"}]},"hostnameDeviceLastKnown":"SonosCR","macAddress":"00:0e:58:02:1a:50","hostnameDevice":"","macVendor":"Sonos, Inc.","hostnameDirectoryConnector":null,"interfaceId":2,"quotaSize":0,"usernameCaptivePortal":null,"hostnameReports":null,"captivePortalAuthenticated":false,"hostnameOpenVpn":null,"tagsString":"sonos","username":"sonos"},{"usernameIpsecVpn":null,"entitled":true,"quotaExpirationTime":1503471599444,"creationTime":1495525317411,"javaClass":"com.untangle.uvm.HostTableEntry","hostnameDhcp":null,"lastSessionTime":1503416712333,"usernameDirectoryConnector":null,"quotaRemaining":107373569406,"hostnameSource":"Device","hostname":"","quotaIssueTime":1503385239444,"hostnameDns":"SonosZP","usernameSource":"Device","usernameOpenVpn":null,"address":"172.16.2.154","lastAccessTime":1503429180047,"usernameDevice":"sonos","lastCompletedTcpSessionTime":1503416708483,"active":false,"httpUserAgent":"Wget","tags":{"javaClass":"java.util.LinkedList","list":[{"valid":true,"expired":false,"javaClass":"com.untangle.uvm.Tag","expirationTime":0,"name":"sonos"}]},"hostnameDeviceLastKnown":"SonosZP","macAddress":"00:0e:58:c9:6a:8a","hostnameDevice":"","macVendor":"Sonos, Inc.","hostnameDirectoryConnector":null,"interfaceId":2,"quotaSize":107374182400,"usernameCaptivePortal":null,"hostnameReports":null,"captivePortalAuthenticated":false,"hostnameOpenVpn":null,"tagsString":"sonos","username":"sonos"},{"usernameIpsecVpn":null,"entitled":true,"quotaExpirationTime":1503471599247,"creationTime":1494009258801,"javaClass":"com.untangle.uvm.HostTableEntry","hostnameDhcp":"SonosZP","lastSessionTime":1503418576186,"usernameDirectoryConnector":null,"quotaRemaining":107373545596,"hostnameSource":"Device","hostname":"","quotaIssueTime":1503385239247,"hostnameDns":"SonosZP","usernameSource":"Device","usernameOpenVpn":null,"address":"172.16.2.122","lastAccessTime":1503428940021,"usernameDevice":"sonos","lastCompletedTcpSessionTime":1503418576212,"active":false,"httpUserAgent":"Sonos","tags":{"javaClass":"java.util.LinkedList","list":[{"valid":true,"expired":false,"javaClass":"com.untangle.uvm.Tag","expirationTime":0,"name":"sonos"}]},"hostnameDeviceLastKnown":"SonosZP","macAddress":"5c:aa:fd:0f:00:ca","hostnameDevice":"","macVendor":"Sonos, Inc.","hostnameDirectoryConnector":null,"interfaceId":2,"quotaSize":107374182400,"usernameCaptivePortal":null,"hostnameReports":null,"captivePortalAuthenticated":false,"hostnameOpenVpn":null,"tagsString":"sonos","username":"sonos"},{"usernameIpsecVpn":null,"entitled":true,"quotaExpirationTime":0,"creationTime":1500777328419,"javaClass":"com.untangle.uvm.HostTableEntry","hostnameDhcp":null,"lastSessionTime":1500777328424,"usernameDirectoryConnector":null,"quotaRemaining":0,"hostnameSource":"Device","hostname":"","quotaIssueTime":0,"hostnameDns":"SonosCR","usernameSource":"Device","usernameOpenVpn":null,"address":"172.16.2.156","lastAccessTime":1503418217418,"usernameDevice":"sonos","lastCompletedTcpSessionTime":1500777328461,"active":false,"httpUserAgent":"Sonos","tags":{"javaClass":"java.util.LinkedList","list":[{"valid":true,"expired":false,"javaClass":"com.untangle.uvm.Tag","expirationTime":0,"name":"sonos"}]},"hostnameDeviceLastKnown":"SonosCR","macAddress":"00:0e:58:00:d5:1c","hostnameDevice":"","macVendor":"Sonos, Inc.","hostnameDirectoryConnector":null,"interfaceId":2,"quotaSize":0,"usernameCaptivePortal":null,"hostnameReports":null,"captivePortalAuthenticated":false,"hostnameOpenVpn":null,"tagsString":"sonos","username":"sonos"},{"usernameIpsecVpn":null,"entitled":true,"quotaExpirationTime":1503471599288,"creationTime":1495779173043,"javaClass":"com.untangle.uvm.HostTableEntry","hostnameDhcp":null,"lastSessionTime":1503418587717,"usernameDirectoryConnector":null,"quotaRemaining":107373519527,"hostnameSource":"Device","hostname":"","quotaIssueTime":1503385239288,"hostnameDns":"SonosZP","usernameSource":"Device","usernameOpenVpn":null,"address":"172.16.2.190","lastAccessTime":1503429240027,"usernameDevice":"sonos","lastCompletedTcpSessionTime":1503418587733,"active":false,"httpUserAgent":"Wget","tags":{"javaClass":"java.util.LinkedList","list":[{"valid":true,"expired":false,"javaClass":"com.untangle.uvm.Tag","expirationTime":0,"name":"sonos"}]},"hostnameDeviceLastKnown":"SonosZP","macAddress":"00:0e:58:32:26:cc","hostnameDevice":"","macVendor":"Sonos, Inc.","hostnameDirectoryConnector":null,"interfaceId":2,"quotaSize":107374182400,"usernameCaptivePortal":null,"hostnameReports":null,"captivePortalAuthenticated":false,"hostnameOpenVpn":null,"tagsString":"sonos","username":"sonos"},{"usernameIpsecVpn":null,"entitled":true,"quotaExpirationTime":1503471599328,"creationTime":1492613392388,"javaClass":"com.untangle.uvm.HostTableEntry","hostnameDhcp":null,"lastSessionTime":1503429351352,"usernameDirectoryConnector":null,"quotaRemaining":107314949828,"hostnameSource":"Device","hostname":"linuxhead","quotaIssueTime":1503385341328,"hostnameDns":null,"usernameSource":"Device","usernameOpenVpn":null,"address":"172.16.2.2","lastAccessTime":1503429351376,"usernameDevice":"dmorris","lastCompletedTcpSessionTime":1503429351376,"active":true,"httpUserAgent":"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.86 Safari/537.36","tags":{"javaClass":"java.util.LinkedList","list":[{"valid":true,"expired":false,"javaClass":"com.untangle.uvm.Tag","expirationTime":0,"name":"linux"}]},"hostnameDeviceLastKnown":"","macAddress":"f8:b1:56:d9:7d:77","hostnameDevice":"linuxhead","macVendor":"Dell Inc.","hostnameDirectoryConnector":null,"interfaceId":2,"quotaSize":107374182400,"usernameCaptivePortal":null,"hostnameReports":null,"captivePortalAuthenticated":false,"hostnameOpenVpn":null,"tagsString":"linux","username":"dmorris"},{"usernameIpsecVpn":null,"entitled":true,"quotaExpirationTime":0,"creationTime":1496421634395,"javaClass":"com.untangle.uvm.HostTableEntry","hostnameDhcp":null,"lastSessionTime":1503263497601,"usernameDirectoryConnector":null,"quotaRemaining":0,"hostnameSource":"Device","hostname":"","quotaIssueTime":0,"hostnameDns":"SonosCR","usernameSource":"Device","usernameOpenVpn":null,"address":"172.16.2.131","lastAccessTime":1503418217420,"usernameDevice":"sonos","lastCompletedTcpSessionTime":1503263497671,"active":false,"httpUserAgent":"Sonos","tags":{"javaClass":"java.util.LinkedList","list":[{"valid":true,"expired":false,"javaClass":"com.untangle.uvm.Tag","expirationTime":0,"name":"sonos"}]},"hostnameDeviceLastKnown":"SonosCR","macAddress":"00:0e:58:00:70:03","hostnameDevice":"","macVendor":"Sonos, Inc.","hostnameDirectoryConnector":null,"interfaceId":2,"quotaSize":0,"usernameCaptivePortal":null,"hostnameReports":null,"captivePortalAuthenticated":false,"hostnameOpenVpn":null,"tagsString":"sonos","username":"sonos"},{"usernameIpsecVpn":null,"entitled":true,"quotaExpirationTime":1503471599254,"creationTime":1502984366446,"javaClass":"com.untangle.uvm.HostTableEntry","hostnameDhcp":"android-f05f7c7bcd85a35e","lastSessionTime":1503428994550,"usernameDirectoryConnector":null,"quotaRemaining":107298231659,"hostnameSource":"Device","hostname":"","quotaIssueTime":1503385236254,"hostnameDns":"android-f05f7c7bcd85a35e","usernameSource":null,"usernameOpenVpn":null,"address":"172.16.2.132","lastAccessTime":1503429240035,"usernameDevice":null,"lastCompletedTcpSessionTime":1503428994563,"active":true,"httpUserAgent":"okhttp/3.8.1","tags":{"javaClass":"java.util.LinkedList","list":[]},"hostnameDeviceLastKnown":"android-f05f7c7bcd85a35e","macAddress":"ac:cf:85:c9:da:01","hostnameDevice":"","macVendor":"HUAWEI TECHNOLOGIES CO.,LTD","hostnameDirectoryConnector":null,"interfaceId":4,"quotaSize":107374182400,"usernameCaptivePortal":null,"hostnameReports":null,"captivePortalAuthenticated":false,"hostnameOpenVpn":null,"tagsString":"","username":null},{"usernameIpsecVpn":null,"entitled":true,"quotaExpirationTime":1503471599468,"creationTime":1479843828805,"javaClass":"com.untangle.uvm.HostTableEntry","hostnameDhcp":null,"lastSessionTime":1503416713154,"usernameDirectoryConnector":null,"quotaRemaining":107373512989,"hostnameSource":"Device","hostname":"","quotaIssueTime":1503385239468,"hostnameDns":"SonosZP","usernameSource":"Device","usernameOpenVpn":null,"address":"172.16.2.105","lastAccessTime":1503429300028,"usernameDevice":"sonos","lastCompletedTcpSessionTime":1503416713161,"active":false,"httpUserAgent":"Sonos","tags":{"javaClass":"java.util.LinkedList","list":[{"valid":true,"expired":false,"javaClass":"com.untangle.uvm.Tag","expirationTime":0,"name":"sonos"}]},"hostnameDeviceLastKnown":"SonosZP","macAddress":"00:0e:58:23:6c:cc","hostnameDevice":"","macVendor":"Sonos, Inc.","hostnameDirectoryConnector":null,"interfaceId":2,"quotaSize":107374182400,"usernameCaptivePortal":null,"hostnameReports":null,"captivePortalAuthenticated":false,"hostnameOpenVpn":null,"tagsString":"sonos","username":"sonos"},{"usernameIpsecVpn":null,"entitled":true,"quotaExpirationTime":1503471599189,"creationTime":1495814490020,"javaClass":"com.untangle.uvm.HostTableEntry","hostnameDhcp":null,"lastSessionTime":1503429347244,"usernameDirectoryConnector":null,"quotaRemaining":101384483157,"hostnameSource":"Device","hostname":"dmorris-win10","quotaIssueTime":1503385206189,"hostnameDns":null,"usernameSource":"Device","usernameOpenVpn":null,"address":"172.16.2.10","lastAccessTime":1503429347271,"usernameDevice":"dmorris","lastCompletedTcpSessionTime":1503429347271,"active":true,"httpUserAgent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.101 Safari/537.36","tags":{"javaClass":"java.util.LinkedList","list":[{"valid":true,"expired":false,"javaClass":"com.untangle.uvm.Tag","expirationTime":0,"name":"windows"}]},"hostnameDeviceLastKnown":"DESKTOP-A1ORGJH","macAddress":"1c:1b:0d:0d:65:6b","hostnameDevice":"dmorris-win10","macVendor":"GIGA-BYTE TECHNOLOGY CO.,LTD.","hostnameDirectoryConnector":null,"interfaceId":2,"quotaSize":107374182400,"usernameCaptivePortal":null,"hostnameReports":null,"captivePortalAuthenticated":false,"hostnameOpenVpn":null,"tagsString":"windows","username":"dmorris"},{"usernameIpsecVpn":null,"entitled":true,"quotaExpirationTime":1503471599938,"creationTime":1502816308945,"javaClass":"com.untangle.uvm.HostTableEntry","hostnameDhcp":null,"lastSessionTime":1503429248937,"usernameDirectoryConnector":null,"quotaRemaining":107373940976,"hostnameSource":"Device","hostname":"","quotaIssueTime":1503385598938,"hostnameDns":"untangle","usernameSource":null,"usernameOpenVpn":null,"address":"172.16.2.107","lastAccessTime":1503429300029,"usernameDevice":null,"lastCompletedTcpSessionTime":1503418375500,"active":false,"httpUserAgent":"Debian APT-HTTP/1.3 (1.0.9.8.4)","tags":{"javaClass":"java.util.LinkedList","list":[]},"hostnameDeviceLastKnown":"untangle","macAddress":"60:38:e0:0f:6a:c1","hostnameDevice":"","macVendor":"Belkin International Inc.","hostnameDirectoryConnector":null,"interfaceId":2,"quotaSize":107374182400,"usernameCaptivePortal":null,"hostnameReports":null,"captivePortalAuthenticated":false,"hostnameOpenVpn":null,"tagsString":"","username":null}]'));
                store.sort('address', 'DSC');

                v.down('ungridstatus').fireEvent('update');

                grid.getSelectionModel().select(0);
            });
    },

    refillQuota: function (view, rowIndex, colIndex, item, e, record) {
        var me = this;
        Ext.MessageBox.wait('Refilling...'.t(), 'Please wait'.t());
        Rpc.asyncData('rpc.hostTable.refillQuota', record.get('address'))
            .then(function () {
                me.getHosts();
            }, function (ex) {
                Util.handleException(ex);
            }).always(function () {
                Ext.MessageBox.hide();
            });
    },

    dropQuota: function (view, rowIndex, colIndex, item, e, record) {
        var me = this;
        Ext.MessageBox.wait('Removing Quota...'.t(), 'Please wait'.t());
        Rpc.asyncData('rpc.hostTable.removeQuota', record.get('address'))
            .then(function () {
                me.getHosts();
            }, function (ex) {
                Util.handleException(ex);
            }).always(function () {
                Ext.MessageBox.hide();
            });
    }

});
