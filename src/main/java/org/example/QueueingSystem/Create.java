package org.example.QueueingSystem;

import org.example.QueueingSystem.ProbabilityDistributions.DelayGenerator;
import org.example.QueueingSystem.Routing.RouteStrategy;

public class Create extends Element {
    private final DelayGenerator delayGenerator;

    public Create(String name, DelayGenerator delayGenerator) {
        super(name);
        this.delayGenerator = delayGenerator;
        setTnext(0.0);
    }

    public Create(String name, DelayGenerator delayGenerator, RouteStrategy routeStrategy) {
        super(name, routeStrategy);
        this.delayGenerator = delayGenerator;
        setTnext(0.0);
    }
    public void setFirstArrival(double time) {
        setTnext(time);
    }

    @Override
    public void inAct() {
        throw new UnsupportedOperationException("Create cannot accept arrivals");
    }

    @Override
    public void outAct() {
        super.outAct();
        setTnext(getTcurr() + delayGenerator.getDelay());
        send();
    }
}
