package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {
    static final double SIMULATION_TIME = 100_000.0;

    static final int GREEN_MIN = 5;
    static final int GREEN_MAX = 150;
    static final int GREEN_STEP = 5;

    static final int REPLICATIONS = 5;
    static final long BASE_SEED = 12345L;

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        int n = (GREEN_MAX - GREEN_MIN) / GREEN_STEP + 1;
        double[][] waits = gridSearch(GREEN_MIN, GREEN_STEP, n, TrafficModel.RED, REPLICATIONS);

        int bestG1 = 0;
        int bestG2 = 0;
        double bestWait = Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (waits[i][j] < bestWait) {
                    bestWait = waits[i][j];
                    bestG1 = GREEN_MIN + i * GREEN_STEP;
                    bestG2 = GREEN_MIN + j * GREEN_STEP;
                }
            }
        }

        System.out.printf("Best: G1 = %d, G2 = %d, mean waiting time = %.4f%n%n", bestG1, bestG2, bestWait);
        TrafficModel m = new TrafficModel(bestG1, bestG2, BASE_SEED);
        m.run(SIMULATION_TIME);
        m.model.printResult();
        System.out.printf("Mean waiting time: direction 1 = %.4f, direction 2 = %.4f, total = %.4f%n",
                m.meanWait1(), m.meanWait2(), m.meanWait());
    }

    // Повний перебір пар (G1, G2) на сітці min, min+step, ... (n значень); одна задача пулу — одна пара
    static double[][] gridSearch(int min, int step, int n, double red, int replications)
            throws InterruptedException, ExecutionException {
        double[][] waits = new double[n][n];
        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<?>> tasks = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                final int row = i;
                final int col = j;
                final double g1 = min + i * step;
                final double g2 = min + j * step;
                tasks.add(pool.submit(() -> {
                    waits[row][col] = meanOverReplications(g1, g2, red, replications);
                }));
            }
        }
        for (Future<?> task : tasks) {
            task.get();
        }
        pool.shutdown();
        return waits;
    }

    // Середнє за прогонами з однаковими зернами для всіх (G1, G2) — метод спільних випадкових чисел
    static double meanOverReplications(double g1, double g2, double red, int replications) {
        double sum = 0;
        for (int r = 0; r < replications; r++) {
            TrafficModel m = new TrafficModel(g1, g2, red, TrafficModel.MEAN_ARRIVAL_1, TrafficModel.MEAN_ARRIVAL_2, BASE_SEED + r);
            m.run(SIMULATION_TIME);
            sum += m.meanWait();
        }
        return sum / replications;
    }
}
