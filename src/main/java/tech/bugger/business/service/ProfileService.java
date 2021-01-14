package tech.bugger.business.service;

import tech.bugger.business.exception.CorruptImageException;
import tech.bugger.business.internal.ApplicationSettings;
import tech.bugger.business.util.Feedback;
import tech.bugger.business.util.Hasher;
import tech.bugger.business.util.Images;
import tech.bugger.business.util.RegistryKey;
import tech.bugger.global.transfer.Report;
import tech.bugger.global.transfer.Topic;
import tech.bugger.global.transfer.User;
import tech.bugger.global.util.Lazy;
import tech.bugger.global.util.Log;
import tech.bugger.business.exception.NotFoundException;
import tech.bugger.persistence.exception.DuplicateException;
import tech.bugger.persistence.exception.SelfReferenceException;
import tech.bugger.persistence.exception.TransactionException;
import tech.bugger.persistence.util.Transaction;
import tech.bugger.persistence.util.TransactionManager;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.Arrays;
import java.util.ResourceBundle;

/**
 * Service providing methods related to users and user profiles. A {@code Feedback} event is fired, if unexpected
 * circumstances occur.
 */
@Dependent
public class ProfileService {

    /**
     * The {@link Log} instance associated with this class for logging purposes.
     */
    private static final Log log = Log.forClass(ProfileService.class);

    /**
     * Feedback event for user feedback.
     */
    private final Event<Feedback> feedback;

    /**
     * The transaction manager used for creating transactions.
     */
    private final TransactionManager transactionManager;

    /**
     * The application settings holding the current settings information.
     */
    private final ApplicationSettings applicationSettings;

    /**
     * The resource bundle for feedback messages.
     */
    private final ResourceBundle messages;

    /**
     * Constructs a new profile service with the given dependencies.
     *
     * @param feedback The feedback event to be used for user feedback.
     * @param transactionManager The transaction manager to be used for creating transactions.
     * @param applicationSettings The current application settings.
     * @param messages The resource bundle to look up feedback messages.
     */
    @Inject
    public ProfileService(final Event<Feedback> feedback, final TransactionManager transactionManager,
                          final ApplicationSettings applicationSettings,
                          final @RegistryKey("messages") ResourceBundle messages) {
        this.feedback = feedback;
        this.transactionManager = transactionManager;
        this.applicationSettings = applicationSettings;
        this.messages = messages;
    }

    /**
     * Returns the user with the specified ID. If no such user exists, returns {@code null} and fires an event.
     *
     * @param id The ID of the user to return.
     * @return The user, if they exist, {@code null} if no user with that ID exists.
     */
    public User getUser(final int id) {
        User user = null;
        try (Transaction transaction = transactionManager.begin()) {
            user = transaction.newUserGateway().getUserByID(id);
            transaction.commit();
        } catch (tech.bugger.persistence.exception.NotFoundException e) {
            log.error("The user with id " + id + " could not be found.", e);
            throw new NotFoundException(messages.getString("not_found_error"), e);
        } catch (TransactionException e) {
            log.error("Error while loading the user with id " + id, e);
            feedback.fire(new Feedback(messages.getString("data_access_error"), Feedback.Type.ERROR));
        }
        return user;
    }

    /**
     * Creates a new user without need for verification. This should only be available for administrators.
     * Also generates and sets the internal user id inside the given {@code user} object. This also shows a message if
     * the action was not successful.
     *
     * @param user The user to be created.
     * @return Whether the action was successful or not.
     */
    public boolean createUser(final User user) {
        try (Transaction tx = transactionManager.begin()) {
            tx.newUserGateway().createUser(user);
            tx.commit();
            return true;
        } catch (TransactionException e) {
            log.error("User could not be created.", e);
            feedback.fire(new Feedback(messages.getString("data_access_error"), Feedback.Type.ERROR));
        }

        return false;
    }

    /**
     * Irreversibly deletes a user. This does not delete their created reports and posts.
     *
     * @param user The user to be deleted.
     * @return Whether the deletion was successful or not.
     */
    public boolean deleteUser(final User user) {
        if (isLastAdmin(user)) {
            return false;
        }
        try (Transaction tx = transactionManager.begin()) {
            tx.newUserGateway().deleteUser(user);
            tx.commit();
        } catch (tech.bugger.persistence.exception.NotFoundException e) {
            log.error("The user with id " + user.getId() + " could not be found.", e);
        } catch (TransactionException e) {
            log.error("The user with id " + user.getId() + " could not be deleted.", e);
            feedback.fire(new Feedback(messages.getString("data_access_error"), Feedback.Type.ERROR));
            return false;
        }
        return true;
    }

    /**
     * Updates an existing user and returns whether the action was successful.
     *
     * @param user The user to update.
     * @return {@code true} iff the action was successful, {@code false} otherwise.
     */
    public boolean updateUser(final User user) {
        try (Transaction tx = transactionManager.begin()) {
            tx.newUserGateway().updateUser(user);
            tx.commit();
            feedback.fire(new Feedback(messages.getString("operation_successful"), Feedback.Type.INFO));
            return true;
        } catch (tech.bugger.persistence.exception.NotFoundException e) {
            log.error("The user with id " + user.getId() + "could not be found.", e);
            throw new NotFoundException(messages.getString("not_found_error"), e);
        } catch (TransactionException e) {
            log.error("Error while updating the user with id " + user.getId(), e);
            feedback.fire(new Feedback(messages.getString("data_access_error"), Feedback.Type.ERROR));
            return false;
        }
    }

    /**
     * Removes the subscription to a certain topic for one user.
     *
     * @param subscriber The user subscribed to the topic.
     * @param topic      The topic of which the subscription to is to be removed.
     */
    public void deleteTopicSubscription(final User subscriber, final Topic topic) {
        if (subscriber == null) {
            log.error("Anonymous users cannot unsubscribe from anything.");
            throw new IllegalArgumentException("User cannot be null.");
        } else if (subscriber.getId() == null) {
            log.error("Cannot unsubscribe when user ID is null.");
            throw new IllegalArgumentException("User ID cannot be null.");
        } else if (topic == null) {
            log.error("Cannot unsubscribe from topic null.");
            throw new IllegalArgumentException("Topic cannot be null.");
        } else if (topic.getId() == null) {
            log.error("Cannot unsubscribe from topic with ID null.");
            throw new IllegalArgumentException("Topic ID cannot be null.");
        }

        try (Transaction tx = transactionManager.begin()) {
            tx.newSubscriptionGateway().unsubscribe(topic, subscriber);
            tx.commit();
        } catch (tech.bugger.persistence.exception.NotFoundException e) {
            log.error("User " + subscriber + " or topic " + topic + " not found.");
            feedback.fire(new Feedback(messages.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (TransactionException e) {
            log.error("Error when user " + subscriber + " is unsubscribing from topic " + topic + ".");
            feedback.fire(new Feedback(messages.getString("not_found_error"), Feedback.Type.ERROR));
        }
    }

    /**
     * Removes the subscription to a certain report for one user.
     *
     * @param subscriber The user subscribed to the report.
     * @param report     The report of which the subscription to is to be removed.
     */
    public void deleteReportSubscription(final User subscriber, final Report report) {
        if (subscriber == null) {
            log.error("Anonymous users cannot unsubscribe.");
            throw new IllegalArgumentException("User cannot be null.");
        } else if (subscriber.getId() == null) {
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
            tx.newSubscriptionGateway().unsubscribe(report, subscriber);
            tx.commit();
        } catch (tech.bugger.persistence.exception.NotFoundException e) {
            log.error("User " + subscriber + " or report " + report + " not found.");
            feedback.fire(new Feedback(messages.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (TransactionException e) {
            log.error("Error when user " + subscriber + " is unsubscribing from report " + report + ".");
            feedback.fire(new Feedback(messages.getString("data_access_error"), Feedback.Type.ERROR));
        }
    }

    /**
     * Removes the subscription to a certain other user for one user.
     *
     * @param subscriber The user subscribed to the other user.
     * @param user       The user of which the subscription to is to be removed.
     */
    public void deleteUserSubscription(final User subscriber, final User user) {
        if (subscriber == null) {
            log.error("Anonymous users cannot unsubscribe.");
            throw new IllegalArgumentException("User cannot be null.");
        } else if (subscriber.getId() == null) {
            log.error("Cannot unsubscribe when user ID is null.");
            throw new IllegalArgumentException("User ID cannot be null.");
        } else if (user == null) {
            log.error("Cannot unsubscribe from user null.");
            throw new IllegalArgumentException("User cannot be null.");
        } else if (user.getId() == null) {
            log.error("Cannot unsubscribe from user with ID null.");
            throw new IllegalArgumentException("User ID cannot be null.");
        }

        try (Transaction tx = transactionManager.begin()) {
            tx.newSubscriptionGateway().unsubscribe(user, subscriber);
            tx.commit();
        } catch (tech.bugger.persistence.exception.NotFoundException e) {
            log.error("User " + subscriber + " or user " + user + " not found.");
            feedback.fire(new Feedback(messages.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (TransactionException e) {
            log.error("Error when user " + subscriber + " is unsubscribing from user " + user + ".");
            feedback.fire(new Feedback(messages.getString("data_access_error"), Feedback.Type.ERROR));
        }
    }

    /**
     * Removes all subscriptions to topics for one user.
     *
     * @param user The user whose topic subscriptions are to be deleted.
     */
    public void deleteAllTopicSubscriptions(final User user) {
        if (user == null) {
            log.error("Cannot delete subscriptions for user null.");
            throw new IllegalArgumentException("User cannot be null.");
        } else if (user.getId() == null) {
            log.error("Cannot delete subscriptions for user with ID null.");
            throw new IllegalArgumentException("User ID cannot be null.");
        }

        try (Transaction tx = transactionManager.begin()) {
            tx.newSubscriptionGateway().unsubscribeAllTopics(user);
            tx.commit();
        } catch (tech.bugger.persistence.exception.NotFoundException e) {
            log.error("Could not find user " + user + ".", e);
            feedback.fire(new Feedback(messages.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (TransactionException e) {
            log.error("Error when removing all topic subscriptions for user " + user + ".", e);
            feedback.fire(new Feedback(messages.getString("data_access_error"), Feedback.Type.ERROR));
        }
    }

    /**
     * Removes all subscriptions to reports for one user.
     *
     * @param user The user whose report subscriptions are to be deleted.
     */
    public void deleteAllReportSubscriptions(final User user) {
        if (user == null) {
            log.error("Cannot delete subscriptions for user null.");
            throw new IllegalArgumentException("User cannot be null.");
        } else if (user.getId() == null) {
            log.error("Cannot delete subscriptions for user with ID null.");
            throw new IllegalArgumentException("User ID cannot be null.");
        }

        try (Transaction tx = transactionManager.begin()) {
            tx.newSubscriptionGateway().unsubscribeAllReports(user);
            tx.commit();
        } catch (tech.bugger.persistence.exception.NotFoundException e) {
            log.error("Could not find user " + user + ".", e);
            feedback.fire(new Feedback(messages.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (TransactionException e) {
            log.error("Error when removing all report subscriptions for user " + user + ".", e);
            feedback.fire(new Feedback(messages.getString("data_access_error"), Feedback.Type.ERROR));
        }
    }

    /**
     * Removes all subscriptions to other users for one user.
     *
     * @param user The user whose user subscriptions are to be deleted.
     */
    public void deleteAllUserSubscriptions(final User user) {
        if (user == null) {
            log.error("Cannot delete subscriptions for user null.");
            throw new IllegalArgumentException("User cannot be null.");
        } else if (user.getId() == null) {
            log.error("Cannot delete subscriptions for user with ID null.");
            throw new IllegalArgumentException("User ID cannot be null.");
        }

        try (Transaction tx = transactionManager.begin()) {
            tx.newSubscriptionGateway().unsubscribeAllUsers(user);
            tx.commit();
        } catch (tech.bugger.persistence.exception.NotFoundException e) {
            log.error("Could not find user " + user + ".", e);
            feedback.fire(new Feedback(messages.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (TransactionException e) {
            log.error("Error when removing all user subscriptions for user " + user + ".", e);
            feedback.fire(new Feedback(messages.getString("data_access_error"), Feedback.Type.ERROR));
        }
    }

    /**
     * Subscribes one user to another user.
     *
     * @param subscriber   The user who will subscribe to the other user.
     * @param subscribedTo The user who will receive a subscription.
     */
    public void subscribeToUser(final User subscriber, final User subscribedTo) {
        if (subscriber == null) {
            log.error("Anonymous users cannot subscribe to anything.");
            throw new IllegalArgumentException("Subscriber cannot be null.");
        } else if (subscriber.getId() == null) {
            log.error("Cannot subscribe when user ID is null.");
            throw new IllegalArgumentException("Subscriber ID cannot be null.");
        } else if (subscribedTo == null) {
            log.error("Cannot subscribe to user null.");
            throw new IllegalArgumentException("User cannot be null.");
        } else if (subscribedTo.getId() == null) {
            log.error("Cannot subscribe to user with ID null.");
            throw new IllegalArgumentException("User ID cannot be null.");
        }

        try (Transaction tx = transactionManager.begin()) {
            tx.newSubscriptionGateway().subscribe(subscribedTo, subscriber);
            tx.commit();
        } catch (DuplicateException e) {
            log.error("User " + subscriber + " is already subscribed to user " + subscribedTo + ".");
            feedback.fire(new Feedback(messages.getString("already_subscribed"), Feedback.Type.ERROR));
        } catch (tech.bugger.persistence.exception.NotFoundException e) {
            log.error("User " + subscriber + " or user " + subscribedTo + " not found.");
            feedback.fire(new Feedback(messages.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (TransactionException e) {
            log.error("Error when user " + subscriber + " is subscribing to user " + subscribedTo + ".");
            feedback.fire(new Feedback(messages.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (SelfReferenceException e) {
            log.error("User cannot self-subscribe.");
            feedback.fire(new Feedback(messages.getString("no_self_subscribe"), Feedback.Type.ERROR));
        }
    }

    /**
     * Updates the avatar of a user and generates a new thumbnail.
     *
     * @param user The user with a new avatar.
     */
    public void updateAvatar(final User user) {
    }

    /**
     * Returns the voting weight of a particular user.
     *
     * @param user The user in question.
     * @return The voting weight as an {@code int}.
     */
    public int getVotingWeightForUser(final User user) {
        int votingWeight = 0;
        if (user.getForcedVotingWeight() != null) {
            return user.getForcedVotingWeight();
        }
        int numPosts = getNumberOfPostsForUser(user);
        if (numPosts == 0) {
            log.error("No voting weight could be calculated for the user with id " + user.getId()
                    + " as the number of posts could not be calculated");
            return votingWeight;
        } else {
            String[] votingDef = applicationSettings.getConfiguration().getVotingWeightDefinition().split(",");
            if (votingDef.length == 0) {
                log.error("The voting weight definition is empty");
                feedback.fire(new Feedback(messages.getString("voting_weight_failure"), Feedback.Type.ERROR));
                return votingWeight;
            }
            try {
                int[] votingWeightDef = Arrays.stream(votingDef).mapToInt(Integer::parseInt).sorted().toArray();
                votingWeight = calculateVotingWeight(numPosts, votingWeightDef);
            } catch (NumberFormatException e) {
                log.error("The voting weight definition could not be parsed to a number");
                feedback.fire(new Feedback(messages.getString("voting_weight_failure"), Feedback.Type.ERROR));
            }
        }
        return votingWeight;
    }

    /**
     * Calculates the voting weight from the given number of posts and the voting weight definition.
     *
     * @param numPosts The number of posts.
     * @param votingWeightDef The voting weight definition.
     * @return The calculated voting weight.
     */
    private int calculateVotingWeight(final int numPosts, final int[] votingWeightDef) {
        if (votingWeightDef[0] != 0) {
            log.error("The voting weight definition needs to contain a 0.");
            feedback.fire(new Feedback(messages.getString("voting_weight_failure"), Feedback.Type.ERROR));
            return 0;
        }
        for (int i = 1; i < votingWeightDef.length; i++) {
            if (numPosts < votingWeightDef[i]) {
                return i;
            }
        }
        return votingWeightDef.length;
    }

    /**
     * Returns the total number of posts created by a particular user.
     *
     * @param user The user in question.
     * @return The number of posts as an {@code int}.
     */
    public int getNumberOfPostsForUser(final User user) {
        int numPosts = 0;
        try (Transaction transaction = transactionManager.begin()) {
            numPosts = transaction.newUserGateway().getNumberOfPosts(user);
            transaction.commit();
        } catch (tech.bugger.persistence.exception.NotFoundException e) {
            log.error("The number of posts could not be calculated for the user with id " + user.getId());
            feedback.fire(new Feedback(messages.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (TransactionException e) {
            log.error("Error while loading the number of posts for the user with id " + user.getId(), e);
            feedback.fire(new Feedback(messages.getString("data_access_error"), Feedback.Type.ERROR));
        }
        return numPosts;
    }

    /**
     * Promotes the user whose profile is being viewed to an administrator or demotes the user whose profile is being
     * viewed if they are an administrator. However, if they are the last remaining administrator, a feedback event is
     * fired instead.
     *
     * @param user The user to be promoted/demoted.
     */
    public void toggleAdmin(final User user) {
        if (user.isAdministrator() && !isLastAdmin(user)) {
            changeAdminStatus(user, false);
        } else if (!user.isAdministrator()) {
            changeAdminStatus(user, true);
        }
    }

    /**
     * Checks if the given user is the last administrator.
     *
     * @param user The user to be checked.
     * @return {@code true} iff the user is the last administrator.
     */
    private boolean isLastAdmin(final User user) {
        if (!user.isAdministrator()) {
            return false;
        }
        int admins = 0;
        try (Transaction transaction = transactionManager.begin()) {
            admins = transaction.newUserGateway().getNumberOfAdmins();
            transaction.commit();
            if (admins == 0) {
                log.error("No administrators could be found in the database");
                throw new InternalError("No administrators could be found in the database");
            } else if (admins == 1) {
                log.error("The last administrator cannot be deleted");
                feedback.fire(new Feedback(messages.getString("delete_last_admin"), Feedback.Type.ERROR));
            }
        } catch (TransactionException e) {
            log.error("Error while counting the number of administrators.", e);
            feedback.fire(new Feedback(messages.getString("data_access_error"), Feedback.Type.ERROR));
        }
        return admins <= 1;
    }

    /**
     * Changes the given user's administrator status based on the given boolean.
     *
     * @param user The user to be promoted.
     * @param admin The administration status to change to.
     */
    private void changeAdminStatus(final User user, final boolean admin) {
        try (Transaction transaction = transactionManager.begin()) {
            user.setAdministrator(admin);
            transaction.newUserGateway().updateUser(user);
            transaction.commit();
            feedback.fire(new Feedback(messages.getString("operation_successful"), Feedback.Type.INFO));
        } catch (tech.bugger.persistence.exception.NotFoundException e) {
            user.setAdministrator(!admin);
            log.error("The user with id " + user.getId() + "could not be found.", e);
            feedback.fire(new Feedback(messages.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (TransactionException e) {
            user.setAdministrator(!admin);
            log.error("Error while updating the user with id " + user.getId(), e);
            feedback.fire(new Feedback(messages.getString("data_access_error"), Feedback.Type.ERROR));
        }
    }

    /**
     * Checks whether the input password is the same as the given user's password.
     *
     * @param user The user whose password is to be checked.
     * @param password The password given as input.
     * @return {@code true} iff the input matched the user's hashed password.
     */
    public boolean matchingPassword(final User user, final String password) {
        if (user.getPasswordHash().equals(Hasher.hash(password, user.getPasswordSalt(), user.getHashingAlgorithm()))) {
            return true;
        } else {
            feedback.fire(new Feedback(messages.getString("wrong_password"), Feedback.Type.ERROR));
            return false;
        }
    }

    /**
     * Searches and returns the {@link User} with the given {@code emailAddress}.
     *
     * @param emailAddress The e-mail address to search for.
     * @return The complete {@link User} or {@code null} iff the {@code emailAddress} is not assigned to any user.
     */
    public User getUserByEmail(final String emailAddress) {
        User user = null;

        try (Transaction tx = transactionManager.begin()) {
            user = tx.newUserGateway().getUserByEmail(emailAddress);
            tx.commit();
        } catch (tech.bugger.persistence.exception.NotFoundException e) {
            log.debug("User in search for e-mail could not be found.");
        } catch (TransactionException e) {
            log.error("Error while searching for e-mail.", e);
            feedback.fire(new Feedback(messages.getString("data_access_error"), Feedback.Type.ERROR));
        }

        return user;
    }

    /**
     * Searches and returns the {@link User} with the given {@code username}.
     *
     * @param username The username to search for.
     * @return The complete {@link User} or {@code null} iff the {@code username} is not assigned to any user.
     */
    public User getUserByUsername(final String username) {
        User user = null;

        try (Transaction tx = transactionManager.begin()) {
            user = tx.newUserGateway().getUserByUsername(username);
            tx.commit();
        } catch (tech.bugger.persistence.exception.NotFoundException e) {
            log.debug("User in search for username could not be found.");
        } catch (TransactionException e) {
            log.error("Error while searching for username.", e);
            feedback.fire(new Feedback(messages.getString("data_access_error"), Feedback.Type.ERROR));
        }

        return user;
    }

    /**
     * Converts the given {@code avatar} into an {@link Lazy} byte array.
     *
     * @param avatar The input {@link Part} to be converted.
     * @return The new {@link Lazy} byte array or {@code null} iff the input could not be converted.
     */
    public Lazy<byte[]> uploadAvatar(final Part avatar) {
        try {
            return new Lazy<>(avatar.getInputStream().readAllBytes());
        } catch (IOException e) {
            log.error("Error while uploading an avatar.", e);
            feedback.fire(new Feedback(messages.getString("upload_avatar"), Feedback.Type.ERROR));
        }
        return null;
    }

    /**
     * Converts the given image into thumbnail.
     *
     * @param image The image.
     * @return The generated thumbnail or {@code null} iff the given image was corrupt.
     */
    public byte[] generateThumbnail(final byte[] image) {
        try {
            return Images.generateThumbnail(image);
        } catch (CorruptImageException e) {
            log.error("Error while trying to generate a thumbnail.", e);
            feedback.fire(new Feedback(messages.getString("generate_thumbnail"), Feedback.Type.ERROR));
        }
        return null;
    }

}
