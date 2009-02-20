
var PageName = 'Spam Blocker - Summary';
var PageId = 'p1f8cd5821e9b47d198021d79d35589de'
document.title = 'Spam Blocker - Summary';

if (top.location != self.location)
{
	if (parent.HandleMainFrameChanged) {
		parent.HandleMainFrameChanged();
	}
}

var $OnLoadVariable = '';

var $CSUM;

var hasQuery = false;
var query = window.location.hash.substring(1);
if (query.length > 0) hasQuery = true;
var vars = query.split("&");
for (var i = 0; i < vars.length; i++) {
    var pair = vars[i].split("=");
    if (pair[0].length > 0) eval("$" + pair[0] + " = decodeURI(pair[1]);");
} 

if (hasQuery && $CSUM != 1) {
alert('Prototype Warning: Variable values were truncated.');
}

function GetQuerystring() {
    return encodeURI('#OnLoadVariable=' + $OnLoadVariable + '&CSUM=1');
}

function PopulateVariables(value) {
  value = value.replace(/\[\[OnLoadVariable\]\]/g, $OnLoadVariable);
  value = value.replace(/\[\[PageName\]\]/g, PageName);
  return value;
}

function OnLoad() {

}

var u10 = document.getElementById('u10');

var u5 = document.getElementById('u5');
gv_vAlignTable['u5'] = 'top';
var u0 = document.getElementById('u0');

var u13 = document.getElementById('u13');

u13.style.cursor = 'pointer';
if (bIE) u13.attachEvent("onclick", Clicku13);
else u13.addEventListener("click", Clicku13, true);
function Clicku13(e)
{

if (true) {

	self.location.href="Spam Blocker - Email Summary.html" + GetQuerystring();

}

}
gv_vAlignTable['u13'] = 'top';
var u3 = document.getElementById('u3');
gv_vAlignTable['u3'] = 'top';
var u9 = document.getElementById('u9');

var u6 = document.getElementById('u6');

var u1 = document.getElementById('u1');

var u14 = document.getElementById('u14');
gv_vAlignTable['u14'] = 'top';
var u12 = document.getElementById('u12');
gv_vAlignTable['u12'] = 'top';
var u4 = document.getElementById('u4');

var u11 = document.getElementById('u11');
gv_vAlignTable['u11'] = 'top';
var u15 = document.getElementById('u15');
gv_vAlignTable['u15'] = 'top';
var u7 = document.getElementById('u7');

var u2 = document.getElementById('u2');
gv_vAlignTable['u2'] = 'top';
var u8 = document.getElementById('u8');

if (window.OnLoad) OnLoad();
