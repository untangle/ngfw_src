// instances map of i18n objects for nodes 
Ung.i18nNodeInstances={};

//I18N object class
Ung.I18N=Ext.extend(Ext.Component, {
	//map of translations
	map: null,
    //initialize I18N component
    initComponent: function(){
        Ung.I18N.superclass.initComponent.call(this);
		if (this.map == null) {
			this.map = {};
		}
		if (!this.map['decimal_sep']) {
			this.map['decimal_sep'] = '.';
		}
		if (!this.map['thousand_sep']) {
			this.map['thousand_sep'] = ',';
		}
		if (!this.map['date_fmt']) {
			this.map['date_fmt'] = 'm/d/y';
		}
		if (!this.map['timestamp_fmt']) {
			this.map['timestamp_fmt'] = 'Y-m-d g:i:s a';
		}
    },
	// translation function
	_: function (s) {
		if (this.map!== null && this.map[s]) {
			return this.map[s];
		}
		return s;
	},
	// pluralize function
	pluralise: function (s, p, n) {
		if (n != 1) return this._(p);
		return this._(s);
	},
	// replaces arguments with values, for a string with arguments
	sprintf: function (s) {
		var bits = s.split('%');
		var out = bits[0];
		var re = /^([ds])(.*)$/;
		for (var i=1; i<bits.length; i++) {
			p = re.exec(bits[i]);
			if (!p || arguments[i]==null) continue;
			if (p[1] == 'd') {
				out += parseInt(arguments[i], 10);
			} else if (p[1] == 's') {
				out += arguments[i];
			}
			out += p[2];
		}
		return out;
	},
	
	//formats a number with local separators
    numberFormat: function(v) {
        v = parseInt(v,10) + '';
        var x = v.split(this.map['decimal_sep']);
        var x1 = x[0];
        var x2 = x.length > 1 ? this.map['decimal_sep'] + x[1] : '';
        var rgx = /(d+)(d{3})/;
        while (rgx.test(x1)) {
            x1 = x1.replace(rgx, '$1' + this.map['thousand_sep'] + '$2');
        }
        return x1 + x2;
    }, 
    // formats a date
    dateFormat: function(v) {
    	return Ext.util.Format.date(v, this.map['date_fmt']);
    },
    //formats a timestamp
    timestampFormat: function(v) {
    	var date = new Date();
    	date.setTime(v.time);
    	return Ext.util.Format.date(date, this.map['timestamp_fmt']);
    }

});

//node I18N class object
Ung.NodeI18N=Ext.extend(Ung.I18N, {
	//node map
	nodeMap: null,
	//translation function tryes to find the word in the nodeMap
	// and if not succesful it tries to find it in the global map
	_: function (s) {
		if (this.nodeMap!== null && this.nodeMap[s]) {
			return this.nodeMap[s];
		}
		if (this.map!== null && this.map[s]) {
			return this.map[s];
		}
		return s;
	}
});