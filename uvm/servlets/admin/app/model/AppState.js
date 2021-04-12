Ext.define ('Ung.model.AppState', {
    extend: 'Ext.data.Model' ,
    fields: [{
        name: 'on',
        type: 'boolean',
        defaultValue: false
    }, {
        name: 'inconsistent',
        type: 'boolean',
        defaultValue: false
    }, {
        name: 'name',
        type: 'string'
    }, {
        name: 'expired',
        type: 'boolean',
        defaultValue: false
    },{
        name: 'power',
        type: 'boolean',
        defaultValue: false
    }, {
        name: 'colorCls',
        type: 'string',
        calculate: function(data){
            if(data.inconsistent || data.expired){
                return 'fa-red';
            }else if(data.power){
                return 'fa-orange';
            }else if(data.on){
                return 'fa-green';
            }else{
                return 'fa-gray';
            }
        }
    }, {
        name: 'status',
        type: 'string',
        calculate: function(data){
            if(data.on){
                if(data.power){
                    return 'Powering on'.t();
                }else if(data.inconsistent){
                    return 'Enabled but is not active'.t();
                }else{
                    return 'Enabled'.t();
                }
            }else{
                if(data.power){
                    return 'Powering off'.t();
                }else if(data.expired){
                    return 'Disabled, license is invalid or expired'.t();
                }else if(data.inconsistent){
                    return 'Disabled but active'.t();
                }else{
                    return 'Disabled'.t();
                }
            }
        }
    }],
    detect: function(){
        var on = false;
        var inconsistent = false;
        var licenseExpired;
        if( ( this.vm && !Util.isDestroyed(this.vm) ) || 
            ( this.instance && !Util.isDestroyed(this.instance)) ){
            var targetState = this.vm ? this.vm.get('instance.targetState') : this.instance.targetState;

            var runState = this.app ? this.app.getRunState() : ( this.vm ? this.vm.get('runState') : this.instance.runState );

            on = ( runState == 'RUNNING' );
            var daemonRunning = (on && this.vm && this.vm.get('props.daemon') != null) ? Rpc.directData('rpc.UvmContext.daemonManager.isRunning', this.vm.get('props.daemon') ) : true;

            inconsistent = (targetState != runState) || (runState == 'RUNNING' && !daemonRunning);

            var appName = this.vm.get('app.name') ? this.vm.get('app.name'): this.vm.get('urlName');
            licenseExpired = !Rpc.directData('rpc.UvmContext.licenseManager.isLicenseValid', appName); 
        }
        this.set({
            'expired': licenseExpired,
            'on': on,
            'inconsistent': inconsistent,
            'power': false
        });
    },
    vm: null,
    instance: null,
    app: null,
    constructor: function( args, session){
        if(args['vm']){
            this.vm = args['vm'];            
        }else if( args['instance'] ){
            this.instance = args['instance'];
        }
        if(args.app){
            this.app = args['app'];
        }
        this.callParent([{}], session);
        this.detect();
        console.log("constructor END ");
    },
});
