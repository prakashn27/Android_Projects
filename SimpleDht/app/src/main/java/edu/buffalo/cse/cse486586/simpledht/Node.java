package edu.buffalo.cse.cse486586.simpledht;

/**
 * Created by prakashn on 01/04/15.
 */
public class Node {
    String port = null;
    String hashValue = null;   //add the hash Value to the Node
    String curName = null;
    String nextName = null;   //Successor
    String prevName = null;   //predecessor

    public Node(String name, String port, String hashValue) {
        this.curName = name;
        this.port = port;
        this.hashValue = hashValue;
    }

    public String toString() {
        return "port:"+port+" name:"+curName;

    }
}
