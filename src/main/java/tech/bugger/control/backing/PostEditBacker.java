package tech.bugger.control.backing;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.context.ExternalContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.Part;
import tech.bugger.business.internal.ApplicationSettings;
import tech.bugger.business.internal.UserSession;
import tech.bugger.business.service.PostService;
import tech.bugger.business.service.ReportService;
import tech.bugger.control.exception.Error404Exception;
import tech.bugger.global.transfer.Attachment;
import tech.bugger.global.transfer.Authorship;
import tech.bugger.global.transfer.Post;
import tech.bugger.global.transfer.Report;
import tech.bugger.global.transfer.User;

/**
 * Backing bean for the post edit page.
 */
@ViewScoped
@Named
public class PostEditBacker implements Serializable {

    @Serial
    private static final long serialVersionUID = -973315118868047411L;

    /**
     * Whether to create a new post or edit an existing one.
     */
    private boolean create;

    /**
     * The post to edit.
     */
    private Post post;

    /**
     * The report of the post.
     */
    private Report report;

    /**
     * The attachment that was last uploaded.
     */
    private Part lastAttachmentUploaded;

    /**
     * The list of attachments of the post to edit.
     */
    private List<Attachment> attachments;

    /**
     * The current application settings.
     */
    private final ApplicationSettings applicationSettings;

    /**
     * The report service creating reports.
     */
    private final ReportService reportService;

    /**
     * The post service validating posts and attachments.
     */
    private final PostService postService;

    /**
     * The current user session.
     */
    private final UserSession session;

    /**
     * The current {@link ExternalContext}.
     */
    private final ExternalContext ectx;

    /**
     * Constructs a new post editing backing bean with the necessary dependencies.
     *
     * @param applicationSettings The current application settings.
     * @param reportService       The report service to use.
     * @param postService         The post service to use.
     * @param session             The current user session.
     * @param ectx                The current {@link ExternalContext} of the application.
     */
    @Inject
    public PostEditBacker(final ApplicationSettings applicationSettings,
                          final ReportService reportService,
                          final PostService postService,
                          final UserSession session,
                          final ExternalContext ectx) {
        this.applicationSettings = applicationSettings;
        this.reportService = reportService;
        this.postService = postService;
        this.session = session;
        this.ectx = ectx;
        attachments = new ArrayList<>();
    }

    /**
     * Initializes the page for creating and editing posts. Checks if the user is allowed to view the page in the first
     * place. If not, acts as if the page did not exist.
     */
    @PostConstruct
    void init() {
        User user = session.getUser();
        create = ectx.getRequestParameterMap().containsKey("c");
        if (create) {
            Integer reportID = parseRequestParameter("r");
            if (reportID == null) {
                throw new Error404Exception();
            }
            report = reportService.getReportByID(reportID);
            if (report == null || !reportService.canPostInReport(user, report)) {
                throw new Error404Exception();
            }
            Authorship authorship = new Authorship(user, null, user, null);
            post = new Post(0, "", report.getId(), authorship, attachments);
        } else {
            Integer postID = parseRequestParameter("p");
            if (postID == null) {
                throw new Error404Exception();
            }
            post = postService.getPostByID(postID);
            if (post == null) {
                throw new Error404Exception();
            }
            report = reportService.getReportByID(post.getReport());
            if (report == null || !postService.isPrivileged(user, post, report)) {
                throw new Error404Exception();
            }

            attachments = post.getAttachments();
            post.getAuthorship().setModifier(user);
        }

        if (report.getClosingDate() != null && !applicationSettings.getConfiguration().isClosedReportPosting()) {
            throw new Error404Exception();
        }
    }

    /**
     * Creates a new post or saves the changes made to an existing post. On success, the user is redirected to the post
     * in its report page.
     */
    public void saveChanges() {
        boolean success = create ? postService.createPost(post, report) : postService.updatePost(post, report);
        if (success) {
            try {
                ectx.redirect(ectx.getRequestContextPath() + "/report?p=" + post.getId() + "#post-" + post.getId());
            } catch (IOException e) {
                throw new InternalError("Redirection failed.", e);
            }
        }
    }

    /**
     * Converts the last uploaded attachment into a {@code byte[]} and puts it into the post. If the maximum number of
     * attachments has already been reached, displays an error message instead.
     */
    public void uploadAttachment() {
        if (lastAttachmentUploaded != null) {
            postService.addAttachment(post, lastAttachmentUploaded);
        }
    }

    /**
     * Clears the list of attachments of the post.
     */
    public void deleteAllAttachments() {
        attachments.clear();
    }

    /**
     * Tries to parse a request parameter to an integer.
     *
     * @param param The name of the parameter to parse.
     * @return The integer value of the request parameter, {@code null} if the parameter could not be parsed.
     */
    private Integer parseRequestParameter(final String param) {
        try {
            return Integer.parseInt(ectx.getRequestParameterMap().get(param));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Returns the report to create the new post in.
     *
     * @return The report to create the new post in.
     */
    public Report getReport() {
        return report;
    }

    /**
     * Sets the report to create the new post in.
     *
     * @param report The report to set.
     */
    public void setReport(final Report report) {
        this.report = report;
    }

    /**
     * The post to edit.
     *
     * @return The post to edit.
     */
    public Post getPost() {
        return post;
    }

    /**
     * Sets the post to edit.
     *
     * @param post The post ID to set.
     */
    public void setPost(final Post post) {
        this.post = post;
    }

    /**
     * @return The lastAttachmentUploaded.
     */
    public Part getLastAttachmentUploaded() {
        return lastAttachmentUploaded;
    }

    /**
     * @param lastAttachmentUploaded The lastAttachmentUploaded to set.
     */
    public void setLastAttachmentUploaded(final Part lastAttachmentUploaded) {
        this.lastAttachmentUploaded = lastAttachmentUploaded;
    }

    /**
     * Returns the list of attachments of the post.
     *
     * @return The list of attachments of the post.
     */
    public List<Attachment> getAttachments() {
        return attachments;
    }

    /**
     * Sets the list of attachments of the post.
     *
     * @param attachments The list of attachments to set.
     */
    public void setAttachments(final List<Attachment> attachments) {
        this.attachments = attachments;
    }

    /**
     * Returns whether to create a new post or edit an existing one.
     *
     * @return Whether to create or edit a post.
     */
    public boolean isCreate() {
        return create;
    }

    /**
     * Sets whether to create a new post or edit an existing one.
     *
     * @param create Whether to create or edit a post.
     */
    public void setCreate(final boolean create) {
        this.create = create;
    }

}
