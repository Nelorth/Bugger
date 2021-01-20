package tech.bugger.business.internal;

import com.ocpsoft.pretty.PrettyContext;
import com.sun.faces.lifecycle.Phase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.bugger.LogExtension;
import tech.bugger.business.util.Registry;
import tech.bugger.global.transfer.Configuration;
import tech.bugger.global.transfer.User;
import tech.bugger.global.util.Log;
import tech.bugger.persistence.exception.TransactionException;
import tech.bugger.persistence.gateway.TokenGateway;
import tech.bugger.persistence.gateway.UserGateway;
import tech.bugger.persistence.util.Transaction;
import tech.bugger.persistence.util.TransactionManager;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(LogExtension.class)
@ExtendWith(MockitoExtension.class)
public class TrespassListenerTest {

    private MockedStatic<CDI> cdiStaticMock;
    private MockedStatic<PrettyContext> prettyContextStaticMock;

    @Mock
    private ApplicationSettings applicationSettings;

    @Mock
    private Configuration configuration;

    @Mock
    private Registry registry;

    private Instance<UserSession> sessionInstance;

    @Mock
    private PhaseEvent event;

    @Mock
    private FacesContext fctx;

    @Mock
    private ExternalContext ectx;

    private TrespassListener trespassListener;

    @BeforeEach
    public void setUp() throws Exception {
        prettyContextStaticMock = Mockito.mockStatic(PrettyContext.class);
        prettyContextStaticMock.when(PrettyContext::getCurrentInstance).thenReturn(mock(PrettyContext.class));

        cdiStaticMock = Mockito.mockStatic(CDI.class);
        CDI cdi = mock(CDI.class);
        cdiStaticMock.when(CDI::current).thenReturn(cdi);

        Instance<ApplicationSettings> applicationSettingsInstance = mock(Instance.class);
        doReturn(applicationSettingsInstance).when(cdi).select(ApplicationSettings.class);
        doReturn(applicationSettings).when(applicationSettingsInstance).get();

        Instance<Registry> registryInstance = mock(Instance.class);
        doReturn(registryInstance).when(cdi).select(Registry.class);
        doReturn(registry).when(registryInstance).get();

        sessionInstance = mock(Instance.class);
        lenient().doReturn(sessionInstance).when(cdi).select(UserSession.class);

        trespassListener = new TrespassListener();

        lenient().doReturn(fctx).when(event).getFacesContext();
        lenient().doReturn(ectx).when(fctx).getExternalContext();
        lenient().doReturn(mock(Flash.class)).when(ectx).getFlash();
        lenient().doReturn(mock(ResourceBundle.class)).when(registry).getBundle(anyString(), any());
        lenient().doReturn(configuration).when(applicationSettings).getConfiguration();
    }

    @AfterEach
    public void tearDown() {
        cdiStaticMock.close();
        prettyContextStaticMock.close();
    }

    @Test
    public void testGetPhaseId() {
        assertEquals(PhaseId.RESTORE_VIEW, trespassListener.getPhaseId());
    }

    @Test
    public void testAfterPhaseViewRootNull() throws Exception {
        doReturn(null).when(fctx).getViewRoot();
        trespassListener.afterPhase(event);
        verify(ectx).redirect(anyString());
    }

    @Test
    public void testAfterPhaseViewIdNull() throws Exception {
        UIViewRoot viewRoot = mock(UIViewRoot.class);
        doReturn(viewRoot).when(fctx).getViewRoot();
        doReturn(null).when(viewRoot).getViewId();
        trespassListener.afterPhase(event);
        verify(ectx).redirect(anyString());
    }

    @Test
    public void testAfterPhasePublic() throws Exception {
        UIViewRoot viewRoot = mock(UIViewRoot.class);
        doReturn(viewRoot).when(fctx).getViewRoot();
        doReturn("/view/public/weisswurscht.xhtml").when(viewRoot).getViewId();
        doReturn(null).when(sessionInstance).get();
        trespassListener.afterPhase(event);
        verify(ectx, never()).redirect(anyString());
    }

    @Test
    public void testAfterPhaseAdminUserNull() throws Exception {
        UIViewRoot viewRoot = mock(UIViewRoot.class);
        doReturn(viewRoot).when(fctx).getViewRoot();
        doReturn("/test/admin.xhtml").when(viewRoot).getViewId();
        doReturn(null).when(sessionInstance).get();
        trespassListener.afterPhase(event);
        verify(ectx).redirect(anyString());
    }

    @Test
    public void testAfterPhaseAdminUserNotAdmin() throws Exception {
        UIViewRoot viewRoot = mock(UIViewRoot.class);
        doReturn(viewRoot).when(fctx).getViewRoot();
        doReturn("/test/admin.xhtml").when(viewRoot).getViewId();

        UserSession session = mock(UserSession.class);
        User user = new User();
        user.setAdministrator(false);
        doReturn(user).when(session).getUser();
        doReturn(session).when(sessionInstance).get();

        doThrow(IOException.class).when(ectx).redirect(anyString());
        assertThrows(InternalError.class, () -> trespassListener.afterPhase(event));
    }

    @Test
    public void testAfterPhaseRestricted() throws Exception {
        UIViewRoot viewRoot = mock(UIViewRoot.class);
        doReturn(viewRoot).when(fctx).getViewRoot();
        doReturn("/view/restr/weisswurscht.xhtml").when(viewRoot).getViewId();
        doReturn(null).when(sessionInstance).get();
        trespassListener.afterPhase(event);
        verify(ectx).redirect(anyString());
        verify(fctx).addMessage(any(), any());
    }

    @Test
    public void testAfterPhaseAuthorized() throws Exception {
        UIViewRoot viewRoot = mock(UIViewRoot.class);
        doReturn(viewRoot).when(fctx).getViewRoot();
        doReturn("/view/auth/weisswurscht.xhtml").when(viewRoot).getViewId();
        doReturn(null).when(sessionInstance).get();
        doReturn(false).when(configuration).isGuestReading();

        doThrow(IOException.class).when(ectx).redirect(anyString());
        assertThrows(InternalError.class, () -> trespassListener.afterPhase(event));
    }

}
