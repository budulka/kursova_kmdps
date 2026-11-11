package org.example.QueueingSystem;

import java.util.ArrayList;
import java.util.List;

public class Model {

    public interface EventListener {
        void onEvent(double time, List<Element> fired);
    }

    private final List<Element> elements;
    private double tcurr;
    private EventListener listener;

    public Model(List<Element> elements) {
        this.elements = elements;
    }

    public void simulate(double timeModeling) {
        while (tcurr < timeModeling) {
            double tnext = Double.MAX_VALUE;
            for (Element e : elements) {
                tnext = Math.min(tnext, e.getTnext());
            }
            tnext = Math.min(tnext, timeModeling);

            for (Element e : elements) {
                e.doStatistics(tnext - tcurr);
            }
            tcurr = tnext;
            for (Element e : elements) {
                e.setTcurr(tcurr);
            }

            List<Element> fired = new ArrayList<>();
            for (Element e : elements) {
                if (e.getTnext() == tcurr) {
                    fired.add(e);
                    e.outAct();
                }
            }

            boolean moved;
            do {
                moved = false;
                for (Element e : elements) {
                    moved |= e.tryUnblock();
                }
            } while (moved);

            if (listener != null) {
                listener.onEvent(tcurr, fired);
            }
        }
    }

    public void setEventListener(EventListener listener) { this.listener = listener; }

    public void printResult() {
        for (Element e : elements) {
            e.printResult(tcurr);
        }
    }

    public double getTcurr() { return tcurr; }
}
