/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.bench;

import com.carrotsearch.randomizedtesting.generators.RandomStrings;
import org.elasticsearch.common.metrics.MeanMetric;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.shield.authc.support.Hasher;
import org.elasticsearch.shield.authc.support.SecuredString;
import org.junit.Ignore;

import java.util.concurrent.ThreadLocalRandom;

/**
 *
 */
@Ignore
public class HasherBenchmark {

    private static final int WARMING_ITERS = 1000;
    private static final int BENCH_ITERS = 10000;


    public static void main(String[] args) throws Exception {
        test(Hasher.SSHA256).print();
        test(Hasher.MD5).print();
        test(Hasher.SHA1).print();
        test(Hasher.BCRYPT4).print();
    }

    protected static Metrics test(Hasher hasher) {

        Metrics metrics = new Metrics(hasher);

        System.out.print("warming up [" + hasher.name() + "]...");

        for (int i = 0; i < WARMING_ITERS; i++) {
            SecuredString str = new SecuredString(RandomStrings.randomAsciiOfLength(ThreadLocalRandom.current(), 8).toCharArray());
            char[] hash = hasher.hash(str);
            hasher.verify(str, hash);
        }

        System.out.println("done!");
        System.out.print("starting benchmark for [" + hasher.name() + "]...");

        long start;

        for (int i = 0; i < BENCH_ITERS; i++) {
            SecuredString str = new SecuredString(RandomStrings.randomAsciiOfLength(ThreadLocalRandom.current(), 8).toCharArray());

            start = System.nanoTime();
            char[] hash = hasher.hash(str);
            metrics.hash.inc(System.nanoTime() - start);

            start = System.nanoTime();
            hasher.verify(str, hash);
            metrics.verify.inc(System.nanoTime() - start);
            if (i % 1000000 == 0) {
                System.out.println("finished " + i + " iterations");
            }
        }

        System.out.println("done!");

        return metrics;
    }

    private static class Metrics {

        final String name;
        final MeanMetric hash = new MeanMetric();
        final MeanMetric verify = new MeanMetric();

        public Metrics(Hasher hasher) {
            this.name = hasher.name();
        }

        void print() {
            System.out.println(name);
            System.out.println("\tHash (total): " + TimeValue.timeValueNanos(hash.sum()).format());
            System.out.println("\tHash (avg): " + hash.mean() + " nanos");
            System.out.println("\tVerify (total): " + TimeValue.timeValueNanos(verify.sum()).format());
            System.out.println("\tVerify (avg): " + verify.mean() + " nanos");
        }
    }
}
