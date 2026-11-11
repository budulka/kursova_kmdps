package org.example.QueueingSystem;

import org.example.QueueingSystem.Routing.ProbabilityRoute;
import org.example.QueueingSystem.Routing.Route;
import org.example.QueueingSystem.Routing.RouteStrategy;
import org.example.QueueingSystem.Routing.Router;

public abstract class Element {
    private static int nextId = 0;

    private final int id;
    private String name;
    private double tnext;
    private double tcurr;
    private int quantity;

    private final Router router;

    private int arrivals;
    private double lastArrivalTime;
    private double sumArrivalIntervals;

    public Element(String name) {
        this(name, new ProbabilityRoute());
    }

    public Element(String name, RouteStrategy routeStrategy) {
        this.id = nextId++;
        this.name = name;
        this.router = new Router(routeStrategy);
        this.tnext = Double.MAX_VALUE;
        this.tcurr = 0.0;
        this.quantity = 0;
    }


    public Route addNext(Element next, double weight) {
        return router.add(next, weight);
    }

    public Route setNext(Element next) {
        return addNext(next, 1.0);
    }

    public void setBlock(Element el, boolean status) {
        router.find(el).setBlocked(status);
    }



    protected boolean send() {
        if (router.isTerminal()) {
            return true;
        }
        Element next = router.next();
        if (next == null) {
            return false;
        }
        next.inAct();
        return true;
    }


    public abstract void inAct();

    public void outAct() {
        quantity++;
    }

    public boolean tryUnblock() {
        return false;
    }

    protected void incrementQuantity() {
        quantity++;
    }

    protected void registerArrival() {
        if (arrivals > 0) {
            sumArrivalIntervals += tcurr - lastArrivalTime;
        }
        lastArrivalTime = tcurr;
        arrivals++;
    }

    public boolean isFull() {
        return false;
    }

    public void doStatistics(double delta) {
    }

    public void printInfo() {
        System.out.println(name + " quantity=" + quantity + " tnext=" + getTnext());
    }

    public void printResult(double time) {
        System.out.println(name + " quantity = " + quantity);
    }

    public double getMeanArrivalInterval() {
        return arrivals > 1 ? sumArrivalIntervals / (arrivals - 1) : 0.0;
    }

    public Router getRouter() { return router; }
    public int getArrivals() { return arrivals; }
    public double getTnext() { return tnext; }
    public void setTnext(double tnext) { this.tnext = tnext; }
    public double getTcurr() { return tcurr; }
    public void setTcurr(double tcurr) { this.tcurr = tcurr; }
    public int getQuantity() { return quantity; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getId() { return id; }
}
