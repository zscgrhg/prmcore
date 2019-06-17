package com.zte;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
@ExtendWith(TestSuiteProfilerExtension.class)
@Execution(ExecutionMode.CONCURRENT)
class FirstTest {

    @Test
    void sabra() throws InterruptedException {
        Thread.sleep(10_000);
        System.out.println("SABRA! " + Thread.currentThread().getName());
    }

    @Test
    void cadabra() throws InterruptedException {
        Thread.sleep(10_000);
        System.out.println("CADABRA! " + Thread.currentThread().getName());
    }
}


