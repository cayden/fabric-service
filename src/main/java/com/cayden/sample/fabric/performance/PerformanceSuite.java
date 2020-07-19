package com.cayden.sample.fabric.performance;

public interface PerformanceSuite {
    String getName();

    void call(PerformanceSuiteCallback callback);
}
