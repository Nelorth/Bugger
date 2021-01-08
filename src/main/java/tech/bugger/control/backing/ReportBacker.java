package tech.bugger.control.backing;

import tech.bugger.business.internal.ApplicationSettings;
import tech.bugger.business.internal.UserSession;
import tech.bugger.business.service.PostService;
import tech.bugger.business.service.ReportService;
import tech.bugger.business.service.TopicService;
import tech.bugger.business.util.Paginator;
import tech.bugger.global.transfer.Post;
import tech.bugger.global.transfer.Report;
import tech.bugger.global.transfer.Selection;
import tech.bugger.global.util.Log;

import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serial;
import java.io.Serializable;

/**
 * Backing bean for the report page.
 */
@ViewScoped
@Named
public class ReportBacker implements Serializable {

    public enum ReportPageDialog {

        /**
         * Delete a post.
         */
        DELETE_POST,

        /**
         * Delete the report.
         */
        DELETE_REPORT,

        /**
         * Open the report.
         */
        OPEN,

        /**
         * Close the report.
         */
        CLOSE,

        /**
         * Mark the report as a duplicate of another report.
         */
        DUPLICATE
    }

    /**
     * The {@link Log} instance associated with this class for logging purposes.
     */
    private static final Log log = Log.forClass(ReportBacker.class);

    @Serial
    private static final long serialVersionUID = 7260516443406682026L;

    /**
     * The report ID.
     */
    private int reportID;

    /**
     * The report.
     */
    private Report report;

    /**
     * The paginated list of posts.
     */
    private Paginator<Post> posts;

    /**
     * The ID of the report the current report is potentially a duplicate of.
     */
    private int duplicateOfID;

    /**
     * The overwriting relevance.
     */
    private Integer overwritingRelevance;

    /**
     * The post to be deleted.
     */
    private Post postToBeDeleted;

    /**
     * The currently displayed dialog.
     */
    private ReportPageDialog currentDialog;

    /**
     * The application settings cache.
     */
    private final ApplicationSettings applicationSettings;

    /**
     * The report service providing logic.
     */
    private final ReportService reportService;

    /**
     * The post service providing logic.
     */
    private final PostService postService;

    /**
     * The topic service providing logic.
     */
    private final TopicService topicService;

    /**
     * The user session.
     */
    private final UserSession session;

    /**
     * The current {@link FacesContext}.
     */
    private final FacesContext fctx;

    /**
     * Constructs a new report page backing bean with the necessary dependencies.
     *
     * @param applicationSettings The application settings cache.
     * @param reportService       The report service to use.
     * @param postService         The post service to use.
     * @param topicService        The topic service to use.
     * @param session             The user session.
     * @param fctx                The current {@link FacesContext} of the application.
     */
    @Inject
    public ReportBacker(final ApplicationSettings applicationSettings, final ReportService reportService,
                        final PostService postService, final TopicService topicService, final UserSession session,
                        final FacesContext fctx) {
        this.applicationSettings = applicationSettings;
        this.reportService = reportService;
        this.postService = postService;
        this.topicService = topicService;
        this.session = session;
        this.fctx = fctx;
    }

    /**
     * Initializes the report page. Loads the first few posts of the report to display them. Also checks if the user is
     * allowed to view the report. If not, acts as if the page did not exist.
     */
    @PostConstruct
    void init() {
        currentDialog = null;

        posts = new Paginator<>("created_at", Selection.PageSize.NORMAL) {
            @Override
            protected Iterable<Post> fetch() {
                return reportService.getPostsFor(report, getSelection());
            }

            @Override
            protected int totalSize() {
                return reportService.getNumberOfPosts(report);
            }
        };
    }

    /**
     * Displays the specified dialog and reloads the page. {@code null} closes the dialog.
     *
     * @param dialog The dialog to display.
     * @return {@code null} to reload the page.
     */
    public String displayDialog(final ReportPageDialog dialog) {
        currentDialog = dialog;
        return null;
    }

    /**
     * Returns the relevance of the report as saved in the data source.
     *
     * @return the relevance of the report.
     */
    public int getRelevance() {
        return 0;
    }

    /**
     * Adds or removes a subscription to the report for the user, whichever is applicable.
     */
    public void toggleReportSubscription() {
    }

    /**
     * Increases the relevance of the report by the user's voting weight.
     */
    public void upvote() {

    }

    /**
     * Decreases the relevance of the report by the user's voting weight.
     */
    public void downvote() {

    }

    /**
     * Returns if the user has voted to increase the relevance of the report.
     *
     * @return {@code true} if the user has voted up and {@code false} otherwise.
     */
    public boolean hasUpvoted() {
        return false;
    }

    /**
     * Returns if the user has voted to decrease the relevance of the report.
     *
     * @return {@code true} if the user has voted down and {@code false} otherwise.
     */
    public boolean hasDownvoted() {
        return false;
    }

    /**
     * Opens a closed report and closes an open one.
     */
    public void toggleOpenClosed() {
        if (report.getClosingDate() == null) {
            reportService.close(report);
        } else {
            reportService.open(report);
        }
    }

    /**
     * Deletes the report along with all its posts irreversibly.
     */
    public void delete() {
        reportService.deleteReport(report);
    }

    /**
     * Marks the report as a duplicate of another report. This automatically closes the report.
     */
    public void markDuplicate() {

    }

    /**
     * Removes the marking signifying that the report is a duplicate of another one.
     */
    public void unmarkDuplicate() {

    }

    /**
     * Overwrites the relevance of the report with a set value.
     */
    public void overwriteRelevance() {

    }

    /**
     * Deletes the selected post irreversibly. If it is the first post, this deletes the whole report.
     *
     * @param post The post to be deleted.
     */
    public void deletePost(final Post post) {
        postService.deletePost(post);
    }

    /**
     * Checks if the user is privileged, i.e. an admin, a mod or the creator of the report.
     *
     * @return {@code true} if the user is privileged and {@code false} otherwise.
     */
    public boolean isPrivileged() {
        return reportService.isPrivileged(session.getUser(), report);
    }

    /**
     * Checks if the user is privileged for the post.
     *
     * @param post The post in question.
     * @return {@code true} iff the user is privileged.
     */
    public boolean isPrivileged(final Post post) {
        return postService.isPrivileged(session.getUser(), post);
    }

    /**
     * Checks if the user is a moderator of the topic the report is located in.
     *
     * @return {@code true} if the user is a moderator and {@code false} otherwise.
     */
    public boolean isModerator() {
        return false;
    }

    /**
     * Checks if the user is banned from the topic the report is located in.
     *
     * @return {@code true} if the user is banned and {@code false} otherwise.
     */
    public boolean isBanned() {
        return false;
    }

    /**
     * @return The report.
     */
    public Report getReport() {
        return report;
    }

    /**
     * @param report The report to set.
     */
    public void setReport(final Report report) {
        this.report = report;
    }

    /**
     * @return The duplicateOfID.
     */
    public int getDuplicateOfID() {
        return duplicateOfID;
    }

    /**
     * @param duplicateOfID The duplicateOfID to set.
     */
    public void setDuplicateOfID(final int duplicateOfID) {
        this.duplicateOfID = duplicateOfID;
    }

    /**
     * @return The postToBeDeleted.
     */
    public Post getPostToBeDeleted() {
        return postToBeDeleted;
    }

    /**
     * @param postToBeDeleted The postToBeDeleted to set.
     */
    public void setPostToBeDeleted(final Post postToBeDeleted) {
        this.postToBeDeleted = postToBeDeleted;
    }

    /**
     * @return The posts.
     */
    public Paginator<Post> getPosts() {
        return posts;
    }

    /**
     * Gets the current dialog.
     *
     * @return The current dialog.
     */
    public ReportPageDialog getCurrentDialog() {
        return currentDialog;
    }

    /**
     * @param overwritingRelevance The overwritingRelevance to set.
     */
    public void setOverwritingRelevance(final Integer overwritingRelevance) {
        this.overwritingRelevance = overwritingRelevance;
    }

    /**
     * @return The overwritingRelevance.
     */
    public Integer getOverwritingRelevance() {
        return overwritingRelevance;
    }

    /**
     * @return The reportID.
     */
    public int getReportID() {
        return reportID;
    }

    /**
     * @param reportID The reportID to set.
     */
    public void setReportID(final int reportID) {
        this.reportID = reportID;
    }

}
