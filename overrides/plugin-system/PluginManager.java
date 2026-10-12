package ir.sinicable.rubikaqueue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Manages opt-in plugins; stopping one never disables the core application. */
public final class PluginManager {
    public interface Plugin {
        String id();
        Map<String, CapabilityRegistry.Provider> capabilities();
        void start() throws Exception;
        void stop() throws Exception;
    }

    private static final class Entry {
        final Plugin plugin;
        boolean enabled;
        final Map<String, CapabilityRegistry.Provider> registered = new LinkedHashMap<>();
        Entry(Plugin plugin) { this.plugin = plugin; }
    }

    private final CapabilityRegistry registry;
    private final Map<String, Entry> plugins = new LinkedHashMap<>();

    public PluginManager(CapabilityRegistry registry) {
        if (registry == null) throw new IllegalArgumentException("Registry is required");
        this.registry = registry;
    }

    public synchronized void register(Plugin plugin) {
        if (plugin == null || plugin.id() == null || plugin.id().trim().isEmpty()) {
            throw new IllegalArgumentException("Plugin ID is required");
        }
        if (plugins.containsKey(plugin.id())) throw new IllegalArgumentException("Duplicate plugin ID");
        plugins.put(plugin.id(), new Entry(plugin));
    }

    public synchronized boolean isEnabled(String id) {
        Entry entry = plugins.get(id);
        return entry != null && entry.enabled;
    }

    public synchronized void enable(String id) throws Exception {
        Entry entry = required(id);
        if (entry.enabled) return;
        List<String> registered = new ArrayList<>();
        Map<String, CapabilityRegistry.Provider> providers = null;
        try {
            entry.plugin.start();
            providers = entry.plugin.capabilities();
            if (providers == null) throw new IllegalArgumentException("Plugin capabilities missing");
            for (Map.Entry<String, CapabilityRegistry.Provider> item : providers.entrySet()) {
                registry.register(item.getKey(), item.getValue());
                registered.add(item.getKey());
                entry.registered.put(item.getKey(), item.getValue());
            }
            entry.enabled = true;
        } catch (Exception failure) {
            for (String capability : registered) registry.unregister(capability, providers.get(capability).id());
            entry.registered.clear();
            try { entry.plugin.stop(); } catch (Exception stopFailure) { failure.addSuppressed(stopFailure); }
            throw failure;
        }
    }

    public synchronized void disable(String id) throws Exception {
        Entry entry = required(id);
        if (!entry.enabled) return;
        entry.enabled = false;
        for (Map.Entry<String, CapabilityRegistry.Provider> item : entry.registered.entrySet()) {
            registry.unregister(item.getKey(), item.getValue().id());
        }
        entry.registered.clear();
        entry.plugin.stop();
    }

    private Entry required(String id) {
        Entry entry = plugins.get(id);
        if (entry == null) throw new IllegalArgumentException("Unknown plugin ID");
        return entry;
    }
}
