Ext.define('Ung.apps.directory-connector.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-directory-connector',

    control: {
        '#': {
            afterrender: 'getSettings',
        }
    },

    getSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();
        v.setLoading(true);
        v.appManager.getSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            vm.set('settings', result);
         });

    },

    setSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        if (!Util.validateForms(v)) {
            return;
        }

        v.setLoading(true);

        v.appManager.setSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            Util.successToast('Settings saved');
            me.getSettings();
        }, vm.get('settings'));
    },

    downloadUserApiScript: function(){
        window.open("../userapi/");
    },

    activeDirectoryPortChanger: function(elem){
        var me = this, v = this.getView(), vm = this.getViewModel();

        var secureValue = elem.getValue();
        var currentPortValue = vm.get("settings.activeDirectorySettings.LDAPPort");
        if( secureValue == true ){
            if( currentPortValue == "389"){
                vm.set("settings.activeDirectorySettings.LDAPPort", "636");
            }
        }else{
            if( currentPortValue == "636"){
                vm.set("settings.activeDirectorySettings.LDAPPort", "389");
            }
        }
    },

    rpc: {},

    getActiveDirectoryManager: function(forceReload) {
        var me = this, v = this.getView(), vm = this.getViewModel();
        if (forceReload || this.rpc.activeDirectoryManager === undefined) {
            try {
                this.rpc.activeDirectoryManager = v.appManager.getActiveDirectoryManager();
            } catch (e) {
                Util.exceptionToast(e);
            }
        }
        return this.rpc.activeDirectoryManager;
    },

    getRadiusManager: function(forceReload) {
        var me = this, v = this.getView(), vm = this.getViewModel();
        if (forceReload || this.rpc.radiusManager === undefined) {
            try {
                this.rpc.radiusManager = v.appManager.getRadiusManager();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.rpc.radiusManager;
    },

    activeDirectoryTest: function(){
        var me = this, v = this.getView(), vm = this.getViewModel();
        Ext.MessageBox.wait( "Testing...".t(), "Active Directory Test".t());
        this.getActiveDirectoryManager().getActiveDirectoryStatusForSettings( Ext.bind(function(result, exception) {
            if (exception) { Util.exceptionToast(ex); return; }
            var message = result.t();
            Ext.MessageBox.alert( "Active Directory Test".t(), message);
        }, this), vm.get("settings"));
    },

    activeDirectoryUsers: function(){
        var me = this, v = this.getView(), vm = this.getViewModel();
        Ext.MessageBox.wait( "Obtaining users...".t(), "Active Directory Users".t());
        this.getActiveDirectoryManager().getActiveDirectoryUserEntries( Ext.bind(function( result, exception ) {
            if (exception) { Util.exceptionToast(ex); return; }

            var userEntries = result.list;
            var usersList = "";
            var usersArray = [];
            var i;
            for(i=0; i<userEntries.length; i++) {
                if( userEntries[i] == null ) {
                    continue;
                }
                var uid = userEntries[i].uid != null ? userEntries[i].uid: '[any]'.t();
                usersArray.push(( uid + "\r\n"));
            }
            usersArray.sort(function(a,b) {
                a = String(a).toLowerCase();
                b = String(b).toLowerCase();
                try {
                    if(a < b) {
                        return -1;
                    }
                    if (a > b) {
                        return 1;
                    }
                    return 0;
                } catch(e) {
                    return 0;
                }
            });

            usersList += '[any]'.t() +"\r\n";
            for (i = 0 ; i < usersArray.length ; i++) {
                usersList += usersArray[i];
            }

            v.down("[name=activeDirectoryUsersTextarea]").setValue(usersList).setVisible(true);
            Ext.MessageBox.close();
        }, this));
    },

    activeDirectoryGroupMap: function(){
        var me = this, v = this.getView(), vm = this.getViewModel();

        Ext.MessageBox.wait( "Obtaining user group map...".t(), "Active Directory Users".t());
        this.getActiveDirectoryManager().getUserGroupMap( Ext.bind(function(result, exception) {
            if (exception) { Util.exceptionToast(ex); return; }
            var users = [];
            for ( var k in result.map) {
                users.push({name: k, groups: result.map[k]});
            }
            v.down("[name=groupMapGrid]").getStore().loadData(users);
            v.down("[name=groupMapGrid]").setVisible(true);
            Ext.MessageBox.close();
        },this));

    },

    activeDirectoryGroupRefreshCache: function(){
        var me = this, v = this.getView(), vm = this.getViewModel();
        v.appManager.refreshGroupCache(Ext.bind(function(result, exception) {
            if (exception) { Util.exceptionToast(ex); return; }
        }, this));
    },

    radiusTest: function(){
        var me = this, v = this.getView(), vm = this.getViewModel();

        Ext.MessageBox.wait( "Testing RADIUS...".t(), "RADIUS Test".t());
        var username = v.down('textfield[name=radiusTestUsername]').getValue();
        var password = v.down('textfield[name=radiusTestPassword]').getValue();

        var message = this.getRadiusManager().getRadiusStatusForSettings( Ext.bind(function(result, exception) {
            if (exception) { Util.exceptionToast(ex); return; }
            var message = result.t();
            console.log(result);
            Ext.MessageBox.alert("RADIUS Test".t(), message);
        }, this), vm.get('settings'), username, password);
    }

});
