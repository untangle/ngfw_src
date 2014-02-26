/**
 * $Id$
 */
import com.untangle.jvector.*;
import java.util.LinkedList;

public class Test2
{
    private static final int NUMTRAN = 200;
    private static final int NUMTRIP = 500;
    
    private static class Counter implements SocketQueueListener {
        private OutgoingSocketQueue last;
        private IncomingSocketQueue first;

        private int count = 0;
        
        public Counter(IncomingSocketQueue first, OutgoingSocketQueue last)
        {
            this.first = first;
            this.last  = last;
            last.sq().registerListener(this);
        }

        public void event( IncomingSocketQueue in, OutgoingSocketQueue out )
        {
            if (count < NUMTRIP) {
                count++;
                Crumb obj = first.read();
                last.write(obj);
                if (( count % 50) == 0 )
                    System.out.print(".");
            }            
        }

        public void event( IncomingSocketQueue in )
        {
            if (count < NUMTRIP) {
                count++;
                Crumb obj = first.read();
                last.write(obj);
                if (( count % 50) == 0 )
                    System.out.print(".");
            }            
        }

        public void event( OutgoingSocketQueue out )
        {
            /* Nothing to do on an outgoing transform event */
        }


        public void event( SocketQueue o )
        {
            if (count < NUMTRIP) {
                count++;
                Crumb obj = first.read();
                last.write(obj);
                if (count%50 == 0)
                    System.out.print(".");
            }
        }
    }

    public static void main (String[] args)
    {
        IncomingSocketQueue[] incoming = new IncomingSocketQueue[NUMTRAN];
        OutgoingSocketQueue[] outgoing = new OutgoingSocketQueue[NUMTRAN];
        Relay[] relay = new Relay[NUMTRAN];
        Transform[] transform = new Transform[NUMTRAN];
        LinkedList relays = new LinkedList();
        int i;
        
        for (i=0;i<NUMTRAN;i++) {
            incoming[i] = new IncomingSocketQueue();
            outgoing[i] = new OutgoingSocketQueue();
            transform[i] = new Transform(incoming[i], outgoing[i]);
            transform[i].name(new String ("Transform" + i));
        }
        for (i=0;i<NUMTRAN-1;i++) {
            relay[i] = new Relay(outgoing[i],incoming[i+1]);
            relays.add(relay[i]);
        }

        Counter cou   = new Counter(incoming[0],outgoing[NUMTRAN-1]);
        Crumb  obj    = new DataCrumb( new byte[] { 1 } );
        // Object obj    = new Integer(1);

        Vector vec    = new Vector(relays);
        vec.timeout(1);
        
        System.out.println("Sending Object: " + obj);
        System.out.print("Counters: ");

        long start = System.currentTimeMillis();
        
        incoming[0].sq().add(obj);
        vec.vector();

        long stop = System.currentTimeMillis();

        System.out.print("\n");

        System.out.println(NUMTRIP + " trips x " + NUMTRAN +" transforms:" + ((float)(stop-start-1000))/((float)NUMTRIP) + "msec/trip");
        return;
    }

    public void event ( SocketQueue sq )
    {
        System.out.println("Got     Object: " + sq );
    }

    static {
        System.loadLibrary("uvmcore");
    }
}
