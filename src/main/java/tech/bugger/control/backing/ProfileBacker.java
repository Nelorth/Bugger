package tech.bugger.control.backing;

import tech.bugger.business.internal.UserSession;
import tech.bugger.business.service.ProfileService;
import tech.bugger.business.service.TopicService;
import tech.bugger.business.util.MarkdownHandler;
import tech.bugger.business.util.Paginator;
import tech.bugger.control.exception.Error404Exception;
import tech.bugger.global.transfer.Report;
import tech.bugger.global.transfer.Selection;
import tech.bugger.global.transfer.Topic;
import tech.bugger.global.transfer.User;
import tech.bugger.global.util.Log;

import javax.annotation.PostConstruct;
import javax.faces.context.ExternalContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serial;
import java.io.Serializable;

/**
 * Backing bean for the profile page.
 */
@ViewScoped
@Named
public class ProfileBacker implements Serializable {

    /**
     * The {@link Log} instance associated with this class for logging purposes.
     */
    private static final Log log = Log.forClass(ProfileBacker.class);

    @Serial
    private static final long serialVersionUID = -4606230292807293380L;

    /**
     * The type of popup dialog to be rendered on the profile page.
     */
    public enum ProfileDialog {
        /**
         * No dialogs are to be rendered.
         */
        NONE,

        /**
         * The dialog to change the profile owner's administrator status is to be rendered.
         */
        ADMIN,

        /**
         * The dialog to delete all topic subscriptions is to be rendered.
         */
        TOPIC,

        /**
         * The dialog to delete all report subscriptions is to be rendered.
         */
        REPORT,

        /**
         * The dialog to delete all user subscriptions is to be rendered.
         */
        USER
    }

    /**
     * The username of the profile owner.
     */
    private String username;

    /**
     * The profile owner's user information.
     */
    private User user;

    /**
     * The password entered to confirm changes.
     */
    private String password;

    /**
     * The profile owner's sanitized biography.
     */
    private String sanitizedBiography;

    /**
     * The profile owner's topic subscriptions.
     */
    private Paginator<Topic> topicSubscriptions;

    /**
     * The profile owner's report subscriptions.
     */
    private Paginator<Report> reportSubscriptions;

    /**
     * The profile owner's user subscriptions.
     */
    private Paginator<User> userSubscriptions;

    /**
     * The profile owner's moderated topics.
     */
    private Paginator<Topic> moderatedTopics;

    /**
     * The type of popup dialog to be rendered.
     */
    private ProfileDialog displayDialog;

    /**
     * The voting weight of the currently shown user.
     */
    private int votingWeight;

    /**
     * The number of posts of the currently shown user.
     */
    private int numberOfPosts;

    /**
     * The current subscription status.
     */
    private boolean subscribed;

    /**
     * Whether the user is privileged.
     */
    private boolean privileged;

    /**
     * The current user session.
     */
    private final UserSession session;

    /**
     * The current external context.
     */
    private final ExternalContext ectx;

    /**
     * The profile service providing the business logic for user functionality.
     */
    private final transient ProfileService profileService;

    /**
     * The topic service providing the business logic for topic functionality.
     */
    private final transient TopicService topicService;

    /**
     * Constructs a new profile page backing bean with the necessary dependencies.
     *
     * @param topicService   The topic service to use.
     * @param profileService The profile service to use.
     * @param session        The current {@link UserSession}.
     * @param ectx           The current external context.
     */
    @Inject
    public ProfileBacker(final TopicService topicService,
                         final ProfileService profileService,
                         final UserSession session,
                         final ExternalContext ectx) {
        this.topicService = topicService;
        this.profileService = profileService;
        this.session = session;
        this.ectx = ectx;
    }

    /**
     * Initializes the profile page. Checks whether this is the user's own profile page.
     */
    @PostConstruct
    void init() {
        // The initialization of the subscriptions will be implemented in the subscriptions feature.
        if (!ectx.getRequestParameterMap().containsKey("u")) {
            if (session.getUser() != null) {
                username = session.getUser().getUsername();
            } else {
                throw new Error404Exception();
            }
        }

        if (username == null) {
            username = ectx.getRequestParameterMap().get("u");
        }
        user = profileService.getUserByUsername(username);

        if (user == null) {
            throw new Error404Exception();
        }

        User sessionUser = session.getUser();
        votingWeight = profileService.getVotingWeightForUser(user);
        numberOfPosts = profileService.getNumberOfPostsForUser(user);
        subscribed = profileService.isSubscribed(sessionUser, user);
        privileged = sessionUser != null && (sessionUser.isAdministrator() || sessionUser.equals(user));

        if (user.getBiography() != null) {
            sanitizedBiography = MarkdownHandler.toHtml(user.getBiography());
        }

        if (sessionUser != null && sessionUser.equals(user)) {
            session.setUser(new User(user));
        }
        displayDialog = ProfileDialog.NONE;

        moderatedTopics = new Paginator<>("id", Selection.PageSize.SMALL) {
            @Override
            protected Iterable<Topic> fetch() {
                return topicService.getModeratedTopics(user, getSelection());
            }

            @Override
            protected int totalSize() {
                return profileService.getNumberOfModeratedTopics(user);
            }
        };

        if (isPrivileged()) {
            topicSubscriptions = new Paginator<>("id", Selection.PageSize.SMALL) {
                @Override
                protected Iterable<Topic> fetch() {
                    return topicService.selectSubscribedTopics(user, getSelection());
                }

                @Override
                protected int totalSize() {
                    return topicService.countSubscribedTopics(user);
                }
            };

            reportSubscriptions = new Paginator<>("id", Selection.PageSize.SMALL) {
                @Override
                protected Iterable<Report> fetch() {
                    return profileService.selectSubscribedReports(user, getSelection());
                }

                @Override
                protected int totalSize() {
                    return profileService.countSubscribedReports(user);
                }
            };

            userSubscriptions = new Paginator<>("username", Selection.PageSize.SMALL) {
                @Override
                protected Iterable<User> fetch() {
                    return profileService.selectSubscribedUsers(user, getSelection());
                }

                @Override
                protected int totalSize() {
                    return profileService.countSubscribedUsers(user);
                }
            };
        }
    }

    /**
     * Opens the administrator promotion/demotion dialog.
     *
     * @return {@code null} to reload the page.
     */
    public String openPromoteDemoteAdminDialog() {
        displayDialog = ProfileDialog.ADMIN;
        return null;
    }

    /**
     * Opens the dialog for deleting all topic subscriptions.
     *
     * @return {@code null} to reload the page.
     */
    public String openDeleteAllTopicSubscriptionsDialog() {
        displayDialog = ProfileDialog.TOPIC;
        return null;
    }

    /**
     * Opens the dialog for deleting all report subscriptions.
     *
     * @return {@code null} to reload the page.
     */
    public String openDeleteAllReportSubscriptionsDialog() {
        displayDialog = ProfileDialog.REPORT;
        return null;
    }

    /**
     * Opens the dialog for deleting all user subscriptions.
     *
     * @return {@code null} to reload the page.
     */
    public String openDeleteAllUserSubscriptionsDialog() {
        displayDialog = ProfileDialog.USER;
        return null;
    }

    /**
     * Closes any open dialog.
     *
     * @return {@code null} to reload the page.
     */
    public String closeDialog() {
        displayDialog = ProfileDialog.NONE;
        return null;
    }

    /**
     * Returns the voting weight of the user whose profile is viewed. The voting weight is either determined by the
     * number of posts the user has created or directly overwritten by an administrator.
     *
     * @return The voting weight.
     */
    public int getVotingWeight() {
        return votingWeight;
    }

    /**
     * Returns the number of posts the user whose profile is viewed has created. Only posts that have not been deleted
     * are counted.
     *
     * @return The number of posts.
     */
    public int getNumberOfPosts() {
        return numberOfPosts;
    }

    /**
     * Removes the subscription to one particular topic for the user.
     *
     * @param topic The topic of which the subscription to should be removed.
     */
    public void deleteTopicSubscription(final Topic topic) {
        profileService.deleteTopicSubscription(user, topic);
        topicSubscriptions.updateReset();
    }

    /**
     * Removes the subscription to one particular report for the user.
     *
     * @param report The report of which the subscription to should be removed.
     */
    public void deleteReportSubscription(final Report report) {
        profileService.deleteReportSubscription(user, report);
        reportSubscriptions.updateReset();
    }

    /**
     * Removes the subscription to one particular other user for the user.
     *
     * @param subscribee The user of which the subscription to should be removed.
     */
    public void deleteUserSubscription(final User subscribee) {
        profileService.deleteUserSubscription(user, subscribee);
        userSubscriptions.updateReset();
    }

    /**
     * Removes all subscriptions to topics for the user.
     */
    public void deleteAllTopicSubscriptions() {
        profileService.deleteAllTopicSubscriptions(user);
        topicSubscriptions.updateReset();
        closeDialog();
    }

    /**
     * Removes all subscriptions to reports for the user.
     */
    public void deleteAllReportSubscriptions() {
        profileService.deleteAllReportSubscriptions(user);
        reportSubscriptions.updateReset();
        closeDialog();
    }

    /**
     * Removes all subscriptions to other users for the user.
     */
    public void deleteAllUserSubscriptions() {
        profileService.deleteAllUserSubscriptions(user);
        userSubscriptions.updateReset();
        closeDialog();
    }

    /**
     * Subscribes the user to the user whose profile is being viewed.
     */
    public void toggleUserSubscription() {
        User sessionUser = session.getUser();
        if (sessionUser == null || sessionUser.equals(user)) {
            return;
        }

        if (isSubscribed()) {
            profileService.deleteUserSubscription(sessionUser, user);
        } else {
            profileService.subscribeToUser(sessionUser, user);
        }
        subscribed = profileService.isSubscribed(sessionUser, user);
    }

    /**
     * Checks if the current user is subscribed to the user whose profile is being viewed.
     *
     * @return {@code true} iff the current user is a subscriber of the user whose profile is being viewed.
     */
    public boolean isSubscribed() {
        return subscribed;
    }

    /**
     * Checks if the user is privileged, i.e. whether they are viewing their own profile or are an administrator.
     *
     * @return Whether the user is privileged.
     */
    public boolean isPrivileged() {
        return privileged;
    }

    /**
     * Promotes the user whose profile is being viewed to an administrator or demotes the user whose profile is being
     * viewed if they are an administrator. However, if they are the last remaining administrator, an error message is
     * displayed instead.
     */
    public void toggleAdmin() {
        if (session.getUser() == null || !session.getUser().isAdministrator()) {
            log.error("A user was able to to use the promote or demote administrator functionality even though "
                    + "they had no administrator status!");
            return;
        }
        if (!profileService.matchingPassword(session.getUser(), password)) {
            return;
        }
        profileService.toggleAdmin(user);
        if (session.getUser().equals(user)) {
            session.getUser().setAdministrator(user.isAdministrator());
        }
        displayDialog = ProfileDialog.NONE;
    }

    /**
     * Returns the appropriate suffix for the help key.
     *
     * @return The appropriate suffix for the help key.
     */
    public String getHelpSuffix() {
        User user = session.getUser();
        if (user != null) {
            if (user.isAdministrator()) {
                return "_admin";
            } else {
                return "_user";
            }
        }
        return "";
    }

    /**
     * @return The username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username The username to set.
     */
    public void setUsername(final String username) {
        this.username = username;
    }

    /**
     * @return The user.
     */
    public User getUser() {
        return user;
    }

    /**
     * @param user The user to set.
     */
    public void setUser(final User user) {
        this.user = user;
    }

    /**
     * @return The sanitized biography.
     */
    public String getSanitizedBiography() {
        return sanitizedBiography;
    }

    /**
     * @return The topicSubscriptions.
     */
    public Paginator<Topic> getTopicSubscriptions() {
        return topicSubscriptions;
    }

    /**
     * @return The reportSubscriptions.
     */
    public Paginator<Report> getReportSubscriptions() {
        return reportSubscriptions;
    }

    /**
     * @return The userSubscriptions.
     */
    public Paginator<User> getUserSubscriptions() {
        return userSubscriptions;
    }

    /**
     * @return The moderatedTopics.
     */
    public Paginator<Topic> getModeratedTopics() {
        return moderatedTopics;
    }

    /**
     * @return The password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password The password to set.
     */
    public void setPassword(final String password) {
        this.password = password;
    }

    /**
     * @return The DialogType.
     */
    public ProfileDialog getProfileDialog() {
        return displayDialog;
    }

    /**
     * @param displayDialog The DialogType to set.
     */
    public void setProfileDialog(final ProfileDialog displayDialog) {
        this.displayDialog = displayDialog;
    }

}
