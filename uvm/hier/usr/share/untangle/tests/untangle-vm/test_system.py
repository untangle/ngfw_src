import commands

def test_kernel_version():
    assert commands.getoutput( "uname -a" ).find( "untangle" ) >= -1


