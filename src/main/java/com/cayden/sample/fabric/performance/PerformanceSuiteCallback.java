package com.cayden.sample.fabric.performance;

public interface PerformanceSuiteCallback {
    void onSuccess(String message);

    void onFailed(String message);
}
