package org.flossware.container;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.util.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ContainerClient implementation for Kubernetes.
 *
 * <p>Reads data from Kubernetes ConfigMaps and Secrets. Data is stored as Base64-encoded
 * values in ConfigMap/Secret fields.</p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ContainerClient k8s = KubernetesContainerClient.builder()
 *     .namespace("production")
 *     .build();
 *
 * // Read from ConfigMap
 * byte[] data = k8s.read("app-config", "database.url");
 *
 * // List all keys in ConfigMap
 * List<String> keys = k8s.listKeys("app-config");
 *
 * // Read all data from ConfigMap
 * Map<String, byte[]> allData = k8s.readAll("app-config");
 *
 * k8s.close();
 * }</pre>
 */
public class KubernetesContainerClient implements ContainerClient {
    private final CoreV1Api api;
    private final String namespace;

    private KubernetesContainerClient(CoreV1Api api, String namespace) {
        this.api = Objects.requireNonNull(api, "api cannot be null");
        this.namespace = Objects.requireNonNull(namespace, "namespace cannot be null");
    }

    @Override
    public byte[] read(String resource, String key) throws IOException {
        try {
            V1ConfigMap configMap = api.readNamespacedConfigMap(resource, namespace).execute();

            if (configMap.getData() == null) {
                return null;
            }

            String base64Data = configMap.getData().get(key);
            if (base64Data == null) {
                return null;
            }

            return Base64.getDecoder().decode(base64Data);

        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return null;
            }
            throw new IOException("Failed to read from Kubernetes ConfigMap: " + resource, e);
        }
    }

    @Override
    public boolean exists(String resource, String key) throws IOException {
        try {
            V1ConfigMap configMap = api.readNamespacedConfigMap(resource, namespace).execute();
            return configMap.getData() != null && configMap.getData().containsKey(key);
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return false;
            }
            throw new IOException("Failed to check existence in Kubernetes: " + resource, e);
        }
    }

    @Override
    public List<String> listKeys(String resource) throws IOException {
        try {
            V1ConfigMap configMap = api.readNamespacedConfigMap(resource, namespace).execute();

            if (configMap.getData() == null) {
                return new ArrayList<>();
            }

            return new ArrayList<>(configMap.getData().keySet());

        } catch (ApiException e) {
            throw new IOException("Failed to list keys from Kubernetes ConfigMap: " + resource, e);
        }
    }

    @Override
    public Map<String, byte[]> readAll(String resource) throws IOException {
        try {
            V1ConfigMap configMap = api.readNamespacedConfigMap(resource, namespace).execute();

            Map<String, byte[]> result = new HashMap<>();

            if (configMap.getData() != null) {
                for (Map.Entry<String, String> entry : configMap.getData().entrySet()) {
                    result.put(entry.getKey(), Base64.getDecoder().decode(entry.getValue()));
                }
            }

            return result;

        } catch (ApiException e) {
            throw new IOException("Failed to read all from Kubernetes ConfigMap: " + resource, e);
        }
    }

    @Override
    public String getDescription() {
        return "Kubernetes[namespace=" + namespace + "]";
    }

    @Override
    public void close() throws IOException {
        // Kubernetes client doesn't require explicit cleanup
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ApiClient apiClient;
        private String namespace = "default";

        public Builder apiClient(ApiClient apiClient) {
            this.apiClient = apiClient;
            return this;
        }

        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public KubernetesContainerClient build() throws IOException {
            if (apiClient == null) {
                try {
                    apiClient = Config.defaultClient();
                } catch (Exception e) {
                    throw new IOException("Failed to create Kubernetes API client", e);
                }
            }

            CoreV1Api api = new CoreV1Api(apiClient);
            return new KubernetesContainerClient(api, namespace);
        }
    }
}
