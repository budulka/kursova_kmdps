package org.example;

import org.example.QueueingSystem.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Протокол подій: стан мережі після кожного моменту, в який відбулась подія
public class EventLog {
    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        double g1 = args.length > 0 ? Double.parseDouble(args[0]) : 45;
        double g2 = args.length > 1 ? Double.parseDouble(args[1]) : 65;
        double time = args.length > 2 ? Double.parseDouble(args[2]) : 240;
        TrafficModel m = new TrafficModel(g1, g2, Main.BASE_SEED);

        System.out.println("     t, с | Фаза | Q1 F1 P1 | Q2 F2 P2 |  D | Подія");
        m.model.setEventListener((t, fired) -> {
            List<String> events = new ArrayList<>();
            for (Element e : fired) {
                String d = describe(m, e);
                if (d != null) events.add(d);
            }
            if (events.isEmpty()) return; // лише миттєвий перехід через F1/F2 — окремо не виводимо
            System.out.printf("%9.2f | %-4s | %2d %2d %2d | %2d %2d %2d | %2d | %s%n",
                    t, phase(m), m.f1.getQueue(), busy(m.f1), busy(m.p1),
                    m.f2.getQueue(), busy(m.f2), busy(m.p2), m.repair.getChannelsUsed(),
                    String.join("; ", events));
        });
        m.run(time);
    }

    static String phase(TrafficModel m) {
        if (m.green1.isBusy()) return "G1";
        if (m.red1.isBusy()) return "R1";
        if (m.green2.isBusy()) return "G2";
        return "R2";
    }

    static int busy(org.example.QueueingSystem.Process p) {
        return p.getChannelsUsed();
    }

    static String describe(TrafficModel m, Element e) {
        if (e == m.in1) return "прибуття машини напрямку 1";
        if (e == m.in2) return "прибуття машини напрямку 2";
        if (e == m.p1) return "P1: машина 1 в'їхала на ділянку";
        if (e == m.p2) return "P2: машина 2 в'їхала на ділянку";
        if (e == m.repair) return "D: машина залишила ділянку";
        if (e == m.green1) return "кінець G1, червоне";
        if (e == m.red1) return "кінець R1, зелене напрямку 2";
        if (e == m.green2) return "кінець G2, червоне";
        if (e == m.red2) return "кінець R2, зелене напрямку 1";
        return null; // F1/F2 мають нульову затримку і окремої події не утворюють
    }
}
