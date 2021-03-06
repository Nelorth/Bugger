package tech.bugger.business.service;

import tech.bugger.business.util.Feedback;
import tech.bugger.business.util.RegistryKey;
import tech.bugger.global.transfer.Notification;
import tech.bugger.global.transfer.Post;
import tech.bugger.global.transfer.Report;
import tech.bugger.global.transfer.Selection;
import tech.bugger.global.transfer.Topic;
import tech.bugger.global.transfer.User;
import tech.bugger.global.util.Log;
import tech.bugger.persistence.exception.DuplicateException;
import tech.bugger.persistence.exception.NotFoundException;
import tech.bugger.persistence.exception.SelfReferenceException;
import tech.bugger.persistence.exception.TransactionException;
import tech.bugger.persistence.util.Transaction;
import tech.bugger.persistence.util.TransactionManager;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * Service providing methods related to reports. A {@link Feedback} event is fired, if unexpected circumstances occur.
 */
@RequestScoped
public class ReportService {

    /**
     * The {@link Log} instance associated with this class for logging purposes.
     */
    private static final Log log = Log.forClass(PostService.class);

    /**
     * Notification service used for sending notifications.
     */
    private final NotificationService notificationService;

    /**
     * Topic service for checking bans.
     */
    private final TopicService topicService;

    /**
     * Post service used for creating posts.
     */
    private final PostService postService;

    /**
     * Profile service used for getting voting weight.
     */
    private final ProfileService profileService;

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
     * Constructs a new report service with the given dependencies.
     *
     * @param notificationService The notification service to use.
     * @param topicService        The topic service to use.
     * @param postService         The post service to use.
     * @param profileService      The profile service to use.
     * @param transactionManager  The transaction manager to use for creating transactions.
     * @param feedbackEvent       The feedback event to use for user feedback.
     * @param messagesBundle      The resource bundle for feedback messages.
     */
    @Inject
    public ReportService(final NotificationService notificationService, final TopicService topicService,
                         final PostService postService, final ProfileService profileService,
                         final TransactionManager transactionManager, final Event<Feedback> feedbackEvent,
                         final @RegistryKey("messages") ResourceBundle messagesBundle) {
        this.notificationService = notificationService;
        this.topicService = topicService;
        this.postService = postService;
        this.profileService = profileService;
        this.transactionManager = transactionManager;
        this.feedbackEvent = feedbackEvent;
        this.messagesBundle = messagesBundle;
    }

    /**
     * Subscribes a user to a report. Afterwards, they will receive notifications if the report is moved or edited, new
     * posts are created in the report or existing posts are edited.
     *
     * @param user   The user who will subscribe to the report.
     * @param report The report receiving the subscription.
     */
    public void subscribeToReport(final User user, final Report report) {
        if (user == null) {
            log.error("Anonymous users cannot subscribe to anything.");
            throw new IllegalArgumentException("User cannot be null.");
        } else if (user.getId() == null) {
            log.error("Cannot subscribe when user ID is null.");
            throw new IllegalArgumentException("User ID cannot be null.");
        } else if (report == null) {
            log.error("Cannot subscribe to report null.");
            throw new IllegalArgumentException("Report cannot be null.");
        } else if (report.getId() == null) {
            log.error("Cannot subscribe to report with ID null.");
            throw new IllegalArgumentException("Report ID cannot be null.");
        }

        try (Transaction tx = transactionManager.begin()) {
            tx.newSubscriptionGateway().subscribe(report, user);
            tx.commit();
        } catch (DuplicateException e) {
            log.error("User " + user + " is already subscribed to report " + report + ".");
            feedbackEvent.fire(new Feedback(messagesBundle.getString("already_subscribed"), Feedback.Type.ERROR));
        } catch (NotFoundException e) {
            log.error("User " + user + " or report " + report + " not found.");
            feedbackEvent.fire(new Feedback(messagesBundle.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (TransactionException e) {
            log.error("Error when user " + user + " is subscribing to report " + report + ".");
            feedbackEvent.fire(new Feedback(messagesBundle.getString("data_access_error"), Feedback.Type.ERROR));
        }
    }

    /**
     * Removes the subscription to a certain report from one user.
     *
     * @param user   The user whose subscription is to be removed.
     * @param report The report the user is subscribed to.
     */
    public void unsubscribeFromReport(final User user, final Report report) {
        if (user == null) {
            log.error("Anonymous users cannot unsubscribe.");
            throw new IllegalArgumentException("User cannot be null.");
        } else if (user.getId() == null) {
            log.error("Cannot unsubscribe when user ID is null.");
            throw new IllegalArgumentException("User ID cannot be null.");
        } else if (report == null) {
            log.error("Cannot unsubscribe from report null.");
            throw new IllegalArgumentException("Report cannot be null.");
        } else if (report.getId() == null) {
            log.error("Cannot unsubscribe from report with ID null.");
            throw new IllegalArgumentException("Report ID cannot be null.");
        }

        try (Transaction tx = transactionManager.begin()) {
            tx.newSubscriptionGateway().unsubscribe(report, user);
            tx.commit();
        } catch (NotFoundException e) {
            log.error("User " + user + " or report " + report + " not found.");
            feedbackEvent.fire(new Feedback(messagesBundle.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (TransactionException e) {
            log.error("Error when user " + user + " is unsubscribing from report " + report + ".");
            feedbackEvent.fire(new Feedback(messagesBundle.getString("data_access_error"), Feedback.Type.ERROR));
        }
    }

    /**
     * Determines the subscription status of the user to the report.
     *
     * @param user   The user in question.
     * @param report The report in question.
     * @return {@code true} iff the user is subscribed to the report.
     */
    public boolean isSubscribed(final User user, final Report report) {
        if (user == null) {
            return false;
        } else if (user.getId() == null) {
            log.error("Cannot determine subscription status of user with ID null.");
            throw new IllegalArgumentException("User ID cannot be null.");
        } else if (report == null) {
            log.error("Cannot determine subscription status to report null.");
            throw new IllegalArgumentException("Report cannot be null.");
        } else if (report.getId() == null) {
            log.error("Cannot determine subscription status to report with ID null.");
            throw new IllegalArgumentException("Report ID cannot be null.");
        }

        boolean status;
        try (Transaction tx = transactionManager.begin()) {
            status = tx.newSubscriptionGateway().isSubscribed(user, report);
            tx.commit();
        } catch (NotFoundException e) {
            status = false;
            log.error("Could not find user " + user + " or report " + report + ".", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (TransactionException e) {
            status = false;
            log.error("Error when determining subscription status of user " + user + " to report " + report + ".", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("data_access_error"), Feedback.Type.ERROR));
        }
        return status;
    }

    /**
     * Closes an open report. User receive no notifications about closed reports.
     *
     * @param report The report to be closed.
     */
    public void close(final Report report) {
        report.setClosingDate(OffsetDateTime.now());
        try (Transaction tx = transactionManager.begin()) {
            tx.newReportGateway().update(report);
            tx.commit();
        } catch (NotFoundException e) {
            log.error("Could not find report " + report + ".", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (TransactionException e) {
            log.error("Error when closing report " + report + ".", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("data_access_error"), Feedback.Type.ERROR));
        }
    }

    /**
     * Opens a closed report.
     *
     * @param report The report to be opened.
     */
    public void open(final Report report) {
        report.setClosingDate(null);
        try (Transaction tx = transactionManager.begin()) {
            tx.newReportGateway().update(report);
            tx.commit();
        } catch (NotFoundException e) {
            log.error("Could not find report " + report + ".", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (TransactionException e) {
            log.error("Error when opening report " + report + ".", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("data_access_error"), Feedback.Type.ERROR));
        }
    }

    /**
     * Increases the relevance of the report by the user's current voting weight.
     *
     * @param report The report the relevance of which is to be increased.
     * @param user   The user voting on the report.
     */
    public void upvote(final Report report, final User user) {
        removeVote(report, user);
        int votingWeight = profileService.getVotingWeightForUser(user);
        if (votingWeight > 0) {
            try (Transaction tx = transactionManager.begin()) {
                tx.newReportGateway().addVote(report, user, votingWeight);
                tx.commit();
            } catch (NotFoundException e) {
                log.error("Could not find report " + report + ".", e);
                feedbackEvent.fire(new Feedback(messagesBundle.getString("not_found_error"), Feedback.Type.ERROR));
            } catch (TransactionException e) {
                log.error("Error when upvoting report " + report + ".", e);
                feedbackEvent.fire(new Feedback(messagesBundle.getString("data_access_error"), Feedback.Type.ERROR));
            } catch (DuplicateException e) {
                log.error("Error when upvoting report " + report + " because a previous vote was not removed.", e);
                feedbackEvent.fire(new Feedback(messagesBundle.getString("duplicate_vote"), Feedback.Type.ERROR));
            }
        } else {
            feedbackEvent.fire(new Feedback(messagesBundle.getString("voting_weight_zero"), Feedback.Type.INFO));
        }

    }

    /**
     * Decreases the relevance of the report by the user's current voting weight.
     *
     * @param report The report the relevance of which is to be decreased.
     * @param user   The user voting on the report.
     */
    public void downvote(final Report report, final User user) {
        removeVote(report, user);
        int votingWeight = profileService.getVotingWeightForUser(user);
        if (votingWeight > 0) {
            try (Transaction tx = transactionManager.begin()) {
                tx.newReportGateway().addVote(report, user, -votingWeight);
                tx.commit();
            } catch (NotFoundException e) {
                log.error("Could not find report " + report + ".", e);
                feedbackEvent.fire(new Feedback(messagesBundle.getString("not_found_error"), Feedback.Type.ERROR));
            } catch (TransactionException e) {
                log.error("Error when downvoting report " + report + ".", e);
                feedbackEvent.fire(new Feedback(messagesBundle.getString("data_access_error"), Feedback.Type.ERROR));
            } catch (DuplicateException e) {
                log.error("Error when upvoting report " + report + " because a previous vote was not removed.", e);
                feedbackEvent.fire(new Feedback(messagesBundle.getString("duplicate_vote"), Feedback.Type.ERROR));
            }
        } else {
            feedbackEvent.fire(new Feedback(messagesBundle.getString("voting_weight_zero"), Feedback.Type.INFO));
        }
    }

    /**
     * Removes the vote on the report of the user. Does nothing if the user has not voted on the report.
     *
     * @param report The report the vote of the user of which is to be removed.
     * @param user   The user whose vote is to be removed.
     */
    public void removeVote(final Report report, final User user) {
        try (Transaction tx = transactionManager.begin()) {
            tx.newReportGateway().removeVote(report, user);
            tx.commit();
        } catch (NotFoundException e) {
            log.error("Error while removing vote in report " + report + ".", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (TransactionException e) {
            log.error("Error while removing vote in report " + report + ".", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("data_access_error"), Feedback.Type.ERROR));
        }
    }

    /**
     * Checks if the user has voted to increase the relevance of the report.
     *
     * @param report The report in question.
     * @param user   The user in question.
     * @return {@code true} if they have voted to increase the relevance, {@code false} otherwise.
     */
    public boolean hasUpvoted(final Report report, final User user) {
        try (Transaction tx = transactionManager.begin()) {
            Integer vote = tx.newReportGateway().getVote(user, report);
            tx.commit();
            return vote != null && vote > 0;
        } catch (TransactionException e) {
            log.error("Error while searching for vote for report.", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("lookup_failure"), Feedback.Type.ERROR));
            return false;
        }
    }

    /**
     * Checks if the user has voted to decrease the relevance of the report.
     *
     * @param report The report in question.
     * @param user   The user in question.
     * @return {@code true} if they have voted to decrease the relevance, {@code false} otherwise.
     */
    public boolean hasDownvoted(final Report report, final User user) {
        try (Transaction tx = transactionManager.begin()) {
            Integer vote = tx.newReportGateway().getVote(user, report);
            tx.commit();
            return vote != null && vote < 0;
        } catch (TransactionException e) {
            log.error("Error while searching for vote for report.", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("lookup_failure"), Feedback.Type.ERROR));
            return false;
        }
    }

    /**
     * Returns the report with the specified ID, if it exists. If there is no such report, returns {@code null} and
     * fires an event.
     *
     * @param id The ID of the desired report.
     * @return The report with that ID if it exists, {@code null} if there is no report with that ID.
     */
    public Report getReportByID(final int id) {
        try (Transaction tx = transactionManager.begin()) {
            Report report = tx.newReportGateway().find(id);
            tx.commit();
            return report;
        } catch (NotFoundException e) {
            log.debug("Report not found.", e);
            return null;
        } catch (TransactionException e) {
            log.error("Error while searching for report.", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("lookup_failure"), Feedback.Type.ERROR));
            return null;
        }
    }

    /**
     * Creates a new report along with its first post and notifies users about the creation. Notifications are handled
     * by the {@link NotificationService}.
     *
     * @param report    The report to be created.
     * @param firstPost The first post of the report.
     * @return {@code true} iff creating the report succeeded.
     */
    public boolean createReport(final Report report, final Post firstPost) {
        boolean success = false;
        try (Transaction tx = transactionManager.begin()) {
            tx.newReportGateway().create(report);
            firstPost.setReport(report.getId());
            boolean postCreated = postService.createPostWithTransaction(firstPost, tx);
            if (postCreated) {
                tx.commit();
                success = true;
                log.info("Report created successfully.");
                feedbackEvent.fire(new Feedback(messagesBundle.getString("report_created"), Feedback.Type.INFO));
            } else {
                tx.abort();
            }
        } catch (TransactionException e) {
            log.error("Error while creating a new report", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("create_failure"), Feedback.Type.ERROR));
            return false;
        }
        if (success) {
            User creator = new User();
            creator.setId(report.getAuthorship().getCreator().getId());
            subscribeToReport(creator, report);
            Notification notification = new Notification();
            notification.setType(Notification.Type.NEW_REPORT);
            notification.setActuatorID(report.getAuthorship().getCreator().getId());
            notification.setTopicID(report.getTopicID());
            notification.setReportID(report.getId());
            notification.setReportTitle(report.getTitle());
            notificationService.createNotification(notification);
        }
        return success;
    }

    /**
     * Moves a given existing report to another topic and notifies users about the change. Notifications are handled by
     * the {@link NotificationService}.
     *
     * @param report The report to move.
     * @return {@code true} iff moving the report succeeded.
     */
    public boolean move(final Report report) {
        log.debug("Moving report " + report + ".");
        boolean success = false;

        try (Transaction tx = transactionManager.begin()) {
            tx.newReportGateway().update(report);
            tx.commit();
            success = true;
            feedbackEvent.fire(new Feedback(messagesBundle.getString("operation_successful"), Feedback.Type.INFO));
        } catch (NotFoundException e) {
            log.error("Report to be moved could not be found.", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (TransactionException e) {
            log.error("Error while moving a report.", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("update_failure"), Feedback.Type.ERROR));
        }

        if (success && report.getClosingDate() == null) {
            Notification notification = new Notification();
            notification.setType(Notification.Type.MOVED_REPORT);
            notification.setActuatorID(report.getAuthorship().getModifier().getId());
            notification.setTopicID(report.getTopicID());
            notification.setReportID(report.getId());
            notification.setReportTitle(report.getTitle());
            notificationService.createNotification(notification);
        }
        return success;
    }

    /**
     * Updates an existing report and notifies users about the change. Notifications are handled by the {@link
     * NotificationService}.
     *
     * @param report The report to update.
     * @return {@code true} iff updating the report succeeded.
     */
    public boolean updateReport(final Report report) {
        try (Transaction tx = transactionManager.begin()) {
            report.getAuthorship().setModifiedDate(OffsetDateTime.now());
            tx.newReportGateway().update(report);
            tx.commit();
        } catch (NotFoundException e) {
            log.error("Report to be updated could not be found.", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("not_found_error"), Feedback.Type.ERROR));
            return false;
        } catch (TransactionException e) {
            log.error("Error while updating a report.", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("update_failure"), Feedback.Type.ERROR));
            return false;
        }
        if (report.getClosingDate() == null) {
            Notification notification = new Notification();
            notification.setType(Notification.Type.EDITED_REPORT);
            notification.setActuatorID(report.getAuthorship().getModifier().getId());
            notification.setTopicID(report.getTopicID());
            notification.setReportID(report.getId());
            notification.setReportTitle(report.getTitle());
            notificationService.createNotification(notification);
        }
        return true;
    }

    /**
     * Irreversibly deletes the report and all its posts.
     *
     * @param report The report to be deleted.
     * @return {@code true} iff deleting the report was successful.
     */
    public boolean deleteReport(final Report report) {
        try (Transaction tx = transactionManager.begin()) {
            tx.newReportGateway().delete(report);
            tx.commit();
            feedbackEvent.fire(new Feedback(messagesBundle.getString("report_deleted"), Feedback.Type.INFO));
            return true;
        } catch (NotFoundException e) {
            log.error("Could not find report " + report + ".", e);
        } catch (TransactionException e) {
            log.error("Error when deleting report " + report + ".", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("data_access_error"), Feedback.Type.ERROR));
        }
        return false;
    }

    /**
     * Marks the report as a duplicate of another report.
     *
     * @param duplicate     The report which is a duplicate.
     * @param duplicateOfID The ID of the report the other report is a duplicate of.
     * @return {@code true} iff updating the report succeeded.
     */
    public boolean markDuplicate(final Report duplicate, final int duplicateOfID) {
        if (duplicate.getId() == duplicateOfID) {
            log.error("Cannot mark report " + duplicate + " as original report of itself.");
            feedbackEvent.fire(new Feedback(messagesBundle.getString("self_reference_error"), Feedback.Type.ERROR));
            return false;
        }

        int originalID = duplicateOfID;
        Report original = getReportByID(originalID);
        if (original == null) {
            log.error("Could not find report " + duplicate + ".");
            feedbackEvent.fire(new Feedback(messagesBundle.getString("not_found_error"), Feedback.Type.ERROR));
            return false;
        } else if (Objects.equals(original.getDuplicateOf(), duplicate.getId())) {
            log.error("Cannot mark report " + duplicate + " as original report of itself.");
            feedbackEvent.fire(new Feedback(messagesBundle.getString("self_reference_error"), Feedback.Type.ERROR));
            return false;
        } else if (original.getDuplicateOf() != null) {
            originalID = original.getDuplicateOf();
        }

        boolean valid = false;

        try (Transaction tx = transactionManager.begin()) {
            tx.newReportGateway().markDuplicate(duplicate, originalID);
            tx.commit();
            valid = true;
        } catch (SelfReferenceException e) {
            log.error("Cannot mark report " + duplicate + " as original report of itself.", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("self_reference_error"), Feedback.Type.ERROR));
        } catch (NotFoundException e) {
            log.error("Could not find report " + duplicate + '.', e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (TransactionException e) {
            log.error("Error when marking report " + duplicate + " as duplicate.", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("data_access_error"), Feedback.Type.ERROR));
        }

        return valid;
    }

    /**
     * Unmarks the report as a duplicate of another report.
     *
     * @param report The report to be unmarked.
     * @return {@code true} iff updating the report succeeded.
     */
    public boolean unmarkDuplicate(final Report report) {
        boolean valid = false;

        try (Transaction tx = transactionManager.begin()) {
            tx.newReportGateway().unmarkDuplicate(report);
            tx.commit();
            valid = true;
        } catch (NotFoundException e) {
            log.error("Could not find report " + report + '.', e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (TransactionException e) {
            log.error("Error when marking report " + report + " as duplicate.", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("data_access_error"), Feedback.Type.ERROR));
        }

        return valid;
    }

    /**
     * Overwrites the relevance of the report with a set value. Users may still vote on the report, but this will not
     * affect the displayed relevance until the overwriting is undone.
     *
     * @param report    The report the relevance of which is to be overwritten.
     * @param relevance The new value of the relevance.
     */
    public void overwriteRelevance(final Report report, final Integer relevance) {
        try (Transaction tx = transactionManager.begin()) {
            tx.newReportGateway().overwriteRelevance(report, relevance);
            tx.commit();
        } catch (NotFoundException e) {
            log.error("Error while overwriting relevance in report " + report + ".", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (TransactionException e) {
            log.error("Error while overwriting relevance in report " + report + ".", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("data_access_error"), Feedback.Type.ERROR));
        }
    }

    /**
     * Returns the number of posts of a report.
     *
     * @param report The report in question.
     * @return The number of posts as an {@code int}.
     */
    public int getNumberOfPosts(final Report report) {
        int numberOfPosts = 0;
        try (Transaction tx = transactionManager.begin()) {
            numberOfPosts = tx.newReportGateway().countPosts(report);
            tx.commit();
        } catch (NotFoundException e) {
            log.error("Could not find report " + report + ".", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (TransactionException e) {
            log.error("Error when counting posts of report " + report + ".", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("data_access_error"), Feedback.Type.ERROR));
        }
        return numberOfPosts;
    }

    /**
     * Returns selected posts for one report.
     *
     * @param report    The report the posts of which are desired.
     * @param selection Information on which posts to get.
     * @return A list containing the selected posts.
     */
    public List<Post> getPostsFor(final Report report, final Selection selection) {
        List<Post> posts = null;
        try (Transaction tx = transactionManager.begin()) {
            posts = tx.newPostGateway().selectPostsOfReport(report, selection);
            tx.commit();
        } catch (TransactionException e) {
            log.error("Error when finding posts for report " + report + " with selection " + selection + ".", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (NotFoundException e) {
            log.error("Could not find posts for report " + report + " with selection " + selection + ".", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("not_found_error"), Feedback.Type.ERROR));
        }
        return posts;
    }

    /**
     * Returns all duplicates of a given report.
     *
     * @param report    The report the duplicates of which are desired.
     * @param selection Information on which duplicates to get.
     * @return A list containing the selected duplicates.
     */
    public List<Report> getDuplicatesFor(final Report report, final Selection selection) {
        List<Report> reports = Collections.emptyList();

        try (Transaction tx = transactionManager.begin()) {
            reports = tx.newReportGateway().selectDuplicates(report, selection);
            tx.commit();
        } catch (TransactionException e) {
            log.error("Error when finding duplicates for report " + report + " with selection " + selection + ".", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("data_access_error"), Feedback.Type.ERROR));
        }

        return reports;
    }

    /**
     * Returns the number of duplicates of a given report.
     *
     * @param report The report the number of duplicates of which are desired.
     * @return The number of duplicates of the given {@code report}.
     */
    public int getNumberOfDuplicates(final Report report) {
        int duplicates = 0;

        try (Transaction tx = transactionManager.begin()) {
            duplicates = tx.newReportGateway().countDuplicates(report);
            tx.commit();
        } catch (NotFoundException e) {
            log.error("Could not find report " + report + ".", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (TransactionException e) {
            log.error("Error when counting duplicates of report " + report + ".", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("data_access_error"), Feedback.Type.ERROR));
        }

        return duplicates;
    }

    /**
     * Returns whether the user is allowed to post in a given report.
     *
     * @param user   The user in question.
     * @param report The report in question.
     * @return {@code true} iff the user is allowed to post in the report.
     */
    public boolean canPostInReport(final User user, final Report report) {
        if (user == null || report == null) {
            return false;
        }

        if (user.isAdministrator()) {
            return true;
        } else {
            Topic topic = topicService.getTopicByID(report.getTopicID());
            return topic != null && !topicService.isBanned(user, topic);
        }
    }

    /**
     * Find ID of the report containing the post with the specified ID.
     *
     * @param postID The post ID.
     * @return The report ID.
     */
    public int findReportOfPost(final int postID) {
        int reportID = 0;
        try (Transaction tx = transactionManager.begin()) {
            reportID = tx.newReportGateway().findReportOfPost(postID);
            tx.commit();
        } catch (NotFoundException e) {
            log.error("Could not find report containing post with ID " + postID + ".", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (TransactionException e) {
            log.error("Error when finding report containing post with ID " + postID + ".", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("data_access_error"), Feedback.Type.ERROR));
        }
        return reportID;
    }

}
