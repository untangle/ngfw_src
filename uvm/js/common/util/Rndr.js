/**
 * new renderer Class
 * should replace the current Renderer.js (still kept for backward compatibility)
 *
 * renderers are used when a specific value needs to be represented in a different way
 * or some styles/markup are applied to the value
 * e.g. bytes rendered as Kb, Mb etc...
 * the sorting/filtering still remains done by the bytes value,
 * the value is just represented in a more user readable way
 */
Ext.define('Ung.util.Rndr', {
    singleton: true,
    alternateClassName: 'Rndr',

    // predefine columns widths, to add more
    colW: {
        boolean: 80
    },

    // columns filter types
    filters: {
        boolean: { type: 'boolean', yesText: 'true'.t(), noText: 'false'.t() },
        numeric: { type: 'numeric' },
        string: { type: 'string' },
        date: { type: 'date' },
    },


    boolean: function (value) {
        return (value == true || value == 'true') ? 'true' : 'false';
    },

    memory: function (value) {
        var meg = value/1024/1024;
        return (Math.round( meg*10 )/10).toString() + ' MB';
    },

    disk: function (value) {
        var gig = value/1024/1024/1024;
        return (Math.round( gig*10 )/10).toString() + ' GB';
    },

    localLogin: function (value) {
        return value ? 'local'.t() : 'remote'.t();
    },

    successLogin: function (value) {
        return value ? 'success'.t() : 'failed'.t();
    },

    settingsFile: function (value) {
        value = value.replace( /^.*\/settings\//, '' );
        value = value.replace( /^.*\/conf\//, '' );
        return value;
    },

    adBlockerAction: function (value) {
        if (Ext.isEmpty(value)) { return ''; }
        if (value === 'B') {
            return 'block'.t();
        }
        return 'pass'.t();
    },

    // use renderer because have to be sorted by value and not by text
    priority: function (value) {
        return Map.priorities[value] || value;
    },

    bandwidthControlRule: function (value) {
        return value || 'none'.t();
    },

    datasize: function (value) {
        var sizes = [
                [ 1125899906842624, 'PB'.t() ],
                [ 1099511627776, 'TB'.t() ],
                [ 1073741824, 'GB'.t() ],
                [ 1048576, 'MB'.t() ] ,
                [ 1024, 'KB'.t() ],
                [ 1, 'B'.t() ]
            ],
            size = sizes[sizes.length-1];

        if (value === null) { value = 0; }
        value = parseInt( value, 10 );
        for (var i = 0; i < sizes.length; i++){
            size = sizes[i];
            if (value >= size[0] || value <= (0-size[0])){
                break;
            }
        }
        if ((value == 0 ) || (size[0] == 1)){
            return value + ' ' + size[1];
        } else {
            var dividedValue = (value / size[0]).toFixed(2).toString();
            if (dividedValue.substring(dividedValue.length - 3) == '.00') {
                dividedValue = dividedValue.substring(0, dividedValue.length - 3);
            }
            return dividedValue + ' ' + size[1];
        }
    },


});
