package org.example.QueueingSystem.Routing;

import org.example.QueueingSystem.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Router {
    private final List<Route> routes = new ArrayList<>();
    private final RouteStrategy strategy;

    public Router(RouteStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Route strategy must not be null");
        }
        this.strategy = strategy;
    }

    public Route add(Element target, double weight) {
        Route route = new Route(target, weight);
        routes.add(route);
        return route;
    }

    public Route find(Element target) {
        for (Route route : routes) {
            if (route.getTarget() == target) {
                return route;
            }
        }
        throw new IllegalArgumentException(target.getName() + " is not a next element");
    }

    public boolean isTerminal() {
        return routes.isEmpty();
    }

    public Element next() {
        Route route = strategy.select(routes);
        return route == null ? null : route.getTarget();
    }

    public List<Route> getRoutes() {
        return Collections.unmodifiableList(routes);
    }

    public RouteStrategy getStrategy() { return strategy; }
}
