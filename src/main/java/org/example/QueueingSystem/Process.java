package org.example.QueueingSystem;

import org.example.QueueingSystem.ProbabilityDistributions.DelayGenerator;
import org.example.QueueingSystem.Routing.ProbabilityRoute;
import org.example.QueueingSystem.Routing.RouteStrategy;

import java.util.Arrays;

public class Process extends Element {
    private final int maxQueue;
    private final int channels;
    private final DelayGenerator delayGenerator;
    private final double[] tnext;
    private final boolean[] blocked;
    private int queue;
    private int channelsUsed;
    private int failure;
    private double meanQueue;
    private double meanBusy;

    public Process(String name, int maxQueue, int channels, DelayGenerator delayGenerator) {
        this(name, maxQueue, channels, delayGenerator, new ProbabilityRoute());
    }

    public Process(String name, int maxQueue, int channels, DelayGenerator delayGenerator, RouteStrategy routeStrategy) {
        super(name, routeStrategy);
        this.maxQueue = maxQueue;
        this.channels = channels;
        this.delayGenerator = delayGenerator;
        this.tnext = new double[channels];
        this.blocked = new boolean[channels];
        Arrays.fill(tnext, Double.MAX_VALUE);
    }

    @Override
    public void inAct() {
        registerArrival();
        if (channelsUsed < channels) {
            startService();
        } else if (queue < maxQueue) {
            queue++;
        } else {
            failure++;
        }
    }

    @Override
    public void outAct() {
        double now = getTcurr();
        for (int i = 0; i < channels; i++) {
            if (tnext[i] == now) {
                tnext[i] = Double.MAX_VALUE;
                blocked[i] = true;
            }
        }
        tryUnblock();
    }

    @Override
    public boolean tryUnblock() {
        boolean moved = false;
        for (int i = 0; i < channels; i++) {
            if (!blocked[i]) continue;
            blocked[i] = false;
            channelsUsed--;
            if (send()) {
                incrementQuantity();
                moved = true;
                if (queue > 0) {
                    queue--;
                    startService();
                }
            } else {
                blocked[i] = true;
                channelsUsed++;
            }
        }
        return moved;
    }

    private void startService() {
        channelsUsed++;
        tnext[findFreeChannel()] = getTcurr() + delayGenerator.getDelay();
    }

    @Override
    public double getTnext() {
        double min = Double.MAX_VALUE;
        for (double t : tnext) {
            if (t < min) min = t;
        }
        return min;
    }

    @Override
    public boolean isFull() {
        return channelsUsed == channels && queue >= maxQueue;
    }

    public boolean hasFreeChannel() {
        return channelsUsed < channels;
    }

    public boolean isBusy() {
        return channelsUsed > 0;
    }

    @Override
    public void doStatistics(double delta) {
        meanQueue += queue * delta;
        meanBusy += channelsUsed * delta;
    }

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.println("  failure = " + failure + ", queue = " + queue);
    }

    @Override
    public void printResult(double time) {
        System.out.printf("%s: arrivals = %d, served = %d, failure = %d, mean queue = %.4f, mean busy = %.4f%n",
                getName(), getArrivals(), getQuantity(), failure, meanQueue / time, meanBusy / time);
    }

    private int findFreeChannel() {
        for (int i = 0; i < channels; i++) {
            if (tnext[i] == Double.MAX_VALUE && !blocked[i]) {
                return i;
            }
        }
        throw new IllegalStateException("No free channel found");
    }

    public int getQueue() { return queue; }
    public int getChannelsUsed() { return channelsUsed; }
    public int getFailure() { return failure; }
    public double getMeanQueue() { return meanQueue; }
    public double getMeanBusy() { return meanBusy; }
    public int getChannels() { return channels; }
    public int getMaxQueue() { return maxQueue; }
}
