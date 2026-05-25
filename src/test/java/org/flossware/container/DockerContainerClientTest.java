package org.flossware.container;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CopyArchiveFromContainerCmd;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for DockerContainerClient to achieve 100% coverage.
 */
class DockerContainerClientTest {

    @Mock
    private DockerClient dockerClient;

    @Mock
    private CopyArchiveFromContainerCmd copyCmd;

    private AutoCloseable mocks;
    private DockerContainerClient client;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (client != null) {
            client.close();
        }
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    @DisplayName("Should support builder chaining")
    void testBuilderChaining() {
        DockerContainerClient.Builder builder = DockerContainerClient.builder();
        assertSame(builder, builder.dockerHost("unix:///var/run/docker.sock"));
    }

    @Test
    @DisplayName("Should read file from container successfully")
    void testReadSuccess() throws Exception {
        client = createTestClient();

        byte[] expectedData = "file-content".getBytes();
        InputStream mockStream = new ByteArrayInputStream(expectedData);

        when(dockerClient.copyArchiveFromContainerCmd(anyString(), anyString()))
            .thenReturn(copyCmd);
        when(copyCmd.exec()).thenReturn(mockStream);

        byte[] result = client.read("container-id", "/path/to/file.txt");

        assertArrayEquals(expectedData, result);
        verify(dockerClient).copyArchiveFromContainerCmd("container-id", "/path/to/file.txt");
    }

    @Test
    @DisplayName("Should throw IOException on read failure")
    void testReadFailure() throws Exception {
        client = createTestClient();

        when(dockerClient.copyArchiveFromContainerCmd(anyString(), anyString()))
            .thenThrow(new RuntimeException("Docker error"));

        IOException exception = assertThrows(IOException.class,
            () -> client.read("container-id", "/path/file.txt"));

        assertTrue(exception.getMessage().contains("Failed to read from Docker container"));
        assertTrue(exception.getMessage().contains("container-id"));
        assertTrue(exception.getMessage().contains("/path/file.txt"));
    }

    @Test
    @DisplayName("Should check exists returns true when file exists")
    void testExistsTrue() throws Exception {
        client = createTestClient();

        byte[] data = "content".getBytes();
        InputStream mockStream = new ByteArrayInputStream(data);

        when(dockerClient.copyArchiveFromContainerCmd(anyString(), anyString()))
            .thenReturn(copyCmd);
        when(copyCmd.exec()).thenReturn(mockStream);

        assertTrue(client.exists("container-id", "/file.txt"));
    }

    @Test
    @DisplayName("Should check exists returns false when file doesn't exist")
    void testExistsFalse() throws Exception {
        client = createTestClient();

        when(dockerClient.copyArchiveFromContainerCmd(anyString(), anyString()))
            .thenThrow(new RuntimeException("Not found"));

        assertFalse(client.exists("container-id", "/missing.txt"));
    }

    @Test
    @DisplayName("Should throw UnsupportedOperationException for listKeys")
    void testListKeysUnsupported() throws Exception {
        client = createTestClient();

        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> client.listKeys("container-id")
        );

        assertTrue(exception.getMessage().contains("Docker does not support directory listing"));
    }

    @Test
    @DisplayName("Should throw UnsupportedOperationException for readAll")
    void testReadAllUnsupported() throws Exception {
        client = createTestClient();

        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> client.readAll("container-id")
        );

        assertTrue(exception.getMessage().contains("Docker does not support reading all files"));
    }

    @Test
    @DisplayName("Should return description")
    void testGetDescription() throws Exception {
        client = createTestClient();
        assertEquals("Docker[]", client.getDescription());
    }

    @Test
    @DisplayName("Should close Docker client")
    void testClose() throws Exception {
        client = createTestClient();
        client.close();
        verify(dockerClient).close();
    }

    @Test
    @DisplayName("Should wrap exception on close failure")
    void testCloseFailure() throws Exception {
        client = createTestClient();

        doThrow(new RuntimeException("Close error")).when(dockerClient).close();

        IOException exception = assertThrows(IOException.class, () -> client.close());
        assertTrue(exception.getMessage().contains("Failed to close Docker client"));

        client = null; // Prevent tearDown from attempting to close again
    }

    @Test
    @DisplayName("Should handle null Docker client in close")
    void testCloseNullClient() throws Exception {
        client = createTestClient();

        java.lang.reflect.Field field = DockerContainerClient.class.getDeclaredField("dockerClient");
        field.setAccessible(true);
        field.set(client, null);

        assertDoesNotThrow(() -> client.close());
    }

    @Test
    @DisplayName("Should throw NullPointerException when dockerClient is null")
    void testConstructorNullDockerClient() throws Exception {
        java.lang.reflect.Constructor<DockerContainerClient> constructor =
            DockerContainerClient.class.getDeclaredConstructor(DockerClient.class);
        constructor.setAccessible(true);

        java.lang.reflect.InvocationTargetException exception = assertThrows(
            java.lang.reflect.InvocationTargetException.class,
            () -> constructor.newInstance((DockerClient) null)
        );

        assertTrue(exception.getCause() instanceof NullPointerException);
        assertTrue(exception.getCause().getMessage().contains("dockerClient cannot be null"));
    }

    private DockerContainerClient createTestClient() throws Exception {
        java.lang.reflect.Constructor<DockerContainerClient> constructor =
            DockerContainerClient.class.getDeclaredConstructor(DockerClient.class);
        constructor.setAccessible(true);
        return constructor.newInstance(dockerClient);
    }
}
