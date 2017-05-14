Ext.define('Ung.cmp.GridRenderers', {
    singleton: true,
    alternateClassName: 'Renderer',

    boolean: function( value ){
        return '<i class="fa ' + (value ? 'fa-check' : 'fa-minus') + '"></i>';
    },

    timestamp: function( value ){
        if( !value ){
            return '<i class="fa fa-minus"></i>';
        }
        return Ext.util.Format.date(new Date( value ), 'timestamp_fmt'.t());

    },

    interface: function( value ){
        var interfaceName = Util.interfacesListNamesMap()[value];
        if (interfaceName) {
            return interfaceName + ' [' + value + ']';
        }
        return '';
    },

    tags: function( value ){
        if( value != null && value != "" ){
            if( typeof(value) == 'string' ){
                value = Ext.decode( value );
            }
            if( value &&
                 value.list ){
                return value.list.join(', ');
            }
        }
        return '';
    },

    policy: function ( value ) {
        var policy = Ext.getStore('policiestree').findRecord('policyId', value);
        if (policy) {
            return policy.get('name') + ' [' + value + ']';
        }
        return value;
    },

    datasizeMap: [
        [ 1125899906842624, 'PB'.t() ],
        [ 1099511627776, 'TB'.t() ],
        [ 1073741824, 'GB'.t() ],
        [ 1048576, 'MB'.t() ] ,
        [ 1024, 'KB'.t() ],
        [ 1, 'B'.t() ]
    ],
    datasize: function( value ){
        // walk map looking at key.  If larger then divide and use units
        value = parseInt( value, 10 );
        var size;
        for( var i = 0; i < this.datasizeMap.length; i++){
            size = this.datasizeMap[i];
            if( value >= size[0] ){
                break;
            }
        }
        return ( value == 0 ? 0 : ( value / size[0] ).toFixed(2) ) + ' ' + size[1];
    },

    timeIntervalMap: {
        86400: 'Daily'.t(),
        604800: 'Weekly'.t(),
        2419200: 'Monthly'.t()
    },
    timeInterval: function ( value ){
        if( value in this.timeIntervalMap ){
            return this.timeIntervalMap[value];
        }
        return value;
    },

    dayOfWeekMap: {
        0: 'Sunday'.t(),
        1: 'Monday'.t(),
        2: 'Tuesday'.t(),
        3: 'Wednesday'.t(),
        4: 'Thursday'.t(),
        5: 'Friday'.t(),
        6: 'Saturday'.t()
    },
    dayOfWeek: function( value ){
        if( value in this.dayOfWeeklMap ){
            return this.dayOfWeekMap[value];
        }
        return value;
    }

});
