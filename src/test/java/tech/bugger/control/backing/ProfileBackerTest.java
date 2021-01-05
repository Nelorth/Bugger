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
import tech.bugger.business.service.ProfileService;
import tech.bugger.global.transfer.Language;
import tech.bugger.global.transfer.User;

import javax.faces.context.ExternalContext;
import java.io.IOException;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;

@ExtendWith(LogExtension.class)
public class ProfileBackerTest {

    @InjectMocks
    private ProfileBacker profileBacker;

    @Mock
    private ProfileService profileService;

    @Mock
    private UserSession session;

    @Mock
    private ExternalContext ext;

    @Mock
    private RequestParameterMap map;

    private User user;
    private int theAnswer = 42;

    @BeforeEach
    public void setup()
    {
        user = new User(12345, "Helgi", "v3ry_s3cur3", "salt", "algorithm", "helga@web.de", "Helga", "Brötchen", null,
                new byte[]{1}, "Hallo, ich bin die Helgi | Perfect | He/They/Her | vergeben | Abo =|= endorsement",
                Language.GERMAN, User.ProfileVisibility.MINIMAL, ZonedDateTime.now(), null, false);
        MockitoAnnotations.openMocks(this);
        when(ext.getRequestParameterMap()).thenReturn(map);
    }

    @Test
    public void testInit() {
        when(map.containsKey("id")).thenReturn(true);
        when(map.get("id")).thenReturn("12345");
        profileBacker.setUserID(user.getId());
        when(profileService.getUser(user.getId())).thenReturn(user);
        profileBacker.init();
        assertAll(
                () -> assertEquals(user, profileBacker.getUser()),
                () -> assertEquals(profileBacker.getDisplayDialog(), ProfileBacker.DialogType.NONE)
        );
    }

    @Test
    public void testInitEqualUser() {
        when(map.containsKey("id")).thenReturn(true);
        when(map.get("id")).thenReturn("12345");
        profileBacker.setUserID(user.getId());
        when(profileService.getUser(user.getId())).thenReturn(user);
        when(session.getUser()).thenReturn(user);
        profileBacker.init();
        assertAll(
                () -> assertEquals(user, profileBacker.getUser()),
                () -> assertEquals(session.getUser(), profileBacker.getUser())
        );
    }

    @Test
    public void testInitNotEqualUser() {
        when(map.containsKey("id")).thenReturn(true);
        when(map.get("id")).thenReturn("12345");
        profileBacker.setUserID(user.getId());
        User sessionUser = new User(user);
        sessionUser.setId(23456);
        when(profileService.getUser(user.getId())).thenReturn(user);
        when(session.getUser()).thenReturn(sessionUser);
        profileBacker.init();
        assertAll(
                () -> assertEquals(user, profileBacker.getUser()),
                () -> assertNotEquals(session.getUser(), profileBacker.getUser())
        );
    }

    @Test
    public void testInitKeyNotPresent() throws IOException {
        // Since ext.redirect is mocked, it just tries and then executes the rest of the method.
        when(map.get("id")).thenReturn("12345");
        when(profileService.getUser(anyInt())).thenReturn(user);
        profileBacker.init();
        verify(ext, times(1)).redirect(anyString());
    }

    @Test
    public void testInitIOException() throws IOException {
        doThrow(IOException.class).when(ext).redirect(anyString());
        assertThrows(InternalError.class,
                () -> profileBacker.init()
        );
    }

    @Test
    public void testInitNumberFormatException() {
        when(map.containsKey("id")).thenReturn(true);
        when(map.get("id")).thenReturn("abc");
        assertThrows(InternalError.class,
                () -> profileBacker.init()
        );
    }

    @Test
    public void testOpenPromoteDemoteAdminDialog() {
        profileBacker.openPromoteDemoteAdminDialog();
        assertEquals(ProfileBacker.DialogType.ADMIN, profileBacker.getDisplayDialog());
    }

    @Test
    public void testClosePromoteDemoteAdminDialog() throws NoSuchFieldException {
        profileBacker.closePromoteDemoteAdminDialog();
        assertEquals(ProfileBacker.DialogType.NONE, profileBacker.getDisplayDialog());
    }

    @Test
    public void testGetVotingWeight() {
        profileBacker.setUser(user);
        when(profileService.getVotingWeightForUser(user)).thenReturn(theAnswer);
        int votingWeight = profileBacker.getVotingWeight();
        assertEquals(theAnswer, votingWeight);
        verify(profileService, times(1)).getVotingWeightForUser(user);
    }

    @Test
    public void testGetNumberOfPosts() {
        profileBacker.setUser(user);
        when(profileService.getNumberOfPostsForUser(user)).thenReturn(theAnswer);
        int posts = profileBacker.getNumberOfPosts();
        assertEquals(theAnswer, posts);
        verify(profileService, times(1)).getNumberOfPostsForUser(user);
    }

    @Test
    public void testIsPrivilegedEqualUser() {
        profileBacker.setUser(user);
        when(session.getUser()).thenReturn(profileBacker.getUser());
        assertTrue(profileBacker.isPrivileged());
        verify(session, times(3)).getUser();
    }

    @Test
    public void testIsPrivilegedAdmin() {
        user.setAdministrator(true);
        when(session.getUser()).thenReturn(user);
        assertTrue(profileBacker.isPrivileged());
        verify(session, times(2)).getUser();
    }

    @Test
    public void testIsPrivilegedFalse() {
        profileBacker.setUser(user);
        User owner = new User(user);
        owner.setId(45678);
        when(session.getUser()).thenReturn(owner);
        assertFalse(profileBacker.isPrivileged());
        verify(session, times(3)).getUser();
    }

    @Test
    public void testIsPrivilegedNoSessionUser() {
        profileBacker.setUser(user);
        assertFalse(profileBacker.isPrivileged());
        verify(session, times(1)).getUser();
    }

    @Test
    public void testToggleAdmin() {
        user.setAdministrator(true);
        when(session.getUser()).thenReturn(user);
        when(profileService.matchingPassword(any(), any())).thenReturn(true);
        profileBacker.setUser(user);
        profileBacker.toggleAdmin();
        verify(profileService, times(1)).toggleAdmin(user);
        verify(session, times(5)).getUser();
    }

    @Test
    public void testToggleAdminUserNotAdmin() {
        when(session.getUser()).thenReturn(user);
        profileBacker.setUser(user);
        profileBacker.toggleAdmin();
        verify(profileService, times(0)).toggleAdmin(user);
        verify(session, times(2)).getUser();
    }

    @Test
    public void testToggleAdminDifferentSessionUser() {
        User sessionUser = new User(user);
        sessionUser.setId(23456);
        sessionUser.setAdministrator(true);
        when(session.getUser()).thenReturn(sessionUser);
        when(profileService.matchingPassword(any(), any())).thenReturn(true);
        profileBacker.setUser(user);
        profileBacker.toggleAdmin();
        verify(profileService, times(1)).toggleAdmin(user);
        verify(session, times(4)).getUser();
    }

    @Test
    public void testToggleAdminNoSessionUser() {
        profileBacker.setUser(user);
        profileBacker.toggleAdmin();
        verify(profileService, times(0)).toggleAdmin(user);
        verify(session, times(1)).getUser();
    }

    @Test
    public void testToggleAdminWrongPassword() {
        user.setAdministrator(true);
        when(session.getUser()).thenReturn(user);
        when(profileService.matchingPassword(any(), any())).thenReturn(false);
        profileBacker.setUser(user);
        profileBacker.toggleAdmin();
        verify(profileService, times(1)).matchingPassword(any(), any());
        verify(session, times(3)).getUser();
    }
}
