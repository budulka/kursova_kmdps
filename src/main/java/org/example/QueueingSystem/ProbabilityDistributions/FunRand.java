package org.example.QueueingSystem.ProbabilityDistributions;

import java.util.Random;

public class FunRand {
    private static final Random RANDOM = new Random();

    public static double Exp(double timeMean) {
        double a = 0;
        while (a == 0) {
            a = Math.random();
        }
        a = -timeMean * Math.log(a);
        return a;
    }

    public static double Exp(Random rnd, double timeMean) {
        double a = 0;
        while (a == 0) {
            a = rnd.nextDouble();
        }
        return -timeMean * Math.log(a);
    }

    public static double Unif(double timeMin, double timeMax) {
        double a = 0;
        while (a == 0) {
            a = Math.random();
        }
        a = timeMin + a * (timeMax - timeMin);
        return a;
    }

    public static double Norm(double timeMean, double timeDeviation) {
        return timeMean + timeDeviation * RANDOM.nextGaussian();
    }

    public static double Erlang(double timeMean, int k) {
        double sum = 0;
        for (int i = 0; i < k; i++) {
            sum += Exp(timeMean / k);
        }
        return sum;
    }
}
