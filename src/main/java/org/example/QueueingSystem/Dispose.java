package org.example.QueueingSystem;

public class Dispose extends Element {

    public Dispose(String name) {
        super(name);
    }

    public Dispose() {
        this("Dispose");
    }

    @Override
    public void inAct() {
        registerArrival();
        incrementQuantity();
    }

    @Override
    public void printResult(double time) {
        System.out.printf("%s: disposed = %d, mean interval between arrivals = %.4f%n",
                getName(), getQuantity(), getMeanArrivalInterval());
    }
}
