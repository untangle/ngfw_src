Ext.define('Ung.overrides.String', {
    override: 'Ext.String',

    /**
     * Convert an array of strings to a single camel-case string.
     *
     * Example:
     * Ext.String.camelCase(['get','wanStatus'])
     * getWanStatus 
     */
    camelCase: function(stringArray){
        return stringArray.join(' ').replace(/(?:^\w|[A-Z]|\b\w|\s+)/g, function(match, index) {
            if (+match === 0){ 
                return "";
            } // or if (/\s+/.test(match)) for white spaces
            return index == 0 ? match.toLowerCase() : match.toUpperCase();
        });
    }
});