package tech.bugger.business.service;

import tech.bugger.business.internal.ApplicationSettings;
import tech.bugger.business.util.Feedback;
import tech.bugger.business.util.RegistryKey;
import tech.bugger.global.transfer.Attachment;
import tech.bugger.global.transfer.Post;
import tech.bugger.global.transfer.Report;
import tech.bugger.global.transfer.User;
import tech.bugger.global.util.Log;
import tech.bugger.persistence.exception.NotFoundException;
import tech.bugger.persistence.exception.TransactionException;
import tech.bugger.persistence.gateway.AttachmentGateway;
import tech.bugger.persistence.util.Transaction;
import tech.bugger.persistence.util.TransactionManager;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Service providing methods related to posts and attachments. A {@code Feedback} event is fired, if unexpected
 * circumstances occur.
 */
@ApplicationScoped
public class PostService {

    /**
     * The {@link Log} instance associated with this class for logging purposes.
     */
    private static final Log log = Log.forClass(PostService.class);

    /**
     * Notification service used for sending notifications.
     */
    private final NotificationService notificationService;

    /**
     * The current application settings.
     */
    private final ApplicationSettings applicationSettings;

    /**
     * Transaction manager used for creating transactions.
     */
    private final TransactionManager transactionManager;

    /**
     * Feedback Event for user feedback.
     */
    private final Event<Feedback> feedbackEvent;

    /**
     * Resource bundle for feedback messages.
     */
    private final ResourceBundle messagesBundle;

    /**
     * Constructs a new post service with the given dependencies.
     *
     * @param notificationService The notification service to use for sending notifications.
     * @param applicationSettings The application settings to use.
     * @param transactionManager  The transaction manager to use for creating transactions.
     * @param feedbackEvent       The feedback event to use for user feedback.
     * @param messagesBundle      The resource bundle for feedback messages.
     */
    @Inject
    public PostService(final NotificationService notificationService, final ApplicationSettings applicationSettings,
                       final TransactionManager transactionManager, final Event<Feedback> feedbackEvent,
                       final @RegistryKey("messages") ResourceBundle messagesBundle) {
        this.notificationService = notificationService;
        this.applicationSettings = applicationSettings;
        this.transactionManager = transactionManager;
        this.feedbackEvent = feedbackEvent;
        this.messagesBundle = messagesBundle;
    }

    /**
     * Updates an existing post and notifies users about the change. Notifications are handled by the {@code
     * NotificationService}.
     *
     * @param post The post to update.
     */
    public void updatePost(final Post post) {
    }

    /**
     * Checks whether an attachment's name is valid according to the current application configuration.
     *
     * @param name The attachment name to check the validity of.
     * @return Whether the attachment name is valid.
     */
    private boolean isAttachmentNameValid(final String name) {
        return Arrays.stream(applicationSettings.getConfiguration().getAllowedFileExtensions().split(","))
                .anyMatch(name::endsWith);
    }

    /**
     * Checks whether a list of attachments is allowed for a post according to the current application configuration.
     *
     * @param attachments The list of attachments to check the validity of.
     * @return Whether the list of attachments is valid.
     */
    public boolean isAttachmentListValid(final List<Attachment> attachments) {
        int maxAttachments = applicationSettings.getConfiguration().getMaxAttachmentsPerPost();
        if (attachments.size() > maxAttachments) {
            log.info("Trying to create post with too many attachments.");
            String message = MessageFormat.format(messagesBundle.getString("too_many_attachments"),
                    maxAttachments);
            feedbackEvent.fire(new Feedback(message, Feedback.Type.ERROR));
            return false;
        }

        if (attachments.size() != attachments.stream().map(Attachment::getName).distinct().count()) {
            log.info("Trying to create post where attachment names are not unique.");
            feedbackEvent.fire(new Feedback(messagesBundle.getString("attachment_names_not_unique"),
                    Feedback.Type.ERROR));
            return false;
        }

        if (!attachments.stream().map(Attachment::getName).allMatch(this::isAttachmentNameValid)) {
            log.info("Trying to create post with invalid attachment name.");
            feedbackEvent.fire(new Feedback(messagesBundle.getString("attachment_names_invalid"),
                    Feedback.Type.ERROR));
            return false;
        }

        return true;
    }

    /**
     * Creates a post using a given {@code Transaction}.
     *
     * @param post The post to be created.
     * @param tx   The transaction to use when creating the post.
     * @return {@code true} iff creating the post succeeded.
     * @throws TransactionException The transaction could not be committed successfully.
     */
    boolean createPostWithTransaction(final Post post, final Transaction tx) throws TransactionException {
        boolean valid = isAttachmentListValid(post.getAttachments());
        if (valid) {
            tx.newPostGateway().create(post);
            AttachmentGateway attachmentGateway = tx.newAttachmentGateway();
            post.getAttachments().forEach(attachmentGateway::create);
        }
        return valid;
    }

    /**
     * Creates a new post for an existing report and notifies users about the creation. Notifications are handled by the
     * {@code NotificationService}.
     *
     * @param post The post to be created.
     * @return {@code true} iff creating the post succeeded.
     */
    public boolean createPost(final Post post) {
        // Notifications will be dealt with when implementing the subscriptions feature.
        try (Transaction tx = transactionManager.begin()) {
            boolean success = createPostWithTransaction(post, tx);
            if (success) {
                tx.commit();
                log.info("Post created successfully.");
                feedbackEvent.fire(new Feedback(messagesBundle.getString("post_created"), Feedback.Type.INFO));
            }
            return success;
        } catch (TransactionException e) {
            log.error("Error while creating a new post.", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("create_failure"), Feedback.Type.ERROR));
            return false;
        }
    }

    /**
     * Irreversibly deletes a post and its attachments.
     *
     * @param post The post to be deleted.
     */
    public void deletePost(final Post post) {
        if (post == null) {
            log.error("Cannot delete post null.");
            throw new IllegalArgumentException("Post cannot be null.");
        }
        Report report = post.getReport().get();
        try (Transaction tx = transactionManager.begin()) {
            Post firstPost = tx.newPostGateway().getFirstPost(report);
            if (post.equals(firstPost)) {
                tx.newReportGateway().delete(report);
            } else {
                tx.newPostGateway().delete(post);
            }
            tx.commit();
        } catch (NotFoundException e) {
            log.error("Post to be deleted " + post + " not found.", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (TransactionException e) {
            log.error("Error when deleting post " + post + ".", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("data_access_error"), Feedback.Type.ERROR));
        }
    }

    /**
     * Returns the post with the specified ID. If no such post exists, returns {@code null} and fires an event.
     *
     * @param id The ID of the post to be returned.
     * @return The post with the specified ID if it exists, {@code null} if no post with that ID exists.
     */
    public Post getPostByID(final int id) {
        return null;
    }

    /**
     * Returns the attachments of one particular post.
     *
     * @param post The post in question.
     * @return A list of attachments that may be empty.
     */
    public List<Attachment> getAttachmentsForPost(final Post post) {
        return null;
    }

    /**
     * Creates a new attachment in the data storage.
     *
     * @param attachment The attachment to be created.
     */
    public void createAttachment(final Attachment attachment) {

    }

    /**
     * Create several new attachments at once.
     *
     * @param attachments The list of attachments to be created.
     */
    public void createMultipleAttachments(final List<Attachment> attachments) {

    }

    /**
     * Checks if a user is allowed to modify a certain post. Administrators can modify any post, moderators can modify
     * all posts within their moderated topic, regular users can modify their own posts as long as they have not been
     * banned from the topic the post belongs to. Anonymous users cannot modify any posts.
     *
     * @param user The user in question.
     * @param post The post in question.
     * @return {@code true} if the user is allowed to modify the post, {@code false} otherwise.
     */
    public boolean isPrivileged(final User user, final Post post) {
        // TODO add moderator check, ban check
        if (user == null) {
            return false;
        } else if (user.isAdministrator()) {
            return true;
        } else {
            return user.equals(post.getAuthorship().getCreator());
        }
    }

}
