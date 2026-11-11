package org.example.QueueingSystem.Routing;


import java.util.List;

public class ProbabilityRoute implements RouteStrategy {

    @Override
    public Route select(List<Route> routes) {
        double sum = 0;
        for (Route route : routes) {
            if (route.isAvailable()) {
                sum += route.getWeight();
            }
        }
        if (sum <= 0) {
            return null;
        }

        double r = Math.random() * sum;
        double cumulative = 0.0;
        Route last = null;
        for (Route route : routes) {
            if (!route.isAvailable() || route.getWeight() <= 0) continue;
            cumulative += route.getWeight();
            last = route;
            if (r < cumulative) {
                return route;
            }
        }
        return last;
    }
}
