package tech.bugger.control.backing;

import com.sun.faces.context.RequestParameterMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tech.bugger.LogExtension;
import tech.bugger.business.internal.UserSession;
import tech.bugger.business.service.AuthenticationService;
import tech.bugger.global.transfer.Language;
import tech.bugger.global.transfer.User;
import tech.bugger.global.util.Lazy;

import javax.faces.application.Application;
import javax.faces.application.NavigationHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;

@ExtendWith(LogExtension.class)
public class LoginBackerTest {

    @InjectMocks
    private LoginBacker loginBacker;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private UserSession session;

    @Mock
    private FacesContext fctx;

    @Mock
    private ExternalContext context;

    @Mock
    private RequestParameterMap map;

    @Mock
    private NavigationHandler navHandler;

    @Mock
    private Application application;

    private User user;
    private final String home = "home";
    private final String profile = "profile";

    @BeforeEach
    public void setup() {
        user = new User(12345, "Helgi", "v3rys3cur3", "salt", "algorithm", "helga@web.de", "Helga", "Brötchen",
                new Lazy<>(new byte[0]), null, "Hallo, ich bin die Helgi | Perfect | He/They/Her | vergeben | Abo =|= "
                + "endorsement", Language.GERMAN, User.ProfileVisibility.MINIMAL, ZonedDateTime.now(), null, false);
        MockitoAnnotations.openMocks(this);
        loginBacker = new LoginBacker(authenticationService, session, fctx);
        loginBacker.setUsername(user.getUsername());
        loginBacker.setPassword(user.getPasswordHash());
        when(fctx.getExternalContext()).thenReturn(context);
        when(context.getRequestParameterMap()).thenReturn(map);
    }

    @Test
    public void testInit() {
        loginBacker.init();
        assertEquals(home, loginBacker.getRedirectURL());
    }

    @Test
    public void testInitUserNotNull() {
        when(session.getUser()).thenReturn(user);
        when(fctx.getApplication()).thenReturn(application);
        when(application.getNavigationHandler()).thenReturn(navHandler);
        loginBacker.init();
        verify(navHandler, times(1)).handleNavigation(any(), any(), anyString());
    }

    @Test
    public void testInitRedirectURL() {
        when(map.get(anyString())).thenReturn(home);
        loginBacker.init();
        assertEquals(home, loginBacker.getRedirectURL());
    }

    @Test
    public void testLoginRedirectURL() {
        loginBacker.setRedirectURL(profile);
        when(authenticationService.authenticate(loginBacker.getUsername(), loginBacker.getPassword())).thenReturn(user);
        assertAll(
                () -> assertEquals(profile, loginBacker.login()),
                () -> assertEquals(user, loginBacker.getUser())
        );
        verify(authenticationService, times(1)).authenticate(any(), anyString());
    }

    @Test
    public void testLoginUserNull() {
        when(authenticationService.authenticate(any(), anyString())).thenReturn(null);
        assertAll(
                () -> assertNull(loginBacker.login())
        );
        verify(authenticationService, times(1)).authenticate(any(), anyString());
    }
}
