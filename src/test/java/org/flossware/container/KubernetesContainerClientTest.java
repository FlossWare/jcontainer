package org.flossware.container;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for KubernetesContainerClient to achieve 100% coverage.
 */
class KubernetesContainerClientTest {

    @Mock
    private CoreV1Api coreV1Api;

    @Mock
    private ApiClient apiClient;

    @Mock
    private V1ConfigMap configMap;

    @Mock
    private CoreV1Api.APIreadNamespacedConfigMapRequest apiRequest;

    private AutoCloseable mocks;
    private KubernetesContainerClient client;

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
        KubernetesContainerClient.Builder builder = KubernetesContainerClient.builder();
        assertSame(builder, builder.namespace("test-namespace"));
        assertSame(builder, builder.apiClient(apiClient));
    }

    @Test
    @DisplayName("Should read value successfully with Base64 decoding")
    void testReadSuccess() throws Exception {
        client = createTestClient();

        String originalData = "test-data";
        String base64Data = Base64.getEncoder().encodeToString(originalData.getBytes());

        Map<String, String> data = new HashMap<>();
        data.put("key1", base64Data);

        when(coreV1Api.readNamespacedConfigMap(anyString(), anyString())).thenReturn(apiRequest);
        when(apiRequest.execute()).thenReturn(configMap);
        when(configMap.getData()).thenReturn(data);

        byte[] result = client.read("config-map", "key1");

        assertArrayEquals(originalData.getBytes(), result);
        verify(coreV1Api).readNamespacedConfigMap("config-map", "test-namespace");
    }

    @Test
    @DisplayName("Should return null when configMap data is null")
    void testReadNullConfigMapData() throws Exception {
        client = createTestClient();

        when(coreV1Api.readNamespacedConfigMap(anyString(), anyString())).thenReturn(apiRequest);
        when(apiRequest.execute()).thenReturn(configMap);
        when(configMap.getData()).thenReturn(null);

        byte[] result = client.read("config-map", "key1");

        assertNull(result);
    }

    @Test
    @DisplayName("Should return null when key doesn't exist")
    void testReadMissingKey() throws Exception {
        client = createTestClient();

        Map<String, String> data = new HashMap<>();
        data.put("other-key", "value");

        when(coreV1Api.readNamespacedConfigMap(anyString(), anyString())).thenReturn(apiRequest);
        when(apiRequest.execute()).thenReturn(configMap);
        when(configMap.getData()).thenReturn(data);

        byte[] result = client.read("config-map", "missing-key");

        assertNull(result);
    }

    @Test
    @DisplayName("Should return null when ConfigMap not found (404)")
    void testReadNotFound404() throws Exception {
        client = createTestClient();

        when(coreV1Api.readNamespacedConfigMap(anyString(), anyString())).thenReturn(apiRequest);
        when(apiRequest.execute()).thenThrow(new ApiException(404, "Not found"));

        byte[] result = client.read("missing-config", "key1");

        assertNull(result);
    }

    @Test
    @DisplayName("Should throw IOException on API error")
    void testReadApiException() throws Exception {
        client = createTestClient();

        when(coreV1Api.readNamespacedConfigMap(anyString(), anyString())).thenReturn(apiRequest);
        when(apiRequest.execute()).thenThrow(new ApiException(500, "Server error"));

        IOException exception = assertThrows(IOException.class,
            () -> client.read("config-map", "key1"));

        assertTrue(exception.getMessage().contains("Failed to read from Kubernetes ConfigMap"));
        assertTrue(exception.getMessage().contains("config-map"));
    }

    @Test
    @DisplayName("Should check exists returns true")
    void testExistsTrue() throws Exception {
        client = createTestClient();

        Map<String, String> data = new HashMap<>();
        data.put("key1", "value");

        when(coreV1Api.readNamespacedConfigMap(anyString(), anyString())).thenReturn(apiRequest);
        when(apiRequest.execute()).thenReturn(configMap);
        when(configMap.getData()).thenReturn(data);

        assertTrue(client.exists("config-map", "key1"));
    }

    @Test
    @DisplayName("Should check exists returns false for missing key")
    void testExistsFalseMissingKey() throws Exception {
        client = createTestClient();

        Map<String, String> data = new HashMap<>();
        data.put("other-key", "value");

        when(coreV1Api.readNamespacedConfigMap(anyString(), anyString())).thenReturn(apiRequest);
        when(apiRequest.execute()).thenReturn(configMap);
        when(configMap.getData()).thenReturn(data);

        assertFalse(client.exists("config-map", "missing-key"));
    }

    @Test
    @DisplayName("Should check exists returns false when data is null")
    void testExistsFalseNullData() throws Exception {
        client = createTestClient();

        when(coreV1Api.readNamespacedConfigMap(anyString(), anyString())).thenReturn(apiRequest);
        when(apiRequest.execute()).thenReturn(configMap);
        when(configMap.getData()).thenReturn(null);

        assertFalse(client.exists("config-map", "key1"));
    }

    @Test
    @DisplayName("Should check exists returns false on 404")
    void testExistsFalse404() throws Exception {
        client = createTestClient();

        when(coreV1Api.readNamespacedConfigMap(anyString(), anyString())).thenReturn(apiRequest);
        when(apiRequest.execute()).thenThrow(new ApiException(404, "Not found"));

        assertFalse(client.exists("missing-config", "key1"));
    }

    @Test
    @DisplayName("Should throw IOException on exists API error")
    void testExistsApiException() throws Exception {
        client = createTestClient();

        when(coreV1Api.readNamespacedConfigMap(anyString(), anyString())).thenReturn(apiRequest);
        when(apiRequest.execute()).thenThrow(new ApiException(500, "Server error"));

        IOException exception = assertThrows(IOException.class,
            () -> client.exists("config-map", "key1"));

        assertTrue(exception.getMessage().contains("Failed to check existence in Kubernetes"));
    }

    @Test
    @DisplayName("Should list keys successfully")
    void testListKeysSuccess() throws Exception {
        client = createTestClient();

        Map<String, String> data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", "value2");

        when(coreV1Api.readNamespacedConfigMap(anyString(), anyString())).thenReturn(apiRequest);
        when(apiRequest.execute()).thenReturn(configMap);
        when(configMap.getData()).thenReturn(data);

        List<String> keys = client.listKeys("config-map");

        assertEquals(2, keys.size());
        assertTrue(keys.contains("key1"));
        assertTrue(keys.contains("key2"));
    }

    @Test
    @DisplayName("Should list keys returns empty list when data is null")
    void testListKeysNullData() throws Exception {
        client = createTestClient();

        when(coreV1Api.readNamespacedConfigMap(anyString(), anyString())).thenReturn(apiRequest);
        when(apiRequest.execute()).thenReturn(configMap);
        when(configMap.getData()).thenReturn(null);

        List<String> keys = client.listKeys("config-map");

        assertNotNull(keys);
        assertEquals(0, keys.size());
    }

    @Test
    @DisplayName("Should throw IOException on listKeys API error")
    void testListKeysApiException() throws Exception {
        client = createTestClient();

        when(coreV1Api.readNamespacedConfigMap(anyString(), anyString())).thenReturn(apiRequest);
        when(apiRequest.execute()).thenThrow(new ApiException(500, "Server error"));

        IOException exception = assertThrows(IOException.class,
            () -> client.listKeys("config-map"));

        assertTrue(exception.getMessage().contains("Failed to list keys from Kubernetes ConfigMap"));
    }

    @Test
    @DisplayName("Should read all data successfully")
    void testReadAllSuccess() throws Exception {
        client = createTestClient();

        String data1 = Base64.getEncoder().encodeToString("value1".getBytes());
        String data2 = Base64.getEncoder().encodeToString("value2".getBytes());

        Map<String, String> data = new HashMap<>();
        data.put("key1", data1);
        data.put("key2", data2);

        when(coreV1Api.readNamespacedConfigMap(anyString(), anyString())).thenReturn(apiRequest);
        when(apiRequest.execute()).thenReturn(configMap);
        when(configMap.getData()).thenReturn(data);

        Map<String, byte[]> result = client.readAll("config-map");

        assertEquals(2, result.size());
        assertArrayEquals("value1".getBytes(), result.get("key1"));
        assertArrayEquals("value2".getBytes(), result.get("key2"));
    }

    @Test
    @DisplayName("Should read all returns empty map when data is null")
    void testReadAllNullData() throws Exception {
        client = createTestClient();

        when(coreV1Api.readNamespacedConfigMap(anyString(), anyString())).thenReturn(apiRequest);
        when(apiRequest.execute()).thenReturn(configMap);
        when(configMap.getData()).thenReturn(null);

        Map<String, byte[]> result = client.readAll("config-map");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Should throw IOException on readAll API error")
    void testReadAllApiException() throws Exception {
        client = createTestClient();

        when(coreV1Api.readNamespacedConfigMap(anyString(), anyString())).thenReturn(apiRequest);
        when(apiRequest.execute()).thenThrow(new ApiException(500, "Server error"));

        IOException exception = assertThrows(IOException.class,
            () -> client.readAll("config-map"));

        assertTrue(exception.getMessage().contains("Failed to read all from Kubernetes ConfigMap"));
    }

    @Test
    @DisplayName("Should return description")
    void testGetDescription() throws Exception {
        client = createTestClient();
        assertEquals("Kubernetes[namespace=test-namespace]", client.getDescription());
    }

    @Test
    @DisplayName("Should close without error")
    void testClose() throws Exception {
        client = createTestClient();
        assertDoesNotThrow(() -> client.close());
    }

    @Test
    @DisplayName("Should throw NullPointerException when api is null")
    void testConstructorNullApi() throws Exception {
        java.lang.reflect.Constructor<KubernetesContainerClient> constructor =
            KubernetesContainerClient.class.getDeclaredConstructor(CoreV1Api.class, String.class);
        constructor.setAccessible(true);

        java.lang.reflect.InvocationTargetException exception = assertThrows(
            java.lang.reflect.InvocationTargetException.class,
            () -> constructor.newInstance(null, "namespace"));

        assertTrue(exception.getCause() instanceof NullPointerException);
        assertTrue(exception.getCause().getMessage().contains("api cannot be null"));
    }

    @Test
    @DisplayName("Should throw NullPointerException when namespace is null")
    void testConstructorNullNamespace() throws Exception {
        java.lang.reflect.Constructor<KubernetesContainerClient> constructor =
            KubernetesContainerClient.class.getDeclaredConstructor(CoreV1Api.class, String.class);
        constructor.setAccessible(true);

        java.lang.reflect.InvocationTargetException exception = assertThrows(
            java.lang.reflect.InvocationTargetException.class,
            () -> constructor.newInstance(coreV1Api, null));

        assertTrue(exception.getCause() instanceof NullPointerException);
        assertTrue(exception.getCause().getMessage().contains("namespace cannot be null"));
    }

    @Test
    @DisplayName("Should build with custom apiClient and namespace")
    void testBuilderWithCustomApiClient() throws Exception {
        try (org.mockito.MockedConstruction<io.kubernetes.client.openapi.apis.CoreV1Api> coreV1ApiConstruction =
                 org.mockito.Mockito.mockConstruction(io.kubernetes.client.openapi.apis.CoreV1Api.class)) {

            io.kubernetes.client.openapi.ApiClient customClient = mock(io.kubernetes.client.openapi.ApiClient.class);

            KubernetesContainerClient result = KubernetesContainerClient.builder()
                .apiClient(customClient)
                .namespace("custom-namespace")
                .build();

            assertNotNull(result);
            assertTrue(result.getDescription().contains("custom-namespace"));
        }
    }

    @Test
    @DisplayName("Should build with default apiClient and default namespace")
    void testBuilderWithDefaults() throws Exception {
        try (org.mockito.MockedStatic<io.kubernetes.client.util.Config> configStatic =
                 org.mockito.Mockito.mockStatic(io.kubernetes.client.util.Config.class);
             org.mockito.MockedConstruction<io.kubernetes.client.openapi.apis.CoreV1Api> coreV1ApiConstruction =
                 org.mockito.Mockito.mockConstruction(io.kubernetes.client.openapi.apis.CoreV1Api.class)) {

            io.kubernetes.client.openapi.ApiClient defaultClient = mock(io.kubernetes.client.openapi.ApiClient.class);
            configStatic.when(io.kubernetes.client.util.Config::defaultClient).thenReturn(defaultClient);

            KubernetesContainerClient result = KubernetesContainerClient.builder().build();

            assertNotNull(result);
            assertTrue(result.getDescription().contains("default"));
        }
    }

    @Test
    @DisplayName("Should throw IOException when default client creation fails")
    void testBuilderDefaultClientFailure() throws Exception {
        try (org.mockito.MockedStatic<io.kubernetes.client.util.Config> configStatic =
                 org.mockito.Mockito.mockStatic(io.kubernetes.client.util.Config.class)) {

            configStatic.when(io.kubernetes.client.util.Config::defaultClient)
                .thenThrow(new java.io.IOException("Failed to load kubeconfig"));

            IOException exception = assertThrows(IOException.class,
                () -> KubernetesContainerClient.builder().build());

            assertTrue(exception.getMessage().contains("Failed to create Kubernetes API client"));
        }
    }

    private KubernetesContainerClient createTestClient() throws Exception {
        java.lang.reflect.Constructor<KubernetesContainerClient> constructor =
            KubernetesContainerClient.class.getDeclaredConstructor(CoreV1Api.class, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(coreV1Api, "test-namespace");
    }
}
