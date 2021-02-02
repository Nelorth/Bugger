package tech.bugger.control.backing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.bugger.LogExtension;
import tech.bugger.business.internal.ApplicationSettings;
import tech.bugger.business.internal.UserSession;
import tech.bugger.business.service.PostService;
import tech.bugger.business.service.ReportService;
import tech.bugger.business.service.TopicService;
import tech.bugger.control.exception.Error404Exception;
import tech.bugger.global.transfer.Authorship;
import tech.bugger.global.transfer.Configuration;
import tech.bugger.global.transfer.Post;
import tech.bugger.global.transfer.Report;
import tech.bugger.global.transfer.User;

import javax.faces.context.ExternalContext;
import java.time.OffsetDateTime;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(LogExtension.class)
public class ReportBackerTest {

    private ReportBacker reportBacker;

    @Mock
    private TopicService topicService;

    @Mock
    private ReportService reportService;

    @Mock
    private PostService postService;

    @Mock
    private UserSession session;

    @Mock
    private ApplicationSettings settings;

    @Mock
    private ExternalContext ectx;

    @Mock
    private Configuration configuration;

    private User user;
    private Report report;

    @BeforeEach
    public void setUp() {
        reportBacker = new ReportBacker(settings, topicService, reportService, postService, session, ectx);
        user = new User(1, "testuser", "0123456789abcdef", "0123456789abcdef", "SHA3-512", "test@test.de", "Test",
                "User",
                new byte[]{1, 2, 3, 4}, new byte[]{1}, "# I am a test user.",
                Locale.GERMAN, User.ProfileVisibility.MINIMAL, null, null, false);
        report = new Report(100, "Some title", Report.Type.BUG, Report.Severity.RELEVANT, "", mock(Authorship.class),
                mock(OffsetDateTime.class), null, null, false, 1, null);
    }

    @Test
    public void testIsPrivilegedUserNull() {
        reportBacker.setReport(report);
        assertFalse(reportBacker.isPrivileged());
    }

    @Test
    public void testIsBannedUserNull() {
        reportBacker.setReport(report);
        assertFalse(reportBacker.isBanned());
    }

    @Test
    public void testDisplayDialog() {
        assertAll(
                () -> assertNull(reportBacker.displayDialog(ReportBacker.Dialog.DELETE_POST)),
                () -> assertEquals(ReportBacker.Dialog.DELETE_POST, reportBacker.getCurrentDialog())
        );
    }

    @Test
    public void testDeletePostDialog() {
        Post post = new Post(42, "a", 100, null, null);
        assertAll(
                () -> assertNull(reportBacker.deletePostDialog(post)),
                () -> assertEquals(ReportBacker.Dialog.DELETE_POST, reportBacker.getCurrentDialog()),
                () -> assertEquals(post, reportBacker.getPostToBeDeleted())
        );
    }

    @Test
    public void testToggleReportSubscriptionWhenUserIsNull() {
        assertNull(reportBacker.toggleReportSubscription());
        verify(reportService, never()).isSubscribed(any(), any());
    }

    @Test
    public void testToggleReportSubscriptionWhenUserIsSubscribed() {
        doReturn(user).when(session).getUser();
        doReturn(true).when(reportService).isSubscribed(any(), any());
        assertNull(reportBacker.toggleReportSubscription());
        verify(reportService).isSubscribed(eq(user), any());
        verify(reportService).unsubscribeFromReport(eq(user), any());
        verify(reportService, never()).subscribeToReport(any(), any());
    }

    @Test
    public void testToggleReportSubscriptionWhenUserIsNotSubscribed() {
        doReturn(user).when(session).getUser();
        assertNull(reportBacker.toggleReportSubscription());
        verify(reportService).isSubscribed(eq(user), any());
        verify(reportService, never()).unsubscribeFromReport(any(), any());
        verify(reportService).subscribeToReport(eq(user), any());
    }

    @Test
    public void testUpvoteWhenUserIsNull() {
        reportBacker.setReport(report);
        doReturn(report).when(reportService).getReportByID(anyInt());
        assertNull(reportBacker.upvote());
        verify(reportService, never()).upvote(any(), any());
    }

    @Test
    public void testUpvote() {
        reportBacker.setReport(report);
        doReturn(report).when(reportService).getReportByID(anyInt());
        doReturn(user).when(session).getUser();
        assertNull(reportBacker.upvote());
        verify(reportService).upvote(eq(report), eq(user));
    }

    @Test
    public void testUpvoteWhenReportNotFound() {
        reportBacker.setReport(report);
        doReturn(user).when(session).getUser();
        assertThrows(Error404Exception.class, () -> reportBacker.upvote());
    }

    @Test
    public void testUpvoteWhenRelevanceOverwritten() {
        report.setRelevanceOverwritten(true);
        report.setRelevance(42);
        reportBacker.setReport(report);
        doReturn(report).when(reportService).getReportByID(anyInt());
        doReturn(user).when(session).getUser();
        assertNull(reportBacker.upvote());
        assertEquals(42, reportBacker.getOverwriteRelevanceValue());
    }

    @Test
    public void testDownvote() {
        reportBacker.setReport(report);
        doReturn(report).when(reportService).getReportByID(anyInt());
        doReturn(user).when(session).getUser();
        assertNull(reportBacker.downvote());
        verify(reportService).downvote(eq(report), eq(user));
    }

    @Test
    public void testDownvoteWhenUserIsNull() {
        reportBacker.setReport(report);
        doReturn(report).when(reportService).getReportByID(anyInt());
        assertNull(reportBacker.downvote());
        verify(reportService, never()).downvote(any(), any());
    }

    @Test
    public void testRemoveVote() {
        reportBacker.setReport(report);
        doReturn(report).when(reportService).getReportByID(anyInt());
        doReturn(user).when(session).getUser();
        assertNull(reportBacker.removeVote());
        verify(reportService).removeVote(eq(report), eq(user));
    }

    @Test
    public void testRemoveVoteWhenUserIsNull() {
        reportBacker.setReport(report);
        doReturn(report).when(reportService).getReportByID(anyInt());
        assertNull(reportBacker.removeVote());
        verify(reportService, never()).removeVote(any(), any());
    }

    @Test
    public void testToggleOpenClosedWhenReportIsOpen() {
        report.setClosingDate(null);
        reportBacker.setReport(report);
        assertDoesNotThrow(() -> reportBacker.toggleOpenClosed());
        verify(reportService).close(report);
        verify(reportService, never()).open(any());
        assertNull(reportBacker.getCurrentDialog());
    }

    @Test
    public void testToggleOpenClosedWhenReportIsClosed() {
        report.setClosingDate(OffsetDateTime.now());
        reportBacker.setReport(report);
        assertDoesNotThrow(() -> reportBacker.toggleOpenClosed());
        verify(reportService).open(report);
        verify(reportService, never()).close(any());
        assertNull(reportBacker.getCurrentDialog());
    }

    @Test
    public void testDelete() {
        reportBacker.setReport(report);
        assertDoesNotThrow(() -> reportBacker.delete());
        verify(reportService).deleteReport(report);
    }

    @Test
    public void testMarkDuplicate() {
        reportBacker.setReport(report);
    }

}
