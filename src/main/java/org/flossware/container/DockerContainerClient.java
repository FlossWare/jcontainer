package org.flossware.container;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CopyArchiveFromContainerCmd;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ContainerClient implementation for Docker.
 *
 * <p>Reads files from Docker containers. Each "resource" is a container ID,
 * and each "key" is a file path within the container.</p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ContainerClient docker = DockerContainerClient.builder()
 *     .dockerHost("unix:///var/run/docker.sock")
 *     .build();
 *
 * // Read file from container
 * byte[] data = docker.read("container-id", "/app/config.json");
 *
 * docker.close();
 * }</pre>
 */
public class DockerContainerClient implements ContainerClient {
    private final DockerClient dockerClient;

    private DockerContainerClient(DockerClient dockerClient) {
        this.dockerClient = Objects.requireNonNull(dockerClient, "dockerClient cannot be null");
    }

    @Override
    public byte[] read(String resource, String key) throws IOException {
        try {
            CopyArchiveFromContainerCmd cmd = dockerClient.copyArchiveFromContainerCmd(resource, key);

            try (InputStream inputStream = cmd.exec();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                return outputStream.toByteArray();
            }

        } catch (Exception e) {
            throw new IOException("Failed to read from Docker container: " + resource + " path: " + key, e);
        }
    }

    @Override
    public boolean exists(String resource, String key) throws IOException {
        try {
            read(resource, key);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public List<String> listKeys(String resource) throws IOException {
        throw new UnsupportedOperationException(
            "Docker does not support directory listing via copyArchiveFromContainer. " +
            "Use docker exec with ls command for directory enumeration."
        );
    }

    @Override
    public Map<String, byte[]> readAll(String resource) throws IOException {
        throw new UnsupportedOperationException(
            "Docker does not support reading all files. " +
            "Use read(containerId, filePath) for individual files."
        );
    }

    @Override
    public String getDescription() {
        return "Docker[]";
    }

    @Override
    public void close() throws IOException {
        if (dockerClient != null) {
            try {
                dockerClient.close();
            } catch (Exception e) {
                throw new IOException("Failed to close Docker client", e);
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String dockerHost;

        public Builder dockerHost(String dockerHost) {
            this.dockerHost = dockerHost;
            return this;
        }

        public DockerContainerClient build() {
            DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();

            if (dockerHost != null) {
                configBuilder.withDockerHost(dockerHost);
            }

            DockerClient client = DockerClientBuilder.getInstance(configBuilder.build()).build();
            return new DockerContainerClient(client);
        }
    }
}
