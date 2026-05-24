package org.flossware.container;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Universal interface for container and orchestration operations.
 *
 * <p>Provides a unified API for reading data from Kubernetes ConfigMaps/Secrets,
 * Docker container volumes, and Hazelcast distributed maps.</p>
 *
 * <h2>Supported Systems</h2>
 * <ul>
 *   <li>Kubernetes - ConfigMaps and Secrets</li>
 *   <li>Docker - Container file access</li>
 *   <li>Hazelcast - Distributed in-memory data grid</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Kubernetes
 * ContainerClient k8s = KubernetesContainerClient.builder()
 *     .namespace("production")
 *     .build();
 *
 * byte[] data = k8s.read("config-map-name", "key");
 * k8s.close();
 * }</pre>
 *
 * @see KubernetesContainerClient
 * @see DockerContainerClient
 * @see HazelcastContainerClient
 */
public interface ContainerClient extends Closeable {

    /**
     * Reads data from a container resource.
     *
     * <p>Behavior varies by implementation:
     * <ul>
     *   <li>Kubernetes: Reads from ConfigMap/Secret by name and key</li>
     *   <li>Docker: Reads file from container by container ID and file path</li>
     *   <li>Hazelcast: Reads from distributed map by map name and key</li>
     * </ul>
     *
     * @param resource The resource name (ConfigMap name, container ID, map name)
     * @param key The key/path within the resource
     * @return The data as bytes, or null if not found
     * @throws IOException If the read operation fails
     */
    byte[] read(String resource, String key) throws IOException;

    /**
     * Checks if a resource/key combination exists.
     *
     * @param resource The resource name
     * @param key The key/path within the resource
     * @return true if exists, false otherwise
     * @throws IOException If the check fails
     */
    boolean exists(String resource, String key) throws IOException;

    /**
     * Lists all keys in a resource.
     *
     * @param resource The resource name
     * @return A list of keys
     * @throws IOException If the list operation fails
     */
    List<String> listKeys(String resource) throws IOException;

    /**
     * Reads all data from a resource as a map.
     *
     * @param resource The resource name
     * @return A map of key-value pairs (values as bytes)
     * @throws IOException If the read operation fails
     */
    Map<String, byte[]> readAll(String resource) throws IOException;

    /**
     * Gets a human-readable description of this container client.
     *
     * @return A description string (e.g., "Kubernetes[namespace=production]")
     */
    String getDescription();

    /**
     * Closes the container client and releases all resources.
     *
     * @throws IOException If an error occurs during cleanup
     */
    @Override
    void close() throws IOException;
}
