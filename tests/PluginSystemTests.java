package ir.sinicable.rubikaqueue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
        System.out.println("PluginSystemTests: " + count + " checks passed");
    }
}
