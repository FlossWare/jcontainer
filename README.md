# JContainer

Universal container and orchestration abstraction library for Java. Provides a simple, unified API for reading data from Kubernetes ConfigMaps, Docker containers, and Hazelcast distributed maps.

## Features

- ✅ **Unified API** - Single interface for all container systems
- ✅ **3 Systems** - Kubernetes, Docker, Hazelcast
- ✅ **Builder Pattern** - Fluent, type-safe configuration
- ✅ **Optional Dependencies** - Include only the systems you need
- ✅ **Thread-Safe** - Concurrent operations supported
- ✅ **AutoCloseable** - Proper resource management
- ✅ **Minimal Dependencies** - Java 11+, system libraries are optional

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>org.flossware</groupId>
    <artifactId>container-java</artifactId>
    <version>1.0</version>
</dependency>

<!-- Add system SDK (only include what you need) -->
<dependency>
    <groupId>io.kubernetes</groupId>
    <artifactId>client-java</artifactId>
    <version>26.0.0</version>
</dependency>
```

### Basic Usage

```java
import org.flossware.container.ContainerClient;
import org.flossware.container.KubernetesContainerClient;

// Create Kubernetes client
ContainerClient client = KubernetesContainerClient.builder()
    .namespace("production")
    .build();

// Read from ConfigMap
byte[] data = client.read("app-config", "database.url");

// List all keys
List<String> keys = client.listKeys("app-config");

// Read all data
Map<String, byte[]> allData = client.readAll("app-config");

// Clean up
client.close();
```

## Supported Systems

### Kubernetes

```java
ContainerClient k8s = KubernetesContainerClient.builder()
    .namespace("production")
    .build();
```

**Features:**
- Reads from ConfigMaps (Secrets support similar)
- Base64-encoded data
- Namespace support
- Default client configuration

**Dependency:**
```xml
<dependency>
    <groupId>io.kubernetes</groupId>
    <artifactId>client-java</artifactId>
    <version>26.0.0</version>
</dependency>
```

### Docker

```java
ContainerClient docker = DockerContainerClient.builder()
    .dockerHost("unix:///var/run/docker.sock")
    .build();
```

**Features:**
- Read files from running containers
- Container ID + file path access
- Docker socket or TCP connection

**Note:** Listing and readAll not supported (use docker exec for directory listing)

**Dependency:**
```xml
<dependency>
    <groupId>com.github.docker-java</groupId>
    <artifactId>docker-java</artifactId>
    <version>3.7.1</version>
</dependency>
```

### Hazelcast

```java
ContainerClient hazelcast = HazelcastContainerClient.builder()
    .clusterName("dev")
    .addAddress("localhost:5701")
    .addAddress("localhost:5702")
    .build();
```

**Features:**
- Distributed in-memory data grid
- Map-based storage
- Cluster support
- Full key listing

**Dependency:**
```xml
<dependency>
    <groupId>com.hazelcast</groupId>
    <artifactId>hazelcast</artifactId>
    <version>5.7.0</version>
</dependency>
```

## API Reference

```java
public interface ContainerClient extends AutoCloseable {
    byte[] read(String resource, String key) throws IOException;
    boolean exists(String resource, String key) throws IOException;
    List<String> listKeys(String resource) throws IOException;
    Map<String, byte[]> readAll(String resource) throws IOException;
    String getDescription();
    void close() throws IOException;
}
```

## Common Use Cases

### Cloud-Native Configuration

```java
// Kubernetes ConfigMaps for 12-factor apps
ContainerClient k8s = KubernetesContainerClient.builder()
    .namespace("production")
    .build();

byte[] dbUrl = k8s.read("database-config", "url");
byte[] apiKey = k8s.read("secrets", "api.key");
```

### Container Inspection

```java
// Read configuration from running Docker container
ContainerClient docker = DockerContainerClient.builder()
    .dockerHost("unix:///var/run/docker.sock")
    .build();

byte[] config = docker.read("container-id", "/app/config.json");
```

### Distributed Caching

```java
// Hazelcast for distributed configuration
ContainerClient cache = HazelcastContainerClient.builder()
    .clusterName("prod-cluster")
    .addAddress("hazelcast-1:5701")
    .addAddress("hazelcast-2:5701")
    .build();

byte[] cached = cache.read("config-cache", "feature-flags");
```

## System Comparison

| System | Speed | Persistence | Use Case | Listing |
|--------|-------|-------------|----------|---------|
| **Kubernetes** | ⚡⚡⚡ | ⭐⭐⭐⭐⭐ | Cloud-native config | ✅ Yes |
| **Docker** | ⚡⚡⚡⚡ | ⭐⭐⭐ | Container inspection | ❌ No |
| **Hazelcast** | ⚡⚡⚡⚡⚡ | ⭐⭐ | Distributed cache | ✅ Yes |

## Best Practices

1. **Always use try-with-resources or close():**
   ```java
   try (ContainerClient client = builder.build()) {
       byte[] data = client.read("resource", "key");
   }
   ```

2. **Use appropriate system for your use case:**
   ```java
   // Cloud-native apps → Kubernetes
   // Container inspection → Docker
   // Distributed cache → Hazelcast
   ```

3. **Handle null responses:**
   ```java
   byte[] data = client.read("resource", "key");
   if (data == null) {
       // Key not found
   }
   ```

4. **Check feature support:**
   ```java
   // Docker doesn't support listKeys()
   if (client instanceof DockerContainerClient) {
       // Use individual read() calls
   } else {
       List<String> keys = client.listKeys("resource");
   }
   ```

## Versioning and Releases

This project uses **X.Y semantic versioning** (e.g., 1.0, 1.1, 2.0). Versions are automatically incremented on commits to the main branch and published to packagecloud.io.

### Maven Repository

```xml
<repositories>
    <repository>
        <id>packagecloud-flossware</id>
        <url>https://packagecloud.io/flossware/java/maven2</url>
    </repository>
</repositories>
```

## Building from Source

```bash
git clone https://github.com/FlossWare/container-java.git
cd container-java
mvn clean install
```

## License

Apache License 2.0

## Related Projects

- [jcloudstorage](https://github.com/FlossWare/jcloudstorage) - Cloud storage abstraction (S3, Azure, GCS, Google Drive, Dropbox, OneDrive)
- [jfiletransfer](https://github.com/FlossWare/jfiletransfer) - File transfer abstraction (SFTP, WebDAV, SMB/CIFS, FTP/FTPS)
- [jmessaging](https://github.com/FlossWare/jmessaging) - Messaging abstraction (Kafka, RabbitMQ, Redis)
- [jvcs](https://github.com/FlossWare/jvcs) - Version control abstraction (Git)
- [jclassloader](https://github.com/FlossWare/jclassloader) - Dynamic class loading from 34+ transport protocols
