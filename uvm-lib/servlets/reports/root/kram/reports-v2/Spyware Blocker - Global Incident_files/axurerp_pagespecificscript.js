
var PageName = 'Spyware Blocker - Global Incident';
var PageId = 'pb4be64a1208647c8a33c85d1f8c6f78a'
document.title = 'Spyware Blocker - Global Incident';

if (top.location != self.location)
{
	if (parent.HandleMainFrameChanged) {
		parent.HandleMainFrameChanged();
	}
}

if (window.OnLoad) OnLoad();
