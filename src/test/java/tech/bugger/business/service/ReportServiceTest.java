package tech.bugger.business.service;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import javax.enterprise.event.Event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.bugger.LogExtension;
import tech.bugger.ResourceBundleMocker;
import tech.bugger.business.util.Feedback;
import tech.bugger.global.transfer.*;
import tech.bugger.global.util.Lazy;
import tech.bugger.persistence.exception.NotFoundException;
import tech.bugger.persistence.exception.TransactionException;
import tech.bugger.persistence.gateway.PostGateway;
import tech.bugger.persistence.gateway.ReportGateway;
import tech.bugger.persistence.gateway.SubscriptionGateway;
import tech.bugger.persistence.util.Transaction;
import tech.bugger.persistence.util.TransactionManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(LogExtension.class)
@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

    private ReportService service;

    @Mock
    private TopicService topicService;

    @Mock
    private ProfileService profileService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PostService postService;

    @Mock
    private TransactionManager transactionManager;

    @Mock
    private Transaction tx;

    @Mock
    private ReportGateway reportGateway;

    @Mock
    private PostGateway postGateway;

    @Mock
    private SubscriptionGateway subscriptionGateway;

    @Mock
    private Event<Feedback> feedbackEvent;

    private Report testReport;

    private Post testFirstPost;

    @BeforeEach
    public void setUp() {
        service = new ReportService(notificationService, topicService, postService, profileService, transactionManager,
                feedbackEvent, ResourceBundleMocker.mock(""));
        List<Attachment> attachments = Arrays.asList(new Attachment(), new Attachment(), new Attachment());
        testFirstPost = new Post(100, "Some content", new Lazy<>(mock(Report.class)), mock(Authorship.class), attachments);
        User testUser = new User();
        testUser.setId(1);
        Authorship authorship = new Authorship(testUser, ZonedDateTime.now(), testUser, ZonedDateTime.now());
        testReport = new Report(200, "Some title", Report.Type.BUG, Report.Severity.RELEVANT, "", authorship,
                mock(ZonedDateTime.class), null, null, false, 1);

        lenient().doReturn(tx).when(transactionManager).begin();
        lenient().doReturn(reportGateway).when(tx).newReportGateway();
        lenient().doReturn(postGateway).when(tx).newPostGateway();
        lenient().doReturn(subscriptionGateway).when(tx).newSubscriptionGateway();
    }

    @Test
    public void testGetReportByIDWhenExists() throws Exception {
        testReport.setId(100);
        doReturn(testReport).when(reportGateway).find(anyInt());
        assertEquals(testReport, service.getReportByID(100));
    }

    @Test
    public void testGetReportByIDWhenNotExists() throws Exception {
        doThrow(NotFoundException.class).when(reportGateway).find(anyInt());
        assertNull(service.getReportByID(100));
    }

    @Test
    public void testGetReportByIDWhenCommitFails() throws Exception {
        doThrow(TransactionException.class).when(tx).commit();
        assertNull(service.getReportByID(100));
        verify(feedbackEvent).fire(any());
    }

    @Test
    public void testUpdateReportWhenFine() throws Exception {
        assertTrue(service.updateReport(testReport));
        verify(reportGateway).update(testReport);
    }

    @Test
    public void testUpdateReportWhenNotExists() throws Exception {
        doThrow(NotFoundException.class).when(reportGateway).update(any());
        assertFalse(service.updateReport(testReport));
        verify(feedbackEvent).fire(any());
    }

    @Test
    public void testUpdateReportWhenCommitFails() throws Exception {
        doThrow(TransactionException.class).when(tx).commit();
        assertFalse(service.updateReport(testReport));
    }

    @Test
    public void testCreateReportWhenFine() throws Exception {
        doReturn(true).when(postService).createPostWithTransaction(any(), any());
        assertTrue(service.createReport(testReport, testFirstPost));
        verify(reportGateway).create(any());
        verify(tx, times(2)).commit();
    }

    @Test
    public void testCreateReportWhenPostCreationFails() {
        assertFalse(service.createReport(testReport, testFirstPost));
        verify(tx).abort();
    }

    @Test
    public void testCreateReportWhenCommitFails() throws Exception {
        doReturn(true).when(postService).createPostWithTransaction(any(), any());
        doThrow(TransactionException.class).when(tx).commit();
        assertFalse(service.createReport(testReport, testFirstPost));
        verify(feedbackEvent).fire(any());
    }

    @Test
    public void testMarkDuplicateWhenCommitFails() throws Exception {
        doThrow(TransactionException.class).when(tx).commit();
        assertFalse(service.markDuplicate(testReport, 100));
        verify(feedbackEvent, atLeastOnce()).fire(any());
    }

    @Test
    public void testUnmarkDuplicateSuccess() {
        assertTrue(service.unmarkDuplicate(testReport));
    }

    @Test
    public void testUnmarkDuplicateWhenNotFound() throws Exception {
        doThrow(NotFoundException.class).when(reportGateway).unmarkDuplicate(testReport);
        assertFalse(service.unmarkDuplicate(testReport));
        verify(feedbackEvent).fire(any());
    }

    @Test
    public void testUnmarkDuplicateWhenCommitFails() throws Exception {
        doThrow(TransactionException.class).when(tx).commit();
        assertFalse(service.unmarkDuplicate(testReport));
        verify(feedbackEvent).fire(any());
    }

    @Test
    public void testSelectDuplicatesSuccess() {
        List<Report> reports = List.of(new Report(), new Report());
        doReturn(reports).when(reportGateway).selectDuplicates(any(), any());
        assertEquals(reports, service.getDuplicatesFor(testReport, mock(Selection.class)));
    }

    @Test
    public void testGetDuplicatesForWhenCommitFails() throws Exception {
        doThrow(TransactionException.class).when(tx).commit();
        assertEquals(List.of(), service.getDuplicatesFor(testReport, mock(Selection.class)));
        verify(feedbackEvent).fire(any());
    }

    @Test
    public void testGetNumberOfDuplicatesSuccess() throws Exception {
        doReturn(2).when(reportGateway).countDuplicates(testReport);
        assertEquals(2, service.getNumberOfDuplicates(testReport));
    }

    @Test
    public void testGetNumberOfDuplicatesWhenNotFound() throws Exception {
        doThrow(NotFoundException.class).when(reportGateway).countDuplicates(testReport);
        assertEquals(0, service.getNumberOfDuplicates(testReport));
        verify(feedbackEvent).fire(any());
    }

    @Test
    public void testGetNumberOfDuplicatesWhenCommitFails() throws Exception {
        doThrow(TransactionException.class).when(tx).commit();
        assertEquals(0, service.getNumberOfDuplicates(testReport));
        verify(feedbackEvent).fire(any());
    }

}