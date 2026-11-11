package org.example.QueueingSystem.Routing;


import java.util.List;

public interface RouteStrategy {
    Route select(List<Route> routes);
}
