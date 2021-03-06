package tech.bugger.control.backing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.bugger.LogExtension;
import tech.bugger.business.internal.UserSession;
import tech.bugger.business.service.NotificationService;
import tech.bugger.business.service.TopicService;
import tech.bugger.business.util.MarkdownHandler;
import tech.bugger.business.util.Paginator;
import tech.bugger.control.exception.Error404Exception;
import tech.bugger.global.transfer.Notification;
import tech.bugger.global.transfer.Topic;
import tech.bugger.global.transfer.User;

import javax.faces.context.ExternalContext;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(LogExtension.class)
@ExtendWith(MockitoExtension.class)
public class HomeBackerTest {

    private HomeBacker homeBacker;

    @Mock
    private UserSession session;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TopicService topicService;

    @Mock
    private ExternalContext ectx;

    @Mock
    private Paginator<Notification> inboxMock;

    private final Topic testTopic1 = new Topic(1, "Hi", "senberg");
    private final Topic testTopic2 = new Topic(2, "Hi", "performance");
    private final Topic testTopic3 = new Topic(3, "Hi", "de and seek");
    private final Notification notification = new Notification();
    private User user;

    @BeforeEach
    public void setUp() {
        user = new User();
        this.homeBacker = new HomeBacker(session, notificationService, topicService, ectx);
    }

    @Test
    public void testInit() {
        List<Topic> topicsMock = new ArrayList<>();
        topicsMock.add(testTopic1);
        topicsMock.add(testTopic2);
        topicsMock.add(testTopic3);
        doReturn(topicsMock).when(topicService).selectTopics(any());
        doReturn(topicsMock.size()).when(topicService).countTopics();
        homeBacker.init();
        assertAll(
                () -> assertNotNull(homeBacker.getTopics()),
                () -> assertEquals(topicsMock.size(), homeBacker.getTopics().getSelection().getTotalSize()),
                () -> assertEquals(topicsMock, homeBacker.getTopics().getWrappedData())
        );
    }

    @Test
    public void testInitUserNotNull() {
        doReturn(new User()).when(session).getUser();
        homeBacker.init();
        assertAll(
                () -> assertNotNull(homeBacker.getInbox()),
                () -> assertNotNull(homeBacker.getTopics())
        );
    }

    @Test
    public void testInbox() {
        ArrayList<Notification> selectedNotifications = new ArrayList<>();
        selectedNotifications.add(new Notification());
        doReturn(selectedNotifications).when(notificationService).selectNotifications(any(), any());
        doReturn(selectedNotifications.size()).when(notificationService).countNotifications(any());
        doReturn(user).when(session).getUser();
        assertAll(
                () -> assertDoesNotThrow(() -> homeBacker.init()),
                () -> assertEquals(selectedNotifications.size(), homeBacker.getInbox().getSelection().getTotalSize()),
                () -> assertEquals(selectedNotifications, homeBacker.getInbox().getWrappedData())
        );
    }

    @Test
    public void testDeleteNotification() throws Exception {
        setupInbox();
        assertNull(homeBacker.deleteNotification(notification));
        verify(notificationService).deleteNotification(notification);
        verify(inboxMock).updateReset();
    }

    private void setupInbox() throws NoSuchFieldException, IllegalAccessException {
        Field inbox = homeBacker.getClass().getDeclaredField("inbox");
        inbox.setAccessible(true);
        inbox.set(homeBacker, inboxMock);
    }

    @Test
    public void testOpenNotification() throws Exception {
        notification.setPostID(100);
        assertNull(homeBacker.openNotification(notification));
        verify(ectx).redirect(any());
    }

    @Test
    public void testOpenNotificationPostIdNull() throws Exception {
        notification.setTopicID(100);
        assertNull(homeBacker.openNotification(notification));
        verify(ectx).redirect(any());
    }

    @Test
    public void testOpenNotificationIOException() throws Exception {
        doThrow(IOException.class).when(ectx).redirect(any());
        notification.setPostID(100);
        assertThrows(Error404Exception.class, () -> homeBacker.openNotification(notification));
    }

    @Test
    public void testOpenNotificationWhenMarkAsReadFails() {
        assertNull(homeBacker.openNotification(notification));
    }

    @Test
    public void testIsSubscribed() {
        assertFalse(homeBacker.isSubscribed(testTopic1));
    }

    @Test
    public void testIsSubscribedWhenUserIsSubscribed() {
        doReturn(user).when(session).getUser();
        doReturn(true).when(topicService).isSubscribed(any(), any());
        assertTrue(homeBacker.isSubscribed(testTopic1));
    }

    @Test
    public void testLastChange() {
        OffsetDateTime mockDate = OffsetDateTime.now();
        doReturn(mockDate).when(topicService).lastChange(any());
        assertEquals(mockDate, homeBacker.lastChange(testTopic1));
    }

    @Test
    public void testGetDescriptionWhenDescriptionIsNull() {
        assertEquals("", homeBacker.getDescription(new Topic()));
    }

    @Test
    public void testGetDescription() {
        try (MockedStatic<MarkdownHandler> markdownHandlerMockedStatic = mockStatic(MarkdownHandler.class)) {
            markdownHandlerMockedStatic.when(() -> MarkdownHandler.toHtml("senberg")).thenReturn("Walter White");
            assertEquals("Walter White", homeBacker.getDescription(testTopic1));
        }
    }

    @Test
    public void testGetHelpSuffix() {
        doReturn(user).when(session).getUser();
        assertEquals("_user", homeBacker.getHelpSuffix());
    }

    @Test
    public void testGetHelpSuffixAdmin() {
        user.setAdministrator(true);
        doReturn(user).when(session).getUser();
        assertEquals("_admin", homeBacker.getHelpSuffix());
    }

    @Test
    public void testGetHelpSuffixNoUser() {
        assertEquals("", homeBacker.getHelpSuffix());
    }

    @Test
    public void testDisplayDialog() {
        HomeBacker.Dialog dialog = HomeBacker.Dialog.DELETE_ALL_NOTIFICATIONS;
        assertNull(homeBacker.displayDialog(dialog));
        assertEquals(dialog, homeBacker.getCurrentDialog());
    }

    @Test
    public void testDeleteAllNotifications() throws Exception {
        setupInbox();
        doReturn(user).when(session).getUser();
        assertNull(homeBacker.deleteAllNotifications());
        assertNull(homeBacker.getCurrentDialog());
        verify(notificationService).deleteAllNotifications(user);
        verify(inboxMock).updateReset();
    }

}