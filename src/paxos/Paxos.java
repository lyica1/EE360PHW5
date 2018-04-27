package paxos;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is the main class you need to implement paxos instances.
 */
public class Paxos implements PaxosRMI, Runnable{

    ReentrantLock mutex;
    String[] peers; // hostname
    int[] ports; // host port
    int me; // index into peers[]

    Registry registry;
    PaxosRMI stub;

    AtomicBoolean dead;// for testing
    AtomicBoolean unreliable;// for testing

    // Your data here
    State s;
    Map<Integer, retStatus> seqval;
    Map<String, Map<Integer, Object>> thread;
    Map<Integer, Instance> instance;
    AtomicInteger n = new AtomicInteger(0);
    Map<Integer, Integer> done;
    /**
     * Call the constructor to create a Paxos peer.
     * The hostnames of all the Paxos peers (including this one)
     * are in peers[]. The ports are in ports[].
     */
    public Paxos(int me, String[] peers, int[] ports){

        this.me = me;
        this.peers = peers;
        this.ports = ports;
        this.mutex = new ReentrantLock();
        this.dead = new AtomicBoolean(false);
        this.unreliable = new AtomicBoolean(false);

        // Your initialization code here
        this.seqval = new HashMap<>();
        this.thread = new HashMap<>();
        this.instance = new HashMap<>();
        this.s = State.Pending;

        this.done = new HashMap<>();
        // register peers, do not modify this part
        try{
            System.setProperty("java.rmi.server.hostname", this.peers[this.me]);
            registry = LocateRegistry.createRegistry(this.ports[this.me]);
            stub = (PaxosRMI) UnicastRemoteObject.exportObject(this, this.ports[this.me]);
            registry.rebind("Paxos", stub);
        } catch(Exception e){
            e.printStackTrace();
        }
    }


    /**
     * Call() sends an RMI to the RMI handler on server with
     * arguments rmi name, request message, and server id. It
     * waits for the reply and return a response message if
     * the server responded, and return null if Call() was not
     * be able to contact the server.
     *
     * You should assume that Call() will time out and return
     * null after a while if it doesn't get a reply from the server.
     *
     * Please use Call() to send all RMIs and please don't change
     * this function.
     */
    public Response Call(String rmi, Request req, int id){
        Response callReply = null;

        PaxosRMI stub;
        try{
            Registry registry=LocateRegistry.getRegistry(this.ports[id]);
            stub=(PaxosRMI) registry.lookup("Paxos");
            if(rmi.equals("Prepare"))
                callReply = stub.Prepare(req);
            else if(rmi.equals("Accept"))
                callReply = stub.Accept(req);
            else if(rmi.equals("Decide"))
                callReply = stub.Decide(req);
            else
                System.out.println("Wrong parameters!");
        } catch(Exception e){
            return null;
        }
        return callReply;
    }


    /**
     * The application wants Paxos to start agreement on instance seq,
     * with proposed value v. Start() should start a new thread to run
     * Paxos on instance seq. Multiple instances can be run concurrently.
     *
     * Hint: You may start a thread using the runnable interface of
     * Paxos object. One Paxos object may have multiple instances, each
     * instance corresponds to one proposed value/command. Java does not
     * support passing arguments to a thread, so you may reset seq and v
     * in Paxos object before starting a new thread. There is one issue
     * that variable may change before the new thread actually reads it.
     * Test won't fail in this case.
     *
     * Start() just starts a new thread to initialize the agreement.
     * The application will call Status() to find out if/when agreement
     * is reached.
     */
    public void Start(int seq, Object value){
        // Your code here
        retStatus status = new retStatus(State.Pending, value);
        seqval.put(seq, status);
        instance.put(seq, new Instance(-1, -1, -1));
        InnerPaxos temp = new InnerPaxos(seq, value);
        Thread t = new Thread(temp);
        t.start();
    }
    @Override
    public void run(){
        //Your code here
    }
    public Response Callitself(Request req, String name){
        if(name.equals("Prepare")){
            return Prepare(req);
        }else if(name.equals("Accept")){
            return Accept(req);
        }else if(name.equals("Decide")){
            return Decide(req);
        }
        return null;
    }
    // RMI handler
    public Response Prepare(Request req){
        mutex.lock();
        int seq = req.seq;
        Instance ins = instance.get(seq);
        retStatus sta = seqval.get(seq);
        Response res;
        if(req.number > ins.n_p){
            res = new Response(true, seq, req.number, ins.n_p, sta.v);
            ins.n_p = req.number;
            instance.put(seq, ins);
        }else{
            res = new Response(false, seq, req.number, ins.n_p, sta.v);
        }
        // your code here
        mutex.unlock();
        return res;
    }

    public Response Accept(Request req){
        mutex.lock();
        int seq = req.seq;
        Instance ins = instance.get(seq);
        retStatus sta = seqval.get(seq);
        Response res;
        if(req.number >= ins.n_p){
            res = new Response(true, seq, req.number, ins.n_p, sta.v);
            ins.n_p = req.number;
            ins.n_a = req.number;
            ins.v_a = req.v;
        }else{
            res = new Response(false, seq, req.number, ins.n_p, sta.v);
        }
        // your code here
        return res;
    }

    public Response Decide(Request req){
        mutex.lock();
        int seq = req.seq;
        retStatus changestate = seqval.get(seq);
        if(changestate.state == State.Decided){

        }
        changestate.state = State.Decided;
        seqval.put(seq, changestate);
        mutex.unlock();
        // your code here
        return null;
    }

    /**
     * The application on this machine is done with
     * all instances <= seq.
     *
     * see the comments for Min() for more explanation.
     */
    public void Done(int seq) {
        // Your code here
        mutex.lock();
        done.put(me, seq);
        mutex.unlock();
    }


    /**
     * The application wants to know the
     * highest instance sequence known to
     * this peer.
     */
    public int Max(){
        // Your code here
        return 0;
    }

    /**
     * Min() should return one more than the minimum among z_i,
     * where z_i is the highest number ever passed
     * to Done() on peer i. A peers z_i is -1 if it has
     * never called Done().

     * Paxos is required to have forgotten all information
     * about any instances it knows that are < Min().
     * The point is to free up memory in long-running
     * Paxos-based servers.

     * Paxos peers need to exchange their highest Done()
     * arguments in order to implement Min(). These
     * exchanges can be piggybacked on ordinary Paxos
     * agreement protocol messages, so it is OK if one
     * peers Min does not reflect another Peers Done()
     * until after the next instance is agreed to.

     * The fact that Min() is defined as a minimum over
     * all Paxos peers means that Min() cannot increase until
     * all peers have been heard from. So if a peer is dead
     * or unreachable, other peers Min()s will not increase
     * even if all reachable peers call Done. The reason for
     * this is that when the unreachable peer comes back to
     * life, it will need to catch up on instances that it
     * missed -- the other peers therefore cannot forget these
     * instances.
     */
    public int Min(){
        // Your code here
        return 0;
    }



    /**
     * the application wants to know whether this
     * peer thinks an instance has been decided,
     * and if so what the agreed value is. Status()
     * should just inspect the local peer state;
     * it should not contact other Paxos peers.
     */
    public retStatus Status(int seq){
        // Your code here
        if(s == State.Pending){
            return new retStatus(State.Pending, null);
        }
        return null;
    }

    /**
     * helper class for Status() return
     */
    public class retStatus{
        public State state;
        public Object v;

        public retStatus(State state, Object v){
            this.state = state;
            this.v = v;
        }
    }

    /**
     * Tell the peer to shut itself down.
     * For testing.
     * Please don't change these four functions.
     */
    public void Kill(){
        this.dead.getAndSet(true);
        if(this.registry != null){
            try {
                UnicastRemoteObject.unexportObject(this.registry, true);
            } catch(Exception e){
                System.out.println("None reference");
            }
        }
    }

    public boolean isDead(){
        return this.dead.get();
    }

    public void setUnreliable(){
        this.unreliable.getAndSet(true);
    }

    public boolean isunreliable(){
        return this.unreliable.get();
    }
    public class InnerPaxos implements Runnable{
        public int seq;
        public Object value;

        public InnerPaxos(int seq, Object value){
            this.seq = seq;
            this.value = value;
        }
        @Override
        public void run(){
            int count = 0;
            int max = -1;
            int currentn = n.incrementAndGet();
            int request = this.seq;
            retStatus status = seqval.get(request);
            Instance ins = instance.get(request);
            Request req = new Request(request, status.v, currentn);
            Response res;
            Object value = status.v;
            for(;;){
                res = Callitself(req, "Prepare");
                if(res.response){
                    count++;
                    if(res.n_a > max){
                        value = res.v_a;
                        max = res.n_a;
                    }
                }
                for(int i : ports) {
                    res = Call("Prepare", req, i);
                    if(res.response){
                        count++;
                        if(res.n_a > max){
                            value = res.v_a;
                            max = res.n_a;
                        }
                    }
                }
                if(count >= (peers.length/2) +1){ //you got majority true
                    count = 0;
                    Request req1 = new Request(request, value, currentn);
                    res = Callitself(req1, "Accept");
                    if(res.response){
                        count++;
                    }
                    for(int i : ports){
                        res = Call("Accept", req1, i);
                        if(res.response){
                            count++;
                        }
                    }
                    if(count >= (peers.length/2) + 1){
                        res = Callitself(req1, "Decide");
                        for(int i : ports){
                            res = Call("Decide", req1, i);
                        }
                    }
                }
            }
        }
    }

}
