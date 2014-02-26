/**
 * $Id$
 */
import com.untangle.jvector.*;

public class Transform implements SocketQueueListener
{
    public final static int C2S = 0;
    public final static int S2C = 1;

    private IncomingSocketQueue c2s_r;
    private IncomingSocketQueue s2c_r;
    private OutgoingSocketQueue c2s_w;
    private OutgoingSocketQueue s2c_w;

    private String name = "UnamedPlayer";
    private boolean verbose = true;

    public Transform (IncomingSocketQueue c2s_r, OutgoingSocketQueue c2s_w, IncomingSocketQueue s2c_r, OutgoingSocketQueue s2c_w)
    {
        this.c2s_r = c2s_r;
        this.c2s_w = c2s_w;
        this.s2c_r = s2c_r;
        this.s2c_r = s2c_r;

        c2s_r.sq().registerListener(this);
        c2s_r.sq().attach(c2s_w);
        s2c_r.sq().registerListener(this);
        s2c_r.sq().attach(s2c_w);
    }

    public Transform (IncomingSocketQueue c2s_r, OutgoingSocketQueue c2s_w)
    {
        this.c2s_r = c2s_r;
        this.c2s_w = c2s_w;
        this.s2c_r = null;
        this.s2c_r = null;

        c2s_r.sq().registerListener(this);
        c2s_r.sq().attach(c2s_w);
    }

    public void event( IncomingSocketQueue in, OutgoingSocketQueue out )
    {
        /* This is unused */
    }

    public void event( IncomingSocketQueue in )
    {
        OutgoingSocketQueue out = (OutgoingSocketQueue)in.sq().attachment();

        Crumb obj = in.read();

        if (this.verbose)
            System.out.println("TRAN: Transform \"" + name + "\" passing: " + obj);

        out.write(obj);
    }

    public void event( OutgoingSocketQueue out )
    {
        /* Not much to do on these events */
    }


    //     public void event( SocketQueue o )
    //     {
    //         IncomingSocketQueue in = (IncomingSocketQueue)o;
    //         OutgoingSocketQueue out = (OutgoingSocketQueue)in.sq().attachment();
    //         Crumb obj = in.read();

    //         if (this.verbose)
    //             System.out.println("TRAN: Transform \"" + name + "\" passing: " + obj);

    //         out.write(obj);
    //     }

    public void name (String s)
    {
        this.name = s;
    }

    public void verbose (boolean b)
    {
        this.verbose = b;
    }
}



