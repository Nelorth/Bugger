package tech.bugger.business.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.bugger.ResourceBundleMocker;
import tech.bugger.business.internal.ApplicationSettings;
import tech.bugger.business.util.Feedback;
import tech.bugger.global.transfer.Attachment;
import tech.bugger.global.transfer.Authorship;
import tech.bugger.global.transfer.Configuration;
import tech.bugger.global.transfer.Post;
import tech.bugger.global.transfer.Report;
import tech.bugger.global.util.Lazy;
import tech.bugger.persistence.exception.NotFoundException;
import tech.bugger.persistence.exception.TransactionException;
import tech.bugger.persistence.gateway.AttachmentGateway;
import tech.bugger.persistence.gateway.PostGateway;
import tech.bugger.persistence.util.Transaction;
import tech.bugger.persistence.util.TransactionManager;

import javax.enterprise.event.Event;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    private PostService service;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ApplicationSettings applicationSettings;

    private Configuration configuration;

    @Mock
    private TransactionManager transactionManager;

    @Mock
    private Transaction tx;

    @Mock
    private PostGateway postGateway;

    @Mock
    private AttachmentGateway attachmentGateway;

    @Mock
    private Event<Feedback> feedbackEvent;

    private Post testPost;

    @BeforeEach
    public void setUp() {
        service = new PostService(notificationService, applicationSettings, transactionManager, feedbackEvent,
                ResourceBundleMocker.mock(""));
        List<Attachment> attachments = Arrays.asList(
                new Attachment(0, "test1.txt", null, "", null),
                new Attachment(0, "test2.txt", null, "", null),
                new Attachment(0, "test3.txt", null, "", null)
        );
        testPost = new Post(100, "Some content", new Lazy<>(mock(Report.class)), mock(Authorship.class), attachments);

        lenient().doReturn(tx).when(transactionManager).begin();
        lenient().doReturn(postGateway).when(tx).newPostGateway();
        lenient().doReturn(attachmentGateway).when(tx).newAttachmentGateway();
        configuration = new Configuration(false, false, "", ".txt,.mp3", 5, "");
        lenient().doReturn(configuration).when(applicationSettings).getConfiguration();
    }

    @Test
    public void testIsAttachmentListValidWhenFine() {
        assertTrue(service.isAttachmentListValid(testPost.getAttachments()));
    }

    @Test
    public void testIsAttachmentListValidWhenTooManyAttachments() {
        configuration.setMaxAttachmentsPerPost(2);
        assertFalse(service.isAttachmentListValid(testPost.getAttachments()));
        verify(feedbackEvent).fire(any());
    }

    @Test
    public void testIsAttachmentListValidWhenNamesNotUnique() {
        testPost.getAttachments().get(2).setName("test1.txt");
        assertFalse(service.isAttachmentListValid(testPost.getAttachments()));
        verify(feedbackEvent).fire(any());
    }

    @Test
    public void testIsAttachmentListValidWhenNoExtension() {
        testPost.getAttachments().get(2).setName("test");
        assertFalse(service.isAttachmentListValid(testPost.getAttachments()));
        verify(feedbackEvent).fire(any());
    }

    @Test
    public void testIsAttachmentListValidWhenWrongExtension() {
        testPost.getAttachments().get(2).setName("test.gif");
        assertFalse(service.isAttachmentListValid(testPost.getAttachments()));
        verify(feedbackEvent).fire(any());
    }

    @Test
    public void testCreatePostWithTransactionWhenFine() {
        PostService serviceSpy = spy(service);
        lenient().doReturn(true).when(serviceSpy).isAttachmentListValid(any());
        assertTrue(serviceSpy.createPostWithTransaction(testPost, tx));
        verify(postGateway).create(any());
        verify(attachmentGateway, times(3)).create(any());
    }

    @Test
    public void testCreatePostWithTransactionWhenInvalid() {
        PostService serviceSpy = spy(service);
        lenient().doReturn(false).when(serviceSpy).isAttachmentListValid(any());
        assertFalse(serviceSpy.createPostWithTransaction(testPost, tx));
        verify(postGateway, times(0)).create(any());
    }

    @Test
    public void testCreatePostWhenFine() throws Exception {
        PostService serviceSpy = spy(service);
        lenient().doReturn(true).when(serviceSpy).createPostWithTransaction(any(), any());
        assertTrue(serviceSpy.createPost(testPost));
        verify(tx).commit();
    }

    @Test
    public void testCreatePostWhenNoSuccess() throws Exception {
        PostService serviceSpy = spy(service);
        lenient().doReturn(false).when(serviceSpy).createPostWithTransaction(any(), any());
        assertFalse(serviceSpy.createPost(testPost));
    }

    @Test
    public void testCreatePostWhenCommitFails() throws Exception {
        PostService serviceSpy = spy(service);
        lenient().doReturn(true).when(serviceSpy).createPostWithTransaction(any(), any());
        doThrow(TransactionException.class).when(tx).commit();
        assertFalse(serviceSpy.createPost(testPost));
        verify(feedbackEvent).fire(any());
    }

}