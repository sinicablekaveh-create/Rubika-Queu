package ir.sinicable.rubikaqueue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Optional providers are selected by priority, with failure isolation and fallback. */
public final class CapabilityRegistry {
    public interface Provider {
        String id();
        int priority();
        boolean isAvailable();
        Object execute(Object request) throws Exception;
    }

    private final Map<String, List<Provider>> capabilities = new HashMap<>();

    /** Publish all plugin capabilities together, or none if validation fails. */
    public synchronized void registerAll(Map<String, Provider> providers) {
        if (providers == null) throw new IllegalArgumentException("Providers are required");
        Map<String, List<Provider>> staged = new HashMap<>();
        for (Map.Entry<String, List<Provider>> item : capabilities.entrySet()) {
            staged.put(item.getKey(), new ArrayList<>(item.getValue()));
        }
        for (Map.Entry<String, Provider> item : providers.entrySet()) {
            String capability = item.getKey();
            Provider provider = item.getValue();
            if (capability == null || capability.trim().isEmpty() || provider == null ||
                    provider.id() == null || provider.id().trim().isEmpty()) {
                throw new IllegalArgumentException("Capability and provider ID are required");
            }
            List<Provider> entries = staged.get(capability);
            if (entries == null) {
                entries = new ArrayList<>();
                staged.put(capability, entries);
            }
            for (Provider existing : entries) {
                if (existing.id().equals(provider.id())) {
                    throw new IllegalArgumentException("Duplicate provider ID for capability");
                }
            }
            entries.add(provider);
            Collections.sort(entries, new Comparator<Provider>() {
                @Override public int compare(Provider left, Provider right) {
                    return Integer.compare(right.priority(), left.priority());
                }
            });
        }
        capabilities.clear();
        capabilities.putAll(staged);
    }

    public synchronized void register(String capability, Provider provider) {
        registerAll(Collections.singletonMap(capability, provider));
    }

    /** Remove a plugin's providers as a single visible change. */
    public synchronized void unregisterAll(Map<String, Provider> providers) {
        if (providers == null) return;
        for (Map.Entry<String, Provider> item : providers.entrySet()) {
            if (item.getValue() != null) unregister(item.getKey(), item.getValue().id());
        }
    }

    public synchronized void unregister(String capability, String providerId) {
        List<Provider> entries = capabilities.get(capability);
        if (entries == null) return;
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i).id().equals(providerId)) entries.remove(i);
        }
        if (entries.isEmpty()) capabilities.remove(capability);
    }

    public synchronized int providerCount(String capability) {
        List<Provider> entries = capabilities.get(capability);
        return entries == null ? 0 : entries.size();
    }

    public Object execute(String capability, Object request) throws Exception {
        List<Provider> snapshot;
        synchronized (this) {
            List<Provider> entries = capabilities.get(capability);
            snapshot = entries == null ? Collections.<Provider>emptyList() : new ArrayList<>(entries);
        }
        Exception lastFailure = null;
        for (Provider provider : snapshot) {
            try {
                if (provider.isAvailable()) return provider.execute(request);
            } catch (Exception failure) {
                lastFailure = failure;
            }
        }
        throw new IllegalStateException("No available provider for capability: " + capability, lastFailure);
    }
}
