package ir.sinicable.rubikaqueue;

import java.util.LinkedHashMap;
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
        boolean shutdownPending;
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
        if (entry.shutdownPending) {
            throw new IllegalStateException("Plugin shutdown incomplete; retry disable first");
        }
        if (entry.enabled) return;
        Map<String, CapabilityRegistry.Provider> providers = null;
        boolean published = false;
        try {
            entry.plugin.start();
            Map<String, CapabilityRegistry.Provider> offered = entry.plugin.capabilities();
            if (offered == null) throw new IllegalArgumentException("Plugin capabilities missing");
            providers = new LinkedHashMap<>(offered);
            registry.registerAll(providers);
            published = true;
            entry.registered.putAll(providers);
            entry.enabled = true;
        } catch (Exception failure) {
            if (published) registry.unregisterAll(providers);
            entry.registered.clear();
            try {
                entry.plugin.stop();
            } catch (Exception stopFailure) {
                entry.shutdownPending = true;
                failure.addSuppressed(stopFailure);
            }
            throw failure;
        }
    }

    public synchronized void disable(String id) throws Exception {
        Entry entry = required(id);
        if (!entry.enabled && !entry.shutdownPending) return;
        if (!entry.shutdownPending) {
            // Hide all providers before stopping; retry stop if cleanup fails.
            registry.unregisterAll(entry.registered);
            entry.registered.clear();
            entry.shutdownPending = true;
        }
        entry.plugin.stop();
        entry.shutdownPending = false;
        entry.enabled = false;
    }

    private Entry required(String id) {
        Entry entry = plugins.get(id);
        if (entry == null) throw new IllegalArgumentException("Unknown plugin ID");
        return entry;
    }
}
