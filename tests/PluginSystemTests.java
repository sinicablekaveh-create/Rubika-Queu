package ir.sinicable.rubikaqueue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PluginSystemTests {
    private static int count;
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("PluginSystemTests failure at check " + count);
        count++;
    }
    private static CapabilityRegistry.Provider provider(final String id, final int priority,
                                                        final boolean healthy, final boolean fails) {
        return new CapabilityRegistry.Provider() {
            public String id() { return id; }
            public int priority() { return priority; }
            public boolean isAvailable() { return healthy; }
            public Object execute(Object request) throws Exception {
                if (fails) throw new Exception("unavailable");
                return id + ":" + request;
            }
        };
    }
    public static void main(String[] args) throws Exception {
        CapabilityRegistry registry = new CapabilityRegistry();
        registry.register("ai", provider("backup", 1, true, false));
        registry.register("ai", provider("primary", 10, true, true));
        check("backup:hello".equals(registry.execute("ai", "hello")));
        registry.register("research", provider("offline", 1, false, false));
        try { registry.execute("research", "q"); throw new AssertionError("missing failure"); }
        catch (IllegalStateException expected) { check(true); }
        try { registry.register("ai", provider("backup", 0, true, false)); throw new AssertionError("duplicate"); }
        catch (IllegalArgumentException expected) { check(true); }
        final boolean[] started = {false};
        PluginManager.Plugin plugin = new PluginManager.Plugin() {
            public String id() { return "plugin-1"; }
            public Map<String, CapabilityRegistry.Provider> capabilities() {
                Map<String, CapabilityRegistry.Provider> providers = new HashMap<>();
                providers.put("search", provider("plugin-search", 5, true, false));
                return providers;
            }
            public void start() { started[0] = true; }
            public void stop() { started[0] = false; }
        };
        PluginManager manager = new PluginManager(registry);
        manager.register(plugin);
        manager.enable("plugin-1");
        check(started[0] && manager.isEnabled("plugin-1"));
        check("plugin-search:x".equals(registry.execute("search", "x")));
        manager.disable("plugin-1");
        check(!started[0] && !manager.isEnabled("plugin-1"));
        check(registry.providerCount("search") == 0);
        final int[] tries = {0};
        WorkflowEngine.Step retried = new WorkflowEngine.Step() {
            public String id() { return "retry"; }
            public Object execute(Object input) throws Exception {
                if (++tries[0] < 2) throw new Exception("temporary");
                return ((Integer) input) + 1;
            }
        };
        WorkflowEngine engine = new WorkflowEngine();
        WorkflowEngine.Result done = engine.run(Arrays.asList(retried), 1, 3, null);
        check(done.success && (Integer) done.output == 2 && done.totalAttempts == 2);
        WorkflowEngine.Result cancelled = engine.run(Arrays.asList(retried), 1, 3,
                new WorkflowEngine.Cancellation() { public boolean cancelled() { return true; } });
        check(cancelled.cancelled && !cancelled.success && cancelled.totalAttempts == 0);
        WorkflowEngine.Result empty = engine.run(Collections.<WorkflowEngine.Step>emptyList(), 42, 1, null);
        check(empty.success && (Integer) empty.output == 42);
        // A malformed later step must not run any earlier side-effecting step.
        final int[] sideEffects = {0};
        WorkflowEngine.Step effect = new WorkflowEngine.Step() {
            public String id() { return "effect"; }
            public Object execute(Object value) { sideEffects[0]++; return value; }
        };
        try {
            engine.run(Arrays.asList(effect, null), 0, 1, null);
            throw new AssertionError("Malformed step accepted");
        } catch (IllegalArgumentException expected) { check(sideEffects[0] == 0); }

        // Cancellation during the last failed attempt must win over ordinary failure.
        final boolean[] cancelledDuringStep = {false};
        WorkflowEngine.Step cancelledFailure = new WorkflowEngine.Step() {
            public String id() { return "cancel-failure"; }
            public Object execute(Object value) throws Exception {
                cancelledDuringStep[0] = true;
                throw new Exception("cancelled while running");
            }
        };
        WorkflowEngine.Result cancelledLast = engine.run(
                Arrays.asList(cancelledFailure), 0, 1,
                new WorkflowEngine.Cancellation() {
                    public boolean cancelled() { return cancelledDuringStep[0]; }
                });
        check(cancelledLast.cancelled && !cancelledLast.success && cancelledLast.totalAttempts == 1);

        // A duplicate in a batch must not expose earlier capabilities.
        Map<String, CapabilityRegistry.Provider> badBatch = new LinkedHashMap<>();
        badBatch.put("transient", provider("new", 1, true, false));
        badBatch.put("ai", provider("backup", 1, true, false));
        try {
            registry.registerAll(badBatch);
            throw new AssertionError("Invalid registration batch accepted");
        } catch (IllegalArgumentException expected) {
            check(registry.providerCount("transient") == 0);
        }

        // A failed stop keeps shutdown retryable and forbids restart.
        final int[] stops = {0};
        PluginManager.Plugin flaky = new PluginManager.Plugin() {
            public String id() { return "flaky"; }
            public Map<String, CapabilityRegistry.Provider> capabilities() {
                Map<String, CapabilityRegistry.Provider> offered = new HashMap<>();
                offered.put("flaky-capability", provider("flaky-provider", 1, true, false));
                return offered;
            }
            public void start() { }
            public void stop() throws Exception {
                if (++stops[0] == 1) throw new Exception("temporary stop error");
            }
        };
        manager.register(flaky);
        manager.enable("flaky");
        try {
            manager.disable("flaky");
            throw new AssertionError("Failed stop ignored");
        } catch (Exception expected) {
            check(stops[0] == 1 && registry.providerCount("flaky-capability") == 0);
        }
        try {
            manager.enable("flaky");
            throw new AssertionError("Restarted before shutdown completed");
        } catch (IllegalStateException expected) { check(true); }
        manager.disable("flaky");
        check(stops[0] == 2 && !manager.isEnabled("flaky"));
        System.out.println("PluginSystemTests: " + count + " checks passed");
    }
}
