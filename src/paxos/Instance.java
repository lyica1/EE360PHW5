package paxos;

import java.util.ArrayList;
import java.util.List;

public class Instance {
    public int n_p;
    public int n_a;
    public Object v_a;
    public Instance(int p, int a, Object value){
        n_p = p;
        n_a = a;
        v_a = value;
    }
}
