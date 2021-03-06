package tech.bugger.global.transfer;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * DTO representing a report.
 */
public class Report implements Serializable {

    @Serial
    private static final long serialVersionUID = -8063035819918388897L;

    /**
     * The available report types.
     */
    public enum Type {
        /**
         * A bug report.
         */
        BUG,

        /**
         * A feature request.
         */
        FEATURE,

        /**
         * A hint to something.
         */
        HINT
    }

    /**
     * The available severity levels.
     */
    public enum Severity {
        /**
         * A low severity.
         */
        MINOR,

        /**
         * A medium severity.
         */
        RELEVANT,

        /**
         * A high severity.
         */
        SEVERE
    }

    /**
     * The report ID.
     */
    private Integer id;

    /**
     * The report title.
     */
    private String title;

    /**
     * The report type.
     */
    private Type type;

    /**
     * The report severity.
     */
    private Severity severity;

    /**
     * The associated software version of the report.
     */
    private String version;

    /**
     * The authorship of the report.
     */
    private Authorship authorship;

    /**
     * The date and time when the report was closed.
     */
    private OffsetDateTime closingDate;

    /**
     * The ID of the report this report is a duplicate of.
     */
    private Integer duplicateOf;

    /**
     * The relevance value of the report.
     */
    private Integer relevance;

    /**
     * The relevance value of the report.
     */
    private OffsetDateTime lastActivity;

    /**
     * {@code true} if the relevance is overwritten.
     */
    private boolean relevanceOverwritten;

    /**
     * The ID of the topic this report is in.
     */
    private int topicID;

    /**
     * The name of the topic this report is in.
     */
    private String topic;

    /**
     * Constructs a new report.
     *
     * @param id                   The report ID.
     * @param title                The report title.
     * @param type                 The report type.
     * @param severity             The report severity.
     * @param version              The version the report is associated with.
     * @param authorship           The report authorship metadata.
     * @param closingDate          The closing date of the report.
     * @param duplicateOf          The report this report is a duplicate of.
     * @param relevance            The relevance value for the Report.
     * @param relevanceOverwritten The state of the relevance overwrite.
     * @param topicID              The ID of topic the report belongs to.
     * @param topic                The title of topic the report belongs to.
     */
    public Report(final Integer id, final String title, final Type type, final Severity severity, final String version,
                  final Authorship authorship, final OffsetDateTime closingDate, final Integer duplicateOf,
                  final Integer relevance, final boolean relevanceOverwritten, final int topicID, final String topic) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.severity = severity;
        this.version = version;
        this.authorship = authorship;
        this.closingDate = closingDate;
        this.duplicateOf = duplicateOf;
        this.relevance = relevance;
        this.relevanceOverwritten = relevanceOverwritten;
        this.topicID = topicID;
        this.topic = topic;
    }

    /**
     * Constructs an empty report.
     */
    public Report() {
        this(0, "", Type.BUG, Severity.MINOR, "", new Authorship(null, null, null, null), null, null, null, false, 0,
                null);
    }

    /**
     * Constructs a new report as deep clone of the given report.
     *
     * @param report The report to clone.
     */
    public Report(final Report report) {
        this(report.id, report.title, report.type, report.severity, report.version, report.authorship,
                report.closingDate, report.duplicateOf, report.relevance, report.relevanceOverwritten, report.topicID,
                report.topic);
        this.lastActivity = report.lastActivity;
        this.topic = report.topic;
    }

    /**
     * Returns the ID of this report.
     *
     * @return The report ID.
     */
    public Integer getId() {
        return id;
    }

    /**
     * Sets the ID of this report.
     *
     * @param id The report ID to be set.
     */
    public void setId(final Integer id) {
        this.id = id;
    }

    /**
     * Returns the title of this report.
     *
     * @return The report title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of this report.
     *
     * @param title The report title to be set.
     */
    public void setTitle(final String title) {
        this.title = title;
    }

    /**
     * Returns the type of this report.
     *
     * @return The report type.
     */
    public Type getType() {
        return type;
    }

    /**
     * Sets the type of this report.
     *
     * @param type The report type to be set.
     */
    public void setType(final Type type) {
        this.type = type;
    }

    /**
     * Returns the severity of this report.
     *
     * @return The report severity.
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Sets the severity of this report.
     *
     * @param severity The report severity to be set.
     */
    public void setSeverity(final Severity severity) {
        this.severity = severity;
    }

    /**
     * Returns the version this report is associated with.
     *
     * @return The associated version.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version this report is associated with.
     *
     * @param version The associated version to be set.
     */
    public void setVersion(final String version) {
        this.version = version;
    }

    /**
     * Returns the manipulation metadata of this report.
     *
     * @return The report authorship metadata.
     */
    public Authorship getAuthorship() {
        return authorship;
    }

    /**
     * Sets the manipulation metadata of this report.
     *
     * @param authorship The report authorship metadata to be set.
     */
    public void setAuthorship(final Authorship authorship) {
        this.authorship = authorship;
    }

    /**
     * Returns the data this report was closed.
     *
     * @return The report closing date.
     */
    public OffsetDateTime getClosingDate() {
        return closingDate;
    }

    /**
     * Returns the data this report was closed.
     *
     * @param closingDate The report closing date.
     */
    public void setClosingDate(final OffsetDateTime closingDate) {
        this.closingDate = closingDate;
    }

    /**
     * Returns the report this report is a duplicate of.
     *
     * @return The original report or {@code null} if this is no duplicate.
     */
    public Integer getDuplicateOf() {
        return duplicateOf;
    }

    /**
     * Sets the report this report is a duplicate of.
     *
     * @param duplicateOf The original report to be set
     */
    public void setDuplicateOf(final Integer duplicateOf) {
        this.duplicateOf = duplicateOf;
    }

    /**
     * Returns the relevance value.
     *
     * @return The relevance or the report.
     */
    public Integer getRelevance() {
        return relevance;
    }

    /**
     * Sets the relevance value.
     *
     * @param relevance The relevance to be set.
     */
    public void setRelevance(final Integer relevance) {
        this.relevance = relevance;
    }

    /**
     * Returns the topic this report belongs to.
     *
     * @return The associated topic.
     */
    public int getTopicID() {
        return topicID;
    }

    /**
     * Sets the topic this report belongs to.
     *
     * @param topicID The associated topic to be set.
     */
    public void setTopicID(final int topicID) {
        this.topicID = topicID;
    }

    /**
     * @return The title of the associated topic
     */
    public String getTopic() {
        return topic;
    }

    /**
     * @param topic The new title of the associated topic.
     */
    public void setTopic(final String topic) {
        this.topic = topic;
    }

    /**
     * @return The last activity in this report.
     */
    public OffsetDateTime getLastActivity() {
        return lastActivity;
    }

    /**
     * @param lastActivity The new last activity in this report.
     */
    public void setLastActivity(final OffsetDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }

    /**
     * Returns weather the relevance of this report is overwritten.
     *
     * @return {@code true} iff relevance of this topic is overwritten.
     */
    public boolean isRelevanceOverwritten() {
        return relevanceOverwritten;
    }

    /**
     * Sets weather the relevance of this topic should be overwritten.
     *
     * @param relevanceOverwritten {@code true} iff relevance of this topic should be overwritten.
     */
    public void setRelevanceOverwritten(final boolean relevanceOverwritten) {
        this.relevanceOverwritten = relevanceOverwritten;
    }

    /**
     * Indicates whether some {@code other} report is semantically equal to this report.
     *
     * @param other The object to compare this report to.
     * @return {@code true} iff {@code other} is a semantically equivalent report.
     */
    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Report)) {
            return false;
        }
        Report that = (Report) other;
        return Objects.equals(id, that.id);
    }

    /**
     * Calculates a hash code for this report for hashing purposes, and to fulfil the {@link Object#equals(Object)}
     * contract.
     *
     * @return The hash code value of this report.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Converts this report into a human-readable string representation.
     *
     * @return A human-readable string representation of this report.
     */
    @Override
    public String toString() {
        return "Report{"
                + "id=" + id
                + ", title='" + title + '\''
                + ", type=" + type
                + ", severity=" + severity
                + ", version='" + version + '\''
                + ", authorship=" + authorship
                + ", closingDate=" + closingDate
                + ", duplicateOf=" + duplicateOf
                + ", relevance=" + relevance
                + ", relevanceOverwritten" + relevanceOverwritten
                + ", topic=" + topicID
                + '}';
    }

}
