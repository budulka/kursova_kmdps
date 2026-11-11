package org.example.QueueingSystem.Routing;

import org.example.QueueingSystem.Element;

import java.util.function.BooleanSupplier;

public class Route {
    private final Element target;
    private final double weight;
    private boolean blocked;
    private BooleanSupplier condition = () -> true;

    Route(Element target, double weight) {
        if (target == null) {
            throw new IllegalArgumentException("Route target must not be null");
        }
        if (weight < 0) {
            throw new IllegalArgumentException("Route weight must be non-negative");
        }
        this.target = target;
        this.weight = weight;
    }

    public boolean isAvailable() {
        return !blocked && condition.getAsBoolean();
    }

    // Умова відкриття маршруту, перевіряється в момент вибору маршруту
    public void setCondition(BooleanSupplier condition) { this.condition = condition; }

    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    public Element getTarget() { return target; }
    public double getWeight() { return weight; }
}
