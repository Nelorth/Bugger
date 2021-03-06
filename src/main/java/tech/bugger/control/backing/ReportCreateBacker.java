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
import tech.bugger.business.internal.UserSession;
import tech.bugger.business.service.PostService;
import tech.bugger.business.service.ReportService;
import tech.bugger.business.service.TopicService;
import tech.bugger.control.exception.Error404Exception;
import tech.bugger.global.transfer.Attachment;
import tech.bugger.global.transfer.Authorship;
import tech.bugger.global.transfer.Post;
import tech.bugger.global.transfer.Report;
import tech.bugger.global.transfer.Topic;
import tech.bugger.global.transfer.User;

/**
 * Backing Bean for the report create page.
 */
@ViewScoped
@Named
public class ReportCreateBacker implements Serializable {

    @Serial
    private static final long serialVersionUID = 6375834226080077144L;

    /**
     * All possible {@link Report.Type}s.
     */
    public static final Report.Type[] REPORT_TYPES = Report.Type.values();

    /**
     * All possible {@link Report.Severity}s.
     */
    public static final Report.Severity[] REPORT_SEVERITIES = Report.Severity.values();

    /**
     * The topic to create the report in.
     */
    private Topic topic;

    /**
     * The report to create.
     */
    private Report report;

    /**
     * The first post of the report to create.
     */
    private Post firstPost;

    /**
     * The attachment that was just uploaded.
     */
    private Part uploadedAttachment;

    /**
     * The list of attachments of the first post.
     */
    private List<Attachment> attachments;

    /**
     * Whether the user is banned from the topic to create the report in.
     */
    private boolean banned;

    /**
     * The topic service giving access to topics.
     */
    private final TopicService topicService;

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
     * Constructs a new report creation page backing bean with the necessary dependencies.
     *
     * @param topicService  The topic service to use.
     * @param reportService The report service to use.
     * @param postService   The post service to use.
     * @param session       The current user session.
     * @param ectx          The current {@link ExternalContext} of the application.
     */
    @Inject
    public ReportCreateBacker(final TopicService topicService,
                              final ReportService reportService,
                              final PostService postService,
                              final UserSession session,
                              final ExternalContext ectx) {
        this.topicService = topicService;
        this.reportService = reportService;
        this.postService = postService;
        this.session = session;
        this.ectx = ectx;
        this.banned = true;
    }

    /**
     * Initializes the report create page. Checks if the user is allowed to create reports in the first place, i.e. is
     * not banned from the topic the report is located in. If the user may not create reports, acts as if the page did
     * not exist.
     */
    @PostConstruct
    void init() {
        int topicID;
        try {
            topicID = Integer.parseInt(ectx.getRequestParameterMap().get("id"));
        } catch (NumberFormatException e) {
            // Topic ID parameter not given or invalid.
            throw new Error404Exception();
        }

        User user = session.getUser();
        topic = topicService.getTopicByID(topicID);
        if (topic == null || !topicService.canCreateReportIn(user, topic)) {
            throw new Error404Exception();
        }

        banned = false;
        Authorship authorship = new Authorship(user, null, user, null);
        report = new Report(0, "", Report.Type.BUG, Report.Severity.MINOR, "", authorship, null, null, null, false, 0,
                null);
        report.setTopicID(topicID);
        attachments = new ArrayList<>();
        firstPost = new Post(0, "", report.getId(), authorship, attachments);
    }

    /**
     * Saves the new report and its first post to the database.
     * <p>
     * On success, the user is redirected to the report page of the newly created report.
     */
    public void create() {
        if (reportService.createReport(report, firstPost)) {
            try {
                ectx.redirect(ectx.getRequestContextPath() + "/report?id=" + report.getId());
            } catch (IOException e) {
                throw new InternalError("Redirect failed.", e);
            }
        }
    }

    /**
     * Converts the uploaded attachment from a {@link Part} to a {@code byte[]}. The attachment is then associated with
     * the post.
     */
    public void saveAttachment() {
        if (uploadedAttachment != null) {
            postService.addAttachment(firstPost, uploadedAttachment);
        }
    }

    /**
     * Deletes all attachments of the post irreversibly.
     */
    public void deleteAllAttachments() {
        attachments.clear();
    }

    /**
     * Checks if the user is banned from the topic they want to create the report in.
     *
     * @return {@code true} iff the user is banned.
     */
    public boolean isBanned() {
        return banned;
    }

    /**
     * Returns the topic the report is to be created in.
     *
     * @return The topic the report is to be created in.
     */
    public Topic getTopic() {
        return topic;
    }

    /**
     * Sets the topic the report is to be created in.
     *
     * @param topic The topic the report is to be created in.
     */
    public void setTopic(final Topic topic) {
        this.topic = topic;
    }

    /**
     * Returns the report to create.
     *
     * @return The report to create.
     */
    public Report getReport() {
        return report;
    }

    /**
     * Sets the report to create.
     *
     * @param report The report to create..
     */
    public void setReport(final Report report) {
        this.report = report;
    }

    /**
     * Returns the first post of the report to be created.
     *
     * @return The first post of the report to be created.
     */
    public Post getFirstPost() {
        return firstPost;
    }

    /**
     * Sets the first post of the report to be created.
     *
     * @param firstPost The first post of the report to be created.
     */
    public void setFirstPost(final Post firstPost) {
        this.firstPost = firstPost;
    }

    /**
     * Returns the uploaded attachment.
     *
     * @return The uploaded attachment.
     */
    public Part getUploadedAttachment() {
        return uploadedAttachment;
    }

    /**
     * Sets the uploaded attachment.
     *
     * @param uploadedAttachment The uploaded attachment.
     */
    public void setUploadedAttachment(final Part uploadedAttachment) {
        this.uploadedAttachment = uploadedAttachment;
    }

    /**
     * Returns the list of attachments of the first post.
     *
     * @return The list of attachments.
     */
    public List<Attachment> getAttachments() {
        return attachments;
    }

    /**
     * Set the list of attachments of the first post.
     *
     * @param attachments The list of attachments.
     */
    public void setAttachments(final List<Attachment> attachments) {
        this.attachments = attachments;
    }

    /**
     * Returns the current user session.
     *
     * @return The current user session.
     */
    public UserSession getSession() {
        return session;
    }

    /**
     * Returns the list of available report types.
     *
     * @return The list of available report types.
     */
    public Report.Type[] getReportTypes() {
        return REPORT_TYPES;
    }

    /**
     * Returns the list of available report severities.
     *
     * @return The list of available report severities.
     */
    public Report.Severity[] getReportSeverities() {
        return REPORT_SEVERITIES;
    }

}
