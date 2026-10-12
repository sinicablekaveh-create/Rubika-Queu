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

    public synchronized void register(String capability, Provider provider) {
        if (capability == null || capability.trim().isEmpty() || provider == null ||
                provider.id() == null || provider.id().trim().isEmpty()) {
            throw new IllegalArgumentException("Capability and provider ID are required");
        }
        List<Provider> entries = capabilities.get(capability);
        if (entries == null) {
            entries = new ArrayList<>();
            capabilities.put(capability, entries);
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
