package org.example;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// Експерименти для курсової роботи; результати записуються у CSV-файли в каталог з аргументу
public class Experiments {
    static final double T = Main.SIMULATION_TIME;
    static final long SEED = Main.BASE_SEED;

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);
        Path dir = Path.of(args.length > 0 ? args[0] : "results");
        Files.createDirectories(dir);
        verification(dir);
        transient_(dir);
        transientWindows(dir);
        replications(dir);
        int[] coarse = grid(dir, "grid_red55.csv", 55.0);
        int[] optimum = refine(dir, coarse);
        int[] coarse45 = grid(dir, "grid_red45.csv", 45.0);
        proposals(dir, optimum, coarse45);
        factorial(dir);
        System.out.printf("coarse = %s, refined = %s, red45 = %s%n", Arrays.toString(coarse),
                Arrays.toString(optimum), Arrays.toString(coarse45));
    }

    // 1. Верифікація: прогони з різними значеннями вхідних змінних
    static void verification(Path dir) throws IOException {
        double[][] cfg = {
                // g1, g2, red, a1, a2, час моделювання
                {45, 65, 55, 12, 9, T},
                {60, 60, 55, 12, 9, T},
                {60, 60, 55, 12, 12, T},
                {100, 100, 55, 12, 9, T},
                // рідкий потік: черги майже не утворюються, Wi ≈ (T - Gi)^2 / (2T);
                // час моделювання більший, щоб набрати достатню кількість машин
                {45, 65, 55, 600, 600, 1e7},
                {45, 65, 55, 1e9, 9, T},
                {10, 10, 55, 12, 9, T},
                // дзеркальний до прогону 1: напрямки поміняно місцями
                {65, 45, 55, 9, 12, T},
                // збільшений червоний інтервал
                {45, 65, 70, 12, 9, T},
                // рідкий потік при однакових зелених інтервалах
                {100, 100, 55, 600, 600, 1e7},
                // дуже довгі зелені інтервали
                {150, 150, 55, 12, 9, T},
        };
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(dir.resolve("verification.csv")))) {
            out.println("g1,g2,red,a1,a2,time,cycles,green1Share,in1,in2,p1,p2,n1prime,n2prime,repairBusy,exit,exitInterval,q1,q2,w1,w2,w");
            for (double[] c : cfg) {
                double time = c[5];
                TrafficModel m = new TrafficModel(c[0], c[1], c[2], c[3], c[4], SEED);
                m.run(time);
                int p1 = m.p1.getArrivals(), p2 = m.p2.getArrivals();
                out.printf("%.0f,%.0f,%.0f,%.0f,%.0f,%.0f,%d,%.4f,%d,%d,%d,%d,%d,%d,%.4f,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f%n",
                        c[0], c[1], c[2], c[3], c[4], time,
                        m.green1.getArrivals(), m.green1.getMeanBusy() / time,
                        m.in1.getQuantity(), m.in2.getQuantity(),
                        p1, p2, m.in1.getQuantity() - m.f1.getArrivals(), m.in2.getQuantity() - m.f2.getArrivals(),
                        m.repair.getMeanBusy() / time, m.exit.getQuantity(), m.exit.getMeanArrivalInterval(),
                        m.f1.getMeanQueue() / time, m.f2.getMeanQueue() / time,
                        m.meanWait1(), m.meanWait2(), m.meanWait());
            }
        }
    }

    // 2. Перехідний період: накопичене середнє очікування як функція часу моделювання для кількох зерен
    static void transient_(Path dir) throws IOException {
        double[] times = {250, 500, 1000, 2000, 3000, 5000, 7500, 10000, 15000, 20000, 30000, 40000,
                50000, 60000, 70000, 80000, 90000, 100000};
        int seeds = 5;
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(dir.resolve("transient.csv")))) {
            out.print("time");
            for (int s = 0; s < seeds; s++) out.print(",seed" + s);
            out.println();
            for (double t : times) {
                out.printf("%.0f", t);
                for (int s = 0; s < seeds; s++) {
                    TrafficModel m = new TrafficModel(45, 65, SEED + s);
                    m.run(t);
                    out.printf(",%.4f", m.meanWait());
                }
                out.println();
            }
        }
    }

    // 2а. Перехідний період: сумарний час очікування і кількість машин з кроком 250 с для тих самих зерен.
    // Модель просувається послідовно, тому різниці сусідніх записів дають середнє очікування за інтервалом.
    static void transientWindows(Path dir) throws IOException {
        double step = 250;
        int seeds = 5;
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(dir.resolve("transient_windows.csv")))) {
            out.println("seed,time,arrivals,totalWait,cumulativeW");
            for (int s = 0; s < seeds; s++) {
                TrafficModel m = new TrafficModel(45, 65, SEED + s);
                for (double t = step; t <= T; t += step) {
                    m.run(t);
                    int n = m.in1.getQuantity() + m.in2.getQuantity();
                    out.printf("%d,%.0f,%d,%.4f,%.4f%n", s, t, n, m.meanWait() * n, m.meanWait());
                }
            }
        }
    }

    // 3. Кількість прогонів: розкид результату між незалежними прогонами при різному часі моделювання
    static void replications(Path dir) throws IOException {
        double[] times = {10_000, 50_000, 100_000};
        int n = 30;
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(dir.resolve("replications.csv")))) {
            out.println("time,replication,w");
            for (double t : times) {
                for (int r = 0; r < n; r++) {
                    TrafficModel m = new TrafficModel(45, 65, SEED + 1000 + r);
                    m.run(t);
                    out.printf("%.0f,%d,%.4f%n", t, r, m.meanWait());
                }
            }
        }
    }

    // 4. Повний перебір на сітці з кроком 5 с; повертає найкращу пару {G1, G2}
    static int[] grid(Path dir, String file, double red) throws Exception {
        int min = Main.GREEN_MIN, step = Main.GREEN_STEP;
        int n = (Main.GREEN_MAX - min) / step + 1;
        double[][] w = Main.gridSearch(min, step, n, red, Main.REPLICATIONS);
        writeGrid(dir.resolve(file), min, step, n, w);
        return argMin(w, min, step);
    }

    // 5. Уточнення оптимуму з кроком 1 с і 20 прогонами в околі найкращої точки грубої сітки
    static int[] refine(Path dir, int[] coarse) throws Exception {
        int half = 10, step = 1, n = 2 * half + 1;
        int min1 = coarse[0] - half, min2 = coarse[1] - half;
        double[][] w = new double[n][n];
        ExecutorService pool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors());
        List<Future<?>> tasks = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                final int row = i, col = j;
                tasks.add(pool.submit(() -> {
                    w[row][col] = Main.meanOverReplications(min1 + row, min2 + col, TrafficModel.RED, 20);
                }));
            }
        }
        for (var t : tasks) t.get();
        pool.shutdown();
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(dir.resolve("grid_refine.csv")))) {
            out.println("g1,g2,w");
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++)
                    out.printf("%d,%d,%.4f%n", min1 + i, min2 + j, w[i][j]);
        }
        double best = Double.MAX_VALUE;
        int[] arg = null;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                if (w[i][j] < best) { best = w[i][j]; arg = new int[]{min1 + i, min2 + j}; }
        return arg;
    }

    // 6. Пропозиції: порівняння варіантів керування на 30 нових прогонах (зерна не використовувались при пошуку)
    static void proposals(Path dir, int[] optimum, int[] optimumRed45) throws IOException {
        Object[][] variants = {
                {"Рівні інтервали 60/60", 60, 60, 55.0},
                {"Оптимум (червоне 55 с)", optimum[0], optimum[1], 55.0},
                {"Оптимум (червоне 45 с)", optimumRed45[0], optimumRed45[1], 45.0},
        };
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(dir.resolve("proposals.csv")))) {
            out.println("name,g1,g2,red,replication,w1,w2,w");
            for (Object[] v : variants) {
                int g1 = (int) v[1], g2 = (int) v[2];
                double red = (double) v[3];
                for (int r = 0; r < 30; r++) {
                    TrafficModel m = new TrafficModel(g1, g2, red, TrafficModel.MEAN_ARRIVAL_1,
                            TrafficModel.MEAN_ARRIVAL_2, SEED + 5000 + r);
                    m.run(T);
                    out.printf("%s,%d,%d,%.0f,%d,%.4f,%.4f,%.4f%n", v[0], g1, g2, red, r,
                            m.meanWait1(), m.meanWait2(), m.meanWait());
                }
            }
        }
    }

    // 7. Повний факторний експеримент 2^3: фактори G1, G2, R на двох рівнях, p прогонів у кожній точці плану.
    // Рядки плану в стандартному порядку: x1 змінюється найчастіше, x3 — найрідше.
    // Зерна різні для всіх точок плану, тому прогони незалежні.
    static void factorial(Path dir) throws IOException {
        double[][] levels = {{45, 55}, {60, 70}, {45, 55}}; // G1, G2, R: нижній і верхній рівні
        int p = 5;
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(dir.resolve("factorial.csv")))) {
            out.println("experiment,x1,x2,x3,g1,g2,red,replication,w");
            for (int j = 0; j < 8; j++) {
                int[] x = {(j & 1) == 0 ? -1 : 1, (j & 2) == 0 ? -1 : 1, (j & 4) == 0 ? -1 : 1};
                double g1 = levels[0][(x[0] + 1) / 2], g2 = levels[1][(x[1] + 1) / 2], red = levels[2][(x[2] + 1) / 2];
                for (int r = 0; r < p; r++) {
                    TrafficModel m = new TrafficModel(g1, g2, red, TrafficModel.MEAN_ARRIVAL_1,
                            TrafficModel.MEAN_ARRIVAL_2, SEED + 3000 + 100L * j + r);
                    m.run(T);
                    out.printf("%d,%d,%d,%d,%.0f,%.0f,%.0f,%d,%.4f%n", j + 1, x[0], x[1], x[2], g1, g2, red, r, m.meanWait());
                }
            }
        }
    }

    static int[] argMin(double[][] w, int min, int step) {
        double best = Double.MAX_VALUE;
        int[] arg = null;
        for (int i = 0; i < w.length; i++)
            for (int j = 0; j < w[i].length; j++)
                if (w[i][j] < best) { best = w[i][j]; arg = new int[]{min + i * step, min + j * step}; }
        return arg;
    }

    static void writeGrid(Path file, int min, int step, int n, double[][] w) throws IOException {
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(file))) {
            out.println("g1,g2,w");
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++)
                    out.printf("%d,%d,%.4f%n", min + i * step, min + j * step, w[i][j]);
        }
    }
}
