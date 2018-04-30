package paxos;
import java.io.Serializable;

/**
 * Please fill in the data structure you use to represent the response message for each RMI call.
 * Hint: You may need a boolean variable to indicate ack of acceptors and also you may need proposal number and value.
 * Hint: Make it more generic such that you can use it for each RMI call.
 */
public class Response implements Serializable {
    static final long serialVersionUID=2L;
    // your data here
    boolean state;  //True = accept, False = reject
    int n;
    int na;
    Object v;
    Object va;

    // Your constructor and methods here
    public Response(boolean s, int n, int na, Object v, Object va){
        this.state = s;
        this.n = n;
        this.na = na;
        this.v = v;
        this.va = va;
    }


    @Override
    public String toString() {
        return "State:" + state + " n:" + n + " na:" + na + " v:" + v + " va:" + va;
    }
}
