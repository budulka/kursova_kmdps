package org.example.QueueingSystem.Routing;


import java.util.List;

public class PriorityRoute implements RouteStrategy {

    @Override
    public Route select(List<Route> routes) {
        Route best = null;
        Route bestFree = null;
        for (Route route : routes) {
            if (!route.isAvailable()) continue;
            if (best == null || route.getWeight() > best.getWeight()) {
                best = route;
            }
            if (!route.getTarget().isFull() && (bestFree == null || route.getWeight() > bestFree.getWeight())) {
                bestFree = route;
            }
        }
        return bestFree != null ? bestFree : best;
    }
}
