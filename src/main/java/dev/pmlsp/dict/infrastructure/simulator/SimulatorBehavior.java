package dev.pmlsp.dict.infrastructure.simulator;

import dev.pmlsp.dict.infrastructure.config.DictClientProperties;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Encapsulates the misbehaviors a developer can dial into the simulator to exercise
 * client-side resilience: error injection and latency jitter.
 */
@Slf4j
public class SimulatorBehavior {

    private final DictClientProperties.Simulator props;

    public SimulatorBehavior(DictClientProperties.Simulator props) {
        this.props = props;
    }

    /**
     * @return {@code true} when the simulator should pretend to fail the next request.
     */
    public boolean shouldFail() {
        if (props == null || props.failureRate() <= 0) return false;
        return ThreadLocalRandom.current().nextDouble() < props.failureRate();
    }

    /**
     * Sleep a random duration up to {@code latencyJitter} to mimic real network jitter.
     */
    public void applyLatencyJitter() {
        if (props == null || props.latencyJitter() == null || props.latencyJitter().isZero()) return;
        long maxMs = props.latencyJitter().toMillis();
        if (maxMs <= 0) return;
        long sleep = ThreadLocalRandom.current().nextLong(maxMs);
        try {
            Thread.sleep(Duration.ofMillis(sleep));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
