package org.example;

import org.example.QueueingSystem.Create;
import org.example.QueueingSystem.Dispose;
import org.example.QueueingSystem.Model;
import org.example.QueueingSystem.Process;
import org.example.QueueingSystem.ProbabilityDistributions.FunRand;
import org.example.QueueingSystem.Routing.PriorityRoute;

import java.util.List;
import java.util.Random;

// Мережа масового обслуговування "рух через ділянку ремонту дороги" (завдання A8)
public class TrafficModel {
    public static final double RED = 55.0;
    public static final double MEAN_ARRIVAL_1 = 12.0;
    public static final double MEAN_ARRIVAL_2 = 9.0;
    public static final double PASS_INTERVAL = 2.0;
    public static final double SECTION_TIME = 55.0;

    public final Process green1, red1, green2, red2;
    public final Create in1, in2;
    public final Process f1, f2, p1, p2, repair;
    public final Dispose exit;
    public final Model model;

    public TrafficModel(double g1, double g2, long seed) {
        this(g1, g2, RED, MEAN_ARRIVAL_1, MEAN_ARRIVAL_2, seed);
    }

    public TrafficModel(double g1, double g2, double red, double meanArrival1, double meanArrival2, long seed) {
        // Світлофор — кільце G1 -> R1 -> G2 -> R2 -> G1 з однією заявкою-фазою.
        // Фаза "зелене i" активна, поки зайнятий канал Gi.
        green1 = new Process("G1", 0, 1, () -> g1);
        red1 = new Process("R1", 0, 1, () -> red);
        green2 = new Process("G2", 0, 1, () -> g2);
        red2 = new Process("R2", 0, 1, () -> red);
        green1.setNext(red1);
        red1.setNext(green2);
        green2.setNext(red2);
        red2.setNext(green1);

        // Окремий генератор для кожного напрямку, щоб потоки машин не залежали від (G1, G2)
        Random rnd1 = new Random(seed * 2);
        Random rnd2 = new Random(seed * 2 + 1);
        in1 = new Create("Напрямок 1", () -> FunRand.Exp(rnd1, meanArrival1), new PriorityRoute());
        in2 = new Create("Напрямок 2", () -> FunRand.Exp(rnd2, meanArrival2), new PriorityRoute());

        // F1/F2 з чергами Q1/Q2 — машини перед світлофором
        f1 = new Process("F1", Integer.MAX_VALUE, 1, () -> 0.0);
        f2 = new Process("F2", Integer.MAX_VALUE, 1, () -> 0.0);
        p1 = new Process("P1", 0, 1, () -> PASS_INTERVAL);
        p2 = new Process("P2", 0, 1, () -> PASS_INTERVAL);
        repair = new Process("Ремонтна ділянка", 0, 100, () -> SECTION_TIME);
        exit = new Dispose("Вихід");

        // Розгалуження одразу після надходження, до черги (рисунок 2.1).
        // N1' (пріоритет 2) — одразу до D, без зупинки:
        //   B(N1') = (G1 = 0) ∨ (Q1 ≠ 0) ∨ (P1 = 1),
        //   де Q1 ≠ 0 — є машини в черзі Q1 або у F1.
        //   Машина N1' не займає P1, тому машини з інтервалом < 2 с
        //   проїжджають без зупинки.
        // N1 (пріоритет 1) — у чергу Q1 -> F1, без блокувань.
        // F1 -> P1: B = (G1 = 0) ∨ (P1 = 1) — в'їзд з черги з інтервалом 2 с.
        in1.addNext(f1, 1);
        in1.addNext(repair, 2).setCondition(() -> green1.isBusy() && isEmpty(f1) && p1.hasFreeChannel());
        f1.setNext(p1).setCondition(() -> green1.isBusy() && p1.hasFreeChannel());

        // N2, N2' і F2 -> P2 — аналогічно
        in2.addNext(f2, 1);
        in2.addNext(repair, 2).setCondition(() -> green2.isBusy() && isEmpty(f2) && p2.hasFreeChannel());
        f2.setNext(p2).setCondition(() -> green2.isBusy() && p2.hasFreeChannel());

        p1.setNext(repair);
        p2.setNext(repair);
        repair.setNext(exit);

        // Світлофор стоїть першим: при одночасних подіях фаза перемикається раніше, ніж машини перевіряють умови
        model = new Model(List.of(green1, red1, green2, red2, in1, in2, f1, f2, p1, p2, repair, exit));
        green1.inAct(); // у момент t = 0 починається фаза G1
    }

    // Перед світлофором немає жодної машини: черга Q порожня і фіктивний пристрій F вільний
    private static boolean isEmpty(Process f) {
        return f.getQueue() == 0 && f.hasFreeChannel();
    }

    public void run(double time) {
        model.simulate(time);
    }

    // Машина чекає, поки перебуває в черзі Q, у фіктивному пристрої F
    // або в пристрої P: у P вона ще не в'їхала на ділянку
    // (інтервал 2 с між машинами, розгін після вмикання зеленого)
    private static double waitSum(Process f, Process p) {
        return f.getMeanQueue() + f.getMeanBusy() + p.getMeanBusy();
    }

    public double meanWait() {
        return meanWaitingTime(waitSum(f1, p1) + waitSum(f2, p2),
                in1.getQuantity() + in2.getQuantity());
    }
    public double meanWait1() {
        return meanWaitingTime(waitSum(f1, p1), in1.getQuantity());
    }
    public double meanWait2() {
        return meanWaitingTime(waitSum(f2, p2), in2.getQuantity());
    }

    // Середній час очікування: (Σ Q·Δt + Σ (зайнятий F)·Δt + Σ (зайнятий P)·Δt)
    //   / кількість машин, що під'їхали.
    // Машини маршрутів N1', N2' у Q, F і P не потрапляють,
    // але входять у знаменник з нульовим очікуванням.
    private static double meanWaitingTime(double totalWait, int arrivals) {
        return arrivals > 0 ? totalWait / arrivals : 0.0;
    }
}
