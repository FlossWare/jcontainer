package org.flossware.container;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ContainerClient implementation for Hazelcast.
 *
 * <p>Reads data from Hazelcast distributed maps. Each "resource" is a map name,
 * and each "key" is a key within that map.</p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ContainerClient hazelcast = HazelcastContainerClient.builder()
 *     .clusterName("dev")
 *     .addAddress("localhost:5701")
 *     .build();
 *
 * // Read from distributed map
 * byte[] data = hazelcast.read("config-map", "app.setting");
 *
 * // List all keys in map
 * List<String> keys = hazelcast.listKeys("config-map");
 *
 * hazelcast.close();
 * }</pre>
 */
public class HazelcastContainerClient implements ContainerClient {
    private final HazelcastInstance hazelcastInstance;

    private HazelcastContainerClient(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = Objects.requireNonNull(hazelcastInstance, "hazelcastInstance cannot be null");
    }

    @Override
    public byte[] read(String resource, String key) throws IOException {
        try {
            IMap<String, byte[]> map = hazelcastInstance.getMap(resource);
            return map.get(key);
        } catch (Exception e) {
            throw new IOException("Failed to read from Hazelcast map: " + resource, e);
        }
    }

    @Override
    public boolean exists(String resource, String key) throws IOException {
        try {
            IMap<String, byte[]> map = hazelcastInstance.getMap(resource);
            return map.containsKey(key);
        } catch (Exception e) {
            throw new IOException("Failed to check existence in Hazelcast: " + resource, e);
        }
    }

    @Override
    public List<String> listKeys(String resource) throws IOException {
        try {
            IMap<String, byte[]> map = hazelcastInstance.getMap(resource);
            return new ArrayList<>(map.keySet());
        } catch (Exception e) {
            throw new IOException("Failed to list keys from Hazelcast map: " + resource, e);
        }
    }

    @Override
    public Map<String, byte[]> readAll(String resource) throws IOException {
        try {
            IMap<String, byte[]> map = hazelcastInstance.getMap(resource);
            return new HashMap<>(map);
        } catch (Exception e) {
            throw new IOException("Failed to read all from Hazelcast map: " + resource, e);
        }
    }

    @Override
    public String getDescription() {
        return "Hazelcast[cluster=" + hazelcastInstance.getConfig().getClusterName() + "]";
    }

    @Override
    public void close() throws IOException {
        if (hazelcastInstance != null) {
            hazelcastInstance.shutdown();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String clusterName = "dev";
        private List<String> addresses = new ArrayList<>();

        public Builder clusterName(String clusterName) {
            this.clusterName = clusterName;
            return this;
        }

        public Builder addAddress(String address) {
            this.addresses.add(address);
            return this;
        }

        public Builder addresses(List<String> addresses) {
            this.addresses = new ArrayList<>(addresses);
            return this;
        }

        public HazelcastContainerClient build() {
            ClientConfig config = new ClientConfig();
            config.setClusterName(clusterName);

            if (!addresses.isEmpty()) {
                config.getNetworkConfig().addAddress(addresses.toArray(new String[0]));
            }

            HazelcastInstance instance = HazelcastClient.newHazelcastClient(config);
            return new HazelcastContainerClient(instance);
        }
    }
}
