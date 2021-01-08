package tech.bugger.control.backing;

import tech.bugger.business.internal.UserSession;
import tech.bugger.business.service.ReportService;
import tech.bugger.business.service.TopicService;
import tech.bugger.business.util.Feedback;
import tech.bugger.business.util.Registry;
import tech.bugger.global.transfer.Report;
import tech.bugger.global.transfer.Topic;
import tech.bugger.global.transfer.User;
import tech.bugger.global.util.Log;

import javax.enterprise.event.Event;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Backing Bean for the report edit page.
 */
@ViewScoped
@Named
public class ReportEditBacker implements Serializable {

    /**
     * The {@link Log} instance associated with this class for logging purposes.
     */
    private static final Log log = Log.forClass(ReportEditBacker.class);

    @Serial
    private static final long serialVersionUID = -1310546265441099227L;

    /**
     * The ID of the report to edit.
     */
    private int reportID;

    /**
     * The report to edit.
     */
    private Report report;

    /**
     * The current topic of the report to edit.
     */
    private Topic currentTopic;

    /**
     * The ID of the topic to move the report to.
     */
    private int destinationID;

    /**
     * Whether to display the confirmation dialog.
     */
    private boolean displayConfirmDialog;

    /**
     * Whether the user is allowed to edit the report.
     */
    private boolean privileged;

    /**
     * The topic service giving access to topics.
     */
    private transient TopicService topicService;

    /**
     * The report service creating reports.
     */
    private transient ReportService reportService;

    /**
     * The current user session.
     */
    private UserSession session;

    /**
     * The current {@link ExternalContext}.
     */
    private FacesContext fctx;

    /**
     * Feedback event for user feedback.
     */
    private transient Event<Feedback> feedbackEvent;

    /**
     * Resource bundle for feedback message.
     */
    private transient ResourceBundle messagesBundle;

    /**
     * Constructs a new report editing page backing bean with the necessary dependencies.
     *
     * @param topicService        The topic service to use.
     * @param reportService       The report service to use.
     * @param session             The current user session.
     * @param fctx                The current {@link FacesContext} of the application.
     * @param feedbackEvent       The feedback event to use for user feedback.
     * @param registry            The dependency registry to use.
     */
    @Inject
    public ReportEditBacker(final TopicService topicService, final ReportService reportService,
                            final UserSession session, final FacesContext fctx,
                            final Event<Feedback> feedbackEvent, final Registry registry) {
        this.topicService = topicService;
        this.reportService = reportService;
        this.session = session;
        this.fctx = fctx;
        this.feedbackEvent = feedbackEvent;
        this.messagesBundle = registry.getBundle("messages", session);
        this.privileged = false;
    }

    /**
     * Initializes the report edit page. Loads the report to be edited and checks if the user is allowed to edit the
     * report. If this is not the case, acts as if the page did not exist.
     */
    public void init() {
        reportID = 104; // Use fixed ID to at least display page with contents. TODO: Remove
        report = reportService.getReportByID(reportID);
        if (report != null) {
            destinationID = report.getTopic();
            currentTopic = topicService.getTopicByID(destinationID);
            User user = session.getUser();

            privileged = user != null && currentTopic != null
                    && (user.equals(report.getAuthorship().getCreator())
                        || user.isAdministrator()
                        || topicService.isModerator(user, currentTopic));
        }

        if (!privileged) {
            redirectTo404Page();
            return;
        }
    }

    /**
     * Opens the confirmation dialog.
     *
     * @return {@code null} to reload the page.
     */
    public String openConfirmDialog() {
        displayConfirmDialog = true;
        return null;
    }

    /**
     * Closes the confirmation dialog.
     *
     * @return {@code null} to reload the page.
     */
    public String closeConfirmDialog() {
        displayConfirmDialog = false;
        return null;
    }

    /**
     * Saves the changes made into the database or opens the confirmation dialog if the report's topic changed.
     *
     * @return The page to navigate to.
     */
    public String saveChangesWithConfirm() {
        if (report.getTopic() == destinationID) {
            return saveChanges();
        }

        if (canMoveToTopic()) {
            openConfirmDialog();
        }
        return null;
    }

    /**
     * Saves the changes made into the database.
     *
     * @return The page to navigate to.
     */
    public String saveChanges() {
        report.setTopic(destinationID);
        if (reportService.updateReport(report)) {
            return "report.xhtml?r=" + report.getId();
        } else {
            return null;
        }
    }

    /**
     * Returns whether to display a warning that the report will be moved to a topic that the user does not moderate.
     *
     * @return Whether to display the warning.
     */
    public boolean isDisplayNoModerationWarning() {
        if (destinationID == currentTopic.getId()) {
            return false;
        } else {
            Topic destination = topicService.getTopicByID(destinationID);
            User user = session.getUser();
            return user != null
                    && destination != null
                    && topicService.isModerator(user, currentTopic)
                    && !topicService.isModerator(user, destination);
        }
    }

    /**
     * Checks whether the topic with ID {@code destinationID} exists and the user is allowed to move the report to it.
     * Displays an error message if not.
     *
     * @return Whether the report can be moved to the destination topic.
     */
    private boolean canMoveToTopic() {
        Topic destination = topicService.getTopicByID(destinationID);
        if (destination == null || topicService.isBanned(session.getUser(), destination)) {
            String message = MessageFormat.format(messagesBundle.getString("report_edit_topic_not_found"),
                    destinationID);
            fctx.addMessage("f-report-edit:it-topic", new FacesMessage(message));
            destinationID = currentTopic.getId();
            return false;
        } else {
            return true;
        }
    }

    /**
     * Redirects the user to a 404 page.
     */
    private void redirectTo404Page() {
        // This will be subject to change when the error page is implemented.
        try {
            ExternalContext ectx = fctx.getExternalContext();
            ectx.redirect(ectx.getRequestContextPath() + "/faces/view/public/error.xhtml");
        } catch (IOException e) {
            throw new InternalError("Redirection to error page failed.");
        }
    }

    /**
     * Signals whether the user is allowed to edit the report. Only administrators, moderators of the topic the report
     * belongs to and the creator (provided they are not banned) may edit the report.
     *
     * @return {@code true} if the user may edit the report and {@code false} otherwise.
     */
    public boolean isPrivileged() {
        return privileged;
    }

    /**
     * Returns the report to edit.
     *
     * @return The report to edit.
     */
    public Report getReport() {
        return report;
    }

    /**
     * Sets the report to edit.
     *
     * @param report The report to edit.
     */
    public void setReport(Report report) {
        this.report = report;
    }

    /**
     * Returns the current topic of the report to edit.
     *
     * @return The current topic.
     */
    public Topic getCurrentTopic() {
        return currentTopic;
    }

    /**
     * Sets the current topic of the report to edit.
     *
     * @param currentTopic The current topic.
     */
    public void setCurrentTopic(Topic currentTopic) {
        this.currentTopic = currentTopic;
    }

    /**
     * Returns the ID of the topic the report is supposed to be moved to.
     *
     * @return The ID of the destination topic,
     */
    public int getDestinationID() {
        return destinationID;
    }

    /**
     * Sets the ID of the topic the report is to be moved to.
     *
     * @param destinationID The ID of topic to move the report to.
     */
    public void setDestinationID(int destinationID) {
        this.destinationID = destinationID;
    }

    /**
     * Returns the ID of the report to edit.
     *
     * @return The ID of the report to edit.
     */
    public int getReportID() {
        return reportID;
    }

    /**
     * Sets the ID of the report to edit.
     * @param reportID The ID of the report to edit.
     */
    public void setReportID(int reportID) {
        this.reportID = reportID;
    }

    /**
     * Returns whether the confirmation dialog is to be shown.
     *
     * @return Whether the confirmation dialog is to be shown.
     */
    public boolean isDisplayConfirmDialog() {
        return displayConfirmDialog;
    }

    /**
     * Returns the list of available report types.
     *
     * @return The list of available report types.
     */
    public Report.Type[] getReportTypes() {
        return Report.Type.values();
    }

    /**
     * Returns the list of available report severities.
     *
     * @return The list of available report severities.
     */
    public Report.Severity[] getReportSeverities() {
        return Report.Severity.values();
    }

}
