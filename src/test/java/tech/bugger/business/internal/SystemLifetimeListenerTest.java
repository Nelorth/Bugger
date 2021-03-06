package tech.bugger.business.internal;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import tech.bugger.LogExtension;
import tech.bugger.business.util.PriorityExecutor;
import tech.bugger.business.util.Registry;
import tech.bugger.global.transfer.Metadata;
import tech.bugger.global.util.Log;
import tech.bugger.persistence.exception.TransactionException;
import tech.bugger.persistence.gateway.MetadataGateway;
import tech.bugger.persistence.gateway.NotificationGateway;
import tech.bugger.persistence.util.ConnectionPool;
import tech.bugger.persistence.util.PropertiesReader;
import tech.bugger.persistence.util.Transaction;
import tech.bugger.persistence.util.TransactionManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(LogExtension.class)
public class SystemLifetimeListenerTest {

    private static final int PORT = 42424;

    private SystemLifetimeListener systemLifetimeListenerMock;

    private MockedStatic<Log> logStaticMock;
    private MockedStatic<Runtime> runtimeStaticMock;

    private Registry registry;
    private ConnectionPool connectionPoolMock;
    private PriorityExecutor priorityExecutorMock;
    private Transaction transactionMock;
    private MetadataGateway metadataGatewayMock;
    private Runtime runtimeMock;

    private ServletContextEvent sceMock;
    private ServletContext sctxMock;

    private static EmbeddedPostgres pg;

    private NotificationGateway notificationGatewayMock;

    @BeforeAll
    public static void setUpAll() throws Exception {
        while (isPortBlocked()) ;
        pg = EmbeddedPostgres.builder().setPort(PORT).start();
    }

    @AfterAll
    public static void tearDownAll() throws Exception {
        pg.close();
    }

    private static boolean isPortBlocked() {
        try (Socket ignored = new Socket("localhost", PORT)) {
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        logStaticMock = mockStatic(Log.class);
        Log log = mock(Log.class);
        doNothing().when(log).debug(any());
        doNothing().when(log).info(any());
        doNothing().when(log).warning(any());
        doNothing().when(log).error(any());
        doNothing().when(log).debug(any(), any());
        doNothing().when(log).info(any(), any());
        doNothing().when(log).warning(any(), any());
        doNothing().when(log).error(any(), any());
        logStaticMock.when(() -> Log.forClass(any())).thenReturn(log);

        registry = mock(Registry.class);
        TransactionManager transactionManagerMock = mock(TransactionManager.class);

        systemLifetimeListenerMock = new SystemLifetimeListener();
        systemLifetimeListenerMock.setRegistry(registry);
        systemLifetimeListenerMock.setTransactionManager(transactionManagerMock);

        PropertiesReader propertiesReader = mock(PropertiesReader.class);
        when(propertiesReader.getString(any())).thenReturn("");
        when(propertiesReader.getString("DB_DRIVER")).thenReturn("org.postgresql.Driver");
        when(propertiesReader.getString("DB_URL")).thenReturn("jdbc:postgresql://localhost:" + PORT + "/postgres");
        when(propertiesReader.getInt(any())).thenReturn(1);
        when(propertiesReader.getBoolean(any())).thenReturn(false);
        when(registry.getPropertiesReader(anyString())).thenReturn(propertiesReader);

        connectionPoolMock = mock(ConnectionPool.class);
        doNothing().when(connectionPoolMock).shutdown();
        when(registry.getConnectionPool(anyString())).thenReturn(connectionPoolMock);

        priorityExecutorMock = mock(PriorityExecutor.class);
        when(priorityExecutorMock.shutdown(anyLong())).thenReturn(true);
        when(registry.getPriorityExecutor(anyString())).thenReturn(priorityExecutorMock);

        transactionMock = mock(Transaction.class);
        metadataGatewayMock = mock(MetadataGateway.class);
        notificationGatewayMock = mock(NotificationGateway.class);
        when(metadataGatewayMock.retrieveMetadata()).thenReturn(null);
        when(transactionMock.newMetadataGateway()).thenReturn(metadataGatewayMock);
        when(transactionMock.newNotificationGateway()).thenReturn(notificationGatewayMock);
        // doReturn(notificationGateway).when(transactionMock).newNotificationGateway();
        when(transactionManagerMock.begin()).thenReturn(transactionMock);

        runtimeMock = mock(Runtime.class);
        runtimeStaticMock = mockStatic(Runtime.class);
        runtimeStaticMock.when(Runtime::getRuntime).thenReturn(runtimeMock);

        sceMock = mock(ServletContextEvent.class);
        sctxMock = mock(ServletContext.class);
        when(sctxMock.getResourceAsStream(any())).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(sctxMock.getResourceAsStream("/WEB-INF/jdbc.properties"))
                .thenReturn(ClassLoader.getSystemResourceAsStream("jdbc.properties"));
        when(sceMock.getServletContext()).thenReturn(sctxMock);
    }

    @AfterEach
    public void tearDown() {
        logStaticMock.close();
        runtimeStaticMock.close();
    }

    @Test
    public void testContextInitializedInitializesLog() {
        systemLifetimeListenerMock.contextInitialized(sceMock);
        logStaticMock.verify(() -> Log.init(any()));
    }

    @Test
    public void testContextInitializedWhenLogInitFails() {
        logStaticMock.when(() -> Log.init(any())).thenThrow(IOException.class);
        assertThrows(InternalError.class, () -> systemLifetimeListenerMock.contextInitialized(sceMock));
    }

    @Test
    public void testContextInitializedInitializesConfigReader() {
        systemLifetimeListenerMock.contextInitialized(sceMock);
        verify(registry).registerPropertiesReader(any(), any());
    }

    @Test
    public void testContextInitializedWhenConfigLoadingFails() throws IOException {
        InputStream badStream = new BufferedInputStream(new ByteArrayInputStream(new byte[0]));
        badStream.close();
        when(sctxMock.getResourceAsStream("/WEB-INF/config.properties")).thenReturn(badStream);
        assertThrows(InternalError.class, () -> systemLifetimeListenerMock.contextInitialized(sceMock));
    }

    @Test
    public void testContextInitializedInitializesConnectionPool() {
        systemLifetimeListenerMock.contextInitialized(sceMock);
        verify(registry).registerConnectionPool(any(), any());
    }

    @Test
    public void testContextInitializedWhenConnectionPoolInitFails() throws IOException {
        InputStream badStream = new BufferedInputStream(new ByteArrayInputStream(new byte[0]));
        badStream.close();
        when(sctxMock.getResourceAsStream("/WEB-INF/jdbc.properties")).thenReturn(badStream);
        assertThrows(InternalError.class, () -> systemLifetimeListenerMock.contextInitialized(sceMock));
    }

    @Test
    public void testContextInitializedInitializesDatabaseSchema() {
        systemLifetimeListenerMock.contextInitialized(sceMock);
        verify(metadataGatewayMock).initializeSchema(any());
    }

    @Test
    public void testContextInitializedWhenSchemaAlreadyPresent() {
        Metadata metadataMock = mock(Metadata.class);
        when(metadataGatewayMock.retrieveMetadata()).thenReturn(metadataMock);
        systemLifetimeListenerMock.contextInitialized(sceMock);
        verify(metadataGatewayMock, times(0)).initializeSchema(any());
    }

    @Test
    public void testContextInitializedWhenSchemaLoadingFails() {
        when(sctxMock.getResourceAsStream("/WEB-INF/setup.sql")).thenReturn(null);
        assertThrows(InternalError.class, () -> systemLifetimeListenerMock.contextInitialized(sceMock));
    }

    @Test
    public void testContextInitializedWhenSchemaTransactionError() throws Exception {
        doThrow(TransactionException.class).when(transactionMock).commit();
        assertThrows(InternalError.class, () -> systemLifetimeListenerMock.contextInitialized(sceMock));
    }

    @Test
    public void testContextInitializedInitializesMailer() {
        systemLifetimeListenerMock.contextInitialized(sceMock);
        verify(registry).registerMailer(any(), any());
    }

    @Test
    public void testContextInitializedWhenMailerInitFails() throws IOException {
        InputStream badStream = new BufferedInputStream(new ByteArrayInputStream(new byte[0]));
        badStream.close();
        when(sctxMock.getResourceAsStream("/WEB-INF/mailing.properties")).thenReturn(badStream);
        assertThrows(InternalError.class, () -> systemLifetimeListenerMock.contextInitialized(sceMock));
    }

    @Test
    public void testContextInitializedInitializesPriorityExecutor() {
        systemLifetimeListenerMock.contextInitialized(sceMock);
        verify(registry).registerPriorityExecutor(any(), any());
    }

    @Test
    public void testContextInitializedAddsShutdownHooks() {
        systemLifetimeListenerMock.contextInitialized(sceMock);
        verify(runtimeMock, times(3)).addShutdownHook(any());
    }

    @Test
    public void testContextDestroyedShutsDownConnectionPool() {
        systemLifetimeListenerMock.contextInitialized(sceMock);
        systemLifetimeListenerMock.contextDestroyed(sceMock);
        verify(connectionPoolMock).shutdown();
    }

    @Test
    public void testContextDestroyedShutsDownPriorityExecutor() throws Exception {
        systemLifetimeListenerMock.contextInitialized(sceMock);
        systemLifetimeListenerMock.contextDestroyed(sceMock);
        verify(priorityExecutorMock).shutdown(anyLong());
    }

    @Test
    public void testContextDestroyedDeregistersShutdownHooks() {
        systemLifetimeListenerMock.contextInitialized(sceMock);
        systemLifetimeListenerMock.contextDestroyed(sceMock);
        verify(runtimeMock, times(3)).removeShutdownHook(any());
    }

}
