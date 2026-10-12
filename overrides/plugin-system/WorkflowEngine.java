package ir.sinicable.rubikaqueue;

import java.util.List;

/** Deterministic, synchronous steps with bounded retries; no external worker required. */
public final class WorkflowEngine {
    public interface Step {
        String id();
        Object execute(Object input) throws Exception;
    }
    public interface Cancellation {
        boolean cancelled();
    }
    public static final class Result {
        public final boolean success;
        public final boolean cancelled;
        public final String lastStep;
        public final int totalAttempts;
        public final Object output;
        public final Exception error;

        private Result(boolean success, boolean cancelled, String step, int attempts,
                       Object output, Exception error) {
            this.success = success;
            this.cancelled = cancelled;
            this.lastStep = step;
            this.totalAttempts = attempts;
            this.output = output;
            this.error = error;
        }
    }

    public Result run(List<Step> steps, Object initial, int maxAttempts, Cancellation cancellation) {
        if (steps == null || maxAttempts < 1 || maxAttempts > 5) {
            throw new IllegalArgumentException("Steps required; maxAttempts must be 1..5");
        }
        Object current = initial;
        int attempts = 0;
        String currentStep = null;
        for (Step step : steps) {
            if (step == null || step.id() == null || step.id().trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid workflow step");
            }
            currentStep = step.id();
            Exception last = null;
            boolean completed = false;
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                if (cancellation != null && cancellation.cancelled()) {
                    return new Result(false, true, currentStep, attempts, current, null);
                }
                attempts++;
                try {
                    current = step.execute(current);
                    completed = true;
                    break;
                } catch (Exception failure) {
                    last = failure;
                }
            }
            if (!completed) return new Result(false, false, currentStep, attempts, current, last);
        }
        return new Result(true, false, currentStep, attempts, current, null);
    }
}
