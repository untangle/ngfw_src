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
        name: 'power',
        type: 'boolean',
        defaultValue: false
    }, {
        name: 'colorCls',
        type: 'string',
        convert: function(v, record){
            if(record.get('inconsistent')){
                return 'fa-red';
            }else if(record.get('power')){
                return 'fa-orange';
            }else if(record.get('on')){                    
                return 'fa-green';
            }else{
                return 'fa-gray';
            }
        }
    }, {
        name: 'status',
        type: 'string',
        convert: function(v, record){
            if(record.get('on')){
                if(record.get('power')){
                    return 'Powering on'.t();
                }else if(record.get('inconsistent')){
                    return 'Enabled but is not active'.t();
                }else{
                    return 'Enabled'.t();
                }
            }else{
                if(record.get('power')){
                    return 'Powering off'.t();
                }else if(record.get('inconsistent')){
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
        if( ( this.vm && !Util.isDestroyed(this.vm) ) || 
            ( this.instance && !Util.isDestroyed(this.instance)) ){
            var targetState = this.vm ? this.vm.get('instance.targetState') : this.instance.targetState;
            var runState = this.app.getRunState();
            var daemonRunning = (this.vm && this.vm.get('props.daemon') != null) ? Rpc.directData('rpc.UvmContext.daemonManager.isRunning', this.vm.get('props.daemon') ) : true;

            on = ( runState == 'RUNNING' );
            inconsistent = (targetState != runState) || (runState == 'RUNNING' && !daemonRunning);
        }

        this.set('on', on);
        this.set('inconsistent', inconsistent);
        this.set('power', false);
        this.set('colorCls', on);
        this.set('status', on);
    },
    vm: null,
    instance: null,
    constructor: function( args, session){
        if(args['vm']){
            this.vm = args['vm'];            
        }else if( args['instance'] ){
            this.instance = args['instance'];
        }
        this.app = args['app'];
        this.callParent([{}], session);
        this.detect();
    },
});
