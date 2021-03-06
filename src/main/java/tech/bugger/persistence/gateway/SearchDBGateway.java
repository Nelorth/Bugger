package tech.bugger.persistence.gateway;

import com.ocpsoft.pretty.faces.util.StringUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import tech.bugger.global.transfer.Report;
import tech.bugger.global.transfer.Selection;
import tech.bugger.global.transfer.Topic;
import tech.bugger.global.transfer.User;
import tech.bugger.global.util.Log;
import tech.bugger.global.util.Pagitable;
import tech.bugger.persistence.exception.StoreException;
import tech.bugger.persistence.util.StatementParametrizer;

/**
 * Search gateway that retrieves search results from data stored in a database.
 */
public class SearchDBGateway implements SearchGateway {

    /**
     * The {@link Log} instance associated with this class for logging purposes.
     */
    private static final Log log = Log.forClass(SearchDBGateway.class);

    /**
     * The database connection used by this gateway.
     */
    private final Connection conn;

    /**
     * Constructs a new search gateway with the given database connection.
     *
     * @param conn The database connection to use for the gateway.
     */
    public SearchDBGateway(final Connection conn) {
        this.conn = conn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<User> getUserResults(final String query, final Selection selection, final boolean showAdmins,
                                     final boolean showNonAdmins) {
        if (selection == null || query == null) {
            log.error("The selection or query cannot be null!");
            throw new IllegalArgumentException("The selection or query cannot be null!");
        } else if (StringUtils.isBlank(selection.getSortedBy())) {
            log.error("Error when trying to get users sorted by nothing.");
            throw new IllegalArgumentException("The selection needs to have a column to sort by.");
        } else if (selection.getSortedBy().equals("relevance")) {
            log.error("The selection can not be sorted by relevance.");
            throw new IllegalArgumentException("The selection can not be sorted by relevance.");
        }
        List<User> userResults = new ArrayList<>(Math.max(0, selection.getTotalSize()));
        if (!showAdmins && !showNonAdmins) {
            return userResults;
        }
        String adminFilter = "";
        if (showAdmins) {
            adminFilter = "AND is_admin = true ";
        }
        if (showNonAdmins) {
            if (showAdmins) {
                adminFilter = "";
            } else {
                adminFilter = "AND is_admin = false ";
            }
        }
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM \"user\" as u "
                + "LEFT OUTER JOIN user_num_posts as p "
                + "on u.id = p.author WHERE TRIM(LOWER(username)) LIKE CONCAT('%',?,'%') "
                + adminFilter
                + "ORDER BY " + selection.getSortedBy() + (selection.isAscending() ? " ASC " : " DESC ")
                + "LIMIT ? OFFSET ?;")) {
            ResultSet rs = new StatementParametrizer(stmt)
                    .string(query)
                    .integer(Pagitable.getItemLimit(selection))
                    .integer(Pagitable.getItemOffset(selection))
                    .toStatement().executeQuery();

            while (rs.next()) {
                userResults.add(getSearchedUserFromResultSet(rs));
            }
        } catch (SQLException e) {
            log.error("Error while loading the user search suggestions for the query " + query, e);
            throw new StoreException("Error while loading the user search suggestions for the query " + query, e);
        }
        return userResults;
    }

    /**
     * Parses the given {@link ResultSet} and returns the corresponding {@link User}.
     *
     * @param rs The {@link ResultSet} to parse.
     * @return The parsed {@link User}.
     * @throws SQLException Some parsing error occurred.
     */
    static User getSearchedUserFromResultSet(final ResultSet rs) throws SQLException {
        User user = new User();
        user.setUsername(rs.getString("username"));
        user.setAdministrator(rs.getBoolean("is_admin"));
        user.setProfileVisibility(User.ProfileVisibility.valueOf(rs.getString("profile_visibility").toUpperCase()));
        if (user.getProfileVisibility().equals(User.ProfileVisibility.FULL)) {
            user.setFirstName(rs.getString("first_name"));
            user.setLastName(rs.getString("last_name"));
        } else {
            user.setFirstName("");
            user.setLastName("");
        }
        user.setForcedVotingWeight(rs.getObject("forced_voting_weight", Integer.class));
        user.setNumPosts(rs.getInt("num_posts"));
        return user;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getUserBanSuggestions(final String query, final int limit, final Topic topic) {
        validateSuggestionParams(query, limit, topic);
        List<String> userResults = new ArrayList<>(limit);

        try (PreparedStatement stmt = conn.prepareStatement("SELECT u.username FROM \"user\" AS u WHERE u.username "
                + "LIKE CONCAT('%', ?, '%') AND u.is_admin = false AND u.id NOT IN (SELECT t.outcast FROM topic_ban "
                + "AS t WHERE t.topic = ?) AND u.id NOT IN (SELECT m.moderator FROM topic_moderation AS m "
                + "WHERE m.topic = ?) LIMIT ?;")) {
            ResultSet rs = new StatementParametrizer(stmt)
                    .string(query)
                    .integer(topic.getId())
                    .integer(topic.getId())
                    .integer(limit)
                    .toStatement().executeQuery();

            while (rs.next()) {
                userResults.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            log.error("Error while loading the user search suggestions for the query " + query, e);
            throw new StoreException("Error while loading the user search suggestions for the query " + query, e);
        }

        return userResults;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getUserUnbanSuggestions(final String query, final int limit, final Topic topic) {
        validateSuggestionParams(query, limit, topic);
        List<String> userResults = new ArrayList<>(limit);

        try (PreparedStatement stmt = conn.prepareStatement("SELECT u.username FROM \"user\" AS u INNER JOIN "
                + "topic_ban as t ON t.outcast = u.id WHERE u.username LIKE CONCAT('%', ?, '%') AND t.topic = ? "
                + "LIMIT ?;")) {
            ResultSet rs = new StatementParametrizer(stmt)
                    .string(query)
                    .integer(topic.getId())
                    .integer(limit)
                    .toStatement().executeQuery();

            while (rs.next()) {
                userResults.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            log.error("Error while loading the user search suggestions for the query " + query, e);
            throw new StoreException("Error while loading the user search suggestions for the query " + query, e);
        }

        return userResults;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getUserModSuggestions(final String query, final int limit, final Topic topic) {
        validateSuggestionParams(query, limit, topic);
        List<String> modResults = new ArrayList<>(limit);

        try (PreparedStatement stmt = conn.prepareStatement("SELECT u.username FROM \"user\" AS u WHERE u.username "
                + "LIKE CONCAT('%', ?, '%') AND u.is_admin = false AND u.id NOT IN (SELECT t.moderator FROM "
                + "topic_moderation AS t WHERE t.topic = ?) LIMIT ?;")) {
            ResultSet rs = new StatementParametrizer(stmt)
                    .string(query)
                    .integer(topic.getId())
                    .integer(limit)
                    .toStatement().executeQuery();

            while (rs.next()) {
                modResults.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            log.error("Error while loading the user search suggestions for the query " + query, e);
            throw new StoreException("Error while loading the user search suggestions for the query " + query, e);
        }
        return modResults;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getUserUnmodSuggestions(final String query, final int limit, final Topic topic) {
        validateSuggestionParams(query, limit, topic);
        List<String> unmodResults = new ArrayList<>(limit);

        try (PreparedStatement stmt = conn.prepareStatement("SELECT u.username FROM \"user\" AS u INNER JOIN "
                + "topic_moderation as t ON t.moderator = u.id WHERE u.username LIKE CONCAT('%', ?, '%') AND "
                + "t.topic = ? LIMIT ?;")) {
            ResultSet rs = new StatementParametrizer(stmt)
                    .string(query)
                    .integer(topic.getId())
                    .integer(limit)
                    .toStatement().executeQuery();

            while (rs.next()) {
                unmodResults.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            log.error("Error while loading the user search suggestions for the query " + query, e);
            throw new StoreException("Error while loading the user search suggestions for the query " + query, e);
        }

        return unmodResults;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getUserSuggestions(final String query, final int limit) {
        validateSuggestionParams(query, limit);
        List<String> userResults = new ArrayList<>(limit);
        try (PreparedStatement stmt = conn.prepareStatement("SELECT distinct u.username FROM \"user\" AS u "
                + "WHERE TRIM(LOWER(u.username)) LIKE CONCAT('%',?,'%') LIMIT ?;")) {
            ResultSet rs = new StatementParametrizer(stmt)
                    .string(query)
                    .integer(limit)
                    .toStatement().executeQuery();

            while (rs.next()) {
                userResults.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            log.error("Error while loading the user search suggestions for the query " + query, e);
            throw new StoreException("Error while loading the user search suggestions for the query " + query, e);
        }

        return userResults;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getTopicSuggestions(final String query, final int limit) {
        validateSuggestionParams(query, limit);
        List<String> topicResults = new ArrayList<>(limit);
        try (PreparedStatement stmt = conn.prepareStatement("SELECT distinct t.title FROM \"topic\" AS t "
                + "WHERE TRIM(LOWER(t.title)) LIKE CONCAT('%',?,'%') LIMIT ?;")) {
            ResultSet rs = new StatementParametrizer(stmt)
                    .string(query)
                    .integer(limit)
                    .toStatement().executeQuery();

            while (rs.next()) {
                topicResults.add(rs.getString("title"));
            }
        } catch (SQLException e) {
            log.error("Error while loading the topic search suggestions for the query " + query, e);
            throw new StoreException("Error while loading the topic search suggestions for the query " + query, e);
        }

        return topicResults;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getReportSuggestions(final String query, final int limit) {
        validateSuggestionParams(query, limit);
        List<String> reportResults = new ArrayList<>(limit);
        try (PreparedStatement stmt = conn.prepareStatement("SELECT distinct t.title FROM \"report\" AS t "
                + "WHERE TRIM(LOWER(t.title)) LIKE CONCAT('%',?,'%') LIMIT ?;")) {
            ResultSet rs = new StatementParametrizer(stmt)
                    .string(query)
                    .integer(limit)
                    .toStatement().executeQuery();

            while (rs.next()) {
                reportResults.add(rs.getString("title"));
            }
        } catch (SQLException e) {
            log.error("Error while loading the report search suggestions for the query " + query, e);
            throw new StoreException("Error while report the topic search suggestions for the query " + query, e);
        }

        return reportResults;
    }

    /**
     * Checks if the given parameters violate restrictions.
     *
     * @param query The query string to search for.
     * @param limit The maximum amount of results to return.
     * @param topic The topic to check.
     * @throws IllegalArgumentException if the given parameters are invalid.
     */
    private void validateSuggestionParams(final String query, final int limit, final Topic topic) {
        if (query == null) {
            log.error("The search query cannot be null!");
            throw new IllegalArgumentException("The search query cannot be null!");
        } else if (query.isBlank()) {
            log.error("The search query cannot be blank!");
            throw new IllegalArgumentException("The search query cannot be blank!");
        } else if (limit < 0) {
            log.error("The limit of search suggestions to return cannot be negative!");
            throw new IllegalArgumentException("The limit of search suggestions to return cannot be negative!");
        } else if (topic.getId() == null) {
            log.error("The topic cannot be null!");
            throw new IllegalArgumentException("The topic cannot be null!");
        }
    }

    /**
     * Checks if the given parameters violate restrictions.
     *
     * @param query The query string to search for.
     * @param limit The maximum amount of results to return.
     * @throws IllegalArgumentException if the given parameters are invalid.
     */
    private void validateSuggestionParams(final String query, final int limit) {
        if (query == null || query.isBlank() || limit < 0) {
            log.error("The query cannot be null or blank!");
            throw new IllegalArgumentException("The query cannot be null or blank!");
        }
    }

    /**
     * Parses the given {@link ResultSet} and returns the corresponding {@link Topic}.
     *
     * @param rs The {@link ResultSet} to parse.
     * @return The parsed {@link Topic}.
     * @throws SQLException Some parsing error occurred.
     */
    static Topic getSearchedTopicFromResultSet(final ResultSet rs) throws SQLException {
        Topic topic = new Topic(rs.getInt("id"), rs.getString("title"),
                rs.getString("description"));
        topic.setNumSub(rs.getInt("num_subscribers"));
        topic.setNumPosts(rs.getInt("num_posts"));
        topic.setLastActivity(rs.getObject("last_activity", OffsetDateTime.class));
        return topic;
    }

    static Report getSearchedReportFromResultSet(final ResultSet rs)
            throws SQLException {
        Report report = ReportDBGateway.getDefaultReportFromResultSet(rs);

        report.setLastActivity(rs.getObject("last_activity", OffsetDateTime.class));
        report.setTopic(rs.getString("t_title"));
        if (rs.getObject("forced_relevance", Integer.class) != null) {
            report.setRelevance(rs.getObject("forced_relevance", Integer.class));
        } else {
            report.setRelevance(rs.getInt("relevance"));
        }

        return report;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Topic> getTopicResults(final String query, final Selection selection) {
        if (selection == null || query == null) {
            log.error("The selection or query cannot be null!");
            throw new IllegalArgumentException("The selection or query cannot be null!");
        } else if (StringUtils.isBlank(selection.getSortedBy())) {
            log.error("Error when trying to get topics sorted by nothing.");
            throw new IllegalArgumentException("The selection needs to have a column to sort by.");
        }

        List<Topic> topicResults = new ArrayList<>(Math.max(0, selection.getTotalSize()));
        try (PreparedStatement stmt = conn.prepareStatement("Select * FROM \"topic\" as t JOIN topic_num_subscribers "
                + "as s "
                + "on s.topic = t.id LEFT OUTER JOIN topic_last_activity as a on t.id = a.topic "
                + "LEFT OUTER JOIN topic_num_posts as p "
                + "on t.id = p.topic WHERE TRIM(LOWER(title)) LIKE CONCAT('%',?,'%') "
                + "ORDER BY " + selection.getSortedBy() + (selection.isAscending() ? " ASC " : " DESC ")
                + "LIMIT ? OFFSET ?;")) {
            ResultSet rs = new StatementParametrizer(stmt)
                    .string(query)
                    .integer(Pagitable.getItemLimit(selection))
                    .integer(Pagitable.getItemOffset(selection))
                    .toStatement().executeQuery();
            while (rs.next()) {
                topicResults.add(getSearchedTopicFromResultSet(rs));
            }
        } catch (SQLException e) {
            log.error("Error while loading the topic search suggestions for the query " + query, e);
            throw new StoreException("Error while loading the topic search suggestions for the query " + query, e);
        }

        return topicResults;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Report> getReportResults(final String query, final Selection selection,
                                         final OffsetDateTime latestOpeningDateTime,
                                         final OffsetDateTime earliestClosingDateTime, final boolean showOpenReports,
                                         final boolean showClosedReports, final boolean showDuplicates,
                                         final boolean fulltext, final String topic,
                                         final Map<Report.Type, Boolean> reportTypeFilter,
                                         final Map<Report.Severity, Boolean> severityFilter) {
        if (selection == null || query == null || severityFilter == null || reportTypeFilter == null) {
            log.error("The selection or query cannot be null!");
            throw new IllegalArgumentException("The selection or query cannot be null!");
        } else if (StringUtils.isBlank(selection.getSortedBy())) {
            log.error("Error when trying to get reports sorted by nothing.");
            throw new IllegalArgumentException("The selection needs to have a column to sort by.");
        }

        List<Report> reportResults = new ArrayList<>(Math.max(0, selection.getTotalSize()));

        if (!reportTypeFilter.get(Report.Type.BUG) && !reportTypeFilter.get(Report.Type.HINT)
                && !reportTypeFilter.get(Report.Type.FEATURE)) {
            return reportResults;
        }
        if (!severityFilter.get(Report.Severity.RELEVANT) && !severityFilter.get(Report.Severity.MINOR)
                && !severityFilter.get(Report.Severity.SEVERE)) {
            return reportResults;
        }
        if (!showClosedReports && !showOpenReports) {
            return reportResults;
        }

        String filter = getFilter("", showOpenReports, showClosedReports, showDuplicates,
                reportTypeFilter, severityFilter);
        String orderBy = selection.getSortedBy();
        if (orderBy.equals("relevance")) {
            orderBy = "COALESCE(forced_relevance, relevance)";
        }
        try (PreparedStatement stmt = conn.prepareStatement("SELECT r.*, t.title as t_title , a.last_activity, "
                + "v.relevance FROM report AS r LEFT OUTER JOIN topic AS t ON r.topic = t.id "
                + "LEFT OUTER JOIN report_last_activity AS a "
                + "ON a.report = r.id LEFT OUTER JOIN report_relevance AS v ON r.id = v.report "
                + "WHERE (TRIM(LOWER(r.title)) LIKE CONCAT('%',?,'%') "
                + "OR (SELECT COUNT(*) FROM post p WHERE ? AND p.report = r.id AND TRIM(LOWER(p.content)) LIKE "
                + "CONCAT('%',?,'%')) > 0) "
                + "AND r.created_at <= COALESCE(?, r.created_at) "
                + "AND (r.closed_at >= COALESCE(?, r.closed_at) OR r.closed_at IS NULL) " + filter + ' '
                + "AND t.title = COALESCE(?, t.title) "
                + "ORDER BY " + orderBy + (selection.isAscending() ? " ASC " : " DESC ")
                + "LIMIT ? OFFSET ?;")) {
            ResultSet rs = new StatementParametrizer(stmt)
                    .string(query)
                    .bool(fulltext)
                    .string(query)
                    .object(latestOpeningDateTime)
                    .object(earliestClosingDateTime)
                    .string(topic)
                    .integer(Pagitable.getItemLimit(selection))
                    .integer(Pagitable.getItemOffset(selection))
                    .toStatement().executeQuery();
            while (rs.next()) {
                reportResults.add(getSearchedReportFromResultSet(rs));
            }
        } catch (SQLException e) {
            log.error("Error while loading the topic search suggestions for the query " + query, e);
            throw new StoreException("Error while loading the topic search suggestions for the query " + query, e);
        }
        return reportResults;
    }

    private String getFilter(final String prefix, final boolean showOpenReports, final boolean showClosedReports,
                             final boolean showDuplicates,
                             final Map<Report.Type, Boolean> reportTypeFilter,
                             final Map<Report.Severity, Boolean> severityFilter) {
        String filterClosedAt = "";
        if (!showClosedReports) {
            filterClosedAt = "AND closed_at IS NULL ";
        } else if (!showOpenReports) {
            filterClosedAt = "AND closed_at IS NOT NULL ";
        }

        String filterDuplicate = "";
        if (!showDuplicates) {
            filterDuplicate = "AND duplicate_of IS NULL ";
        }

        StringBuilder filterTypeBuilder = new StringBuilder();
        filterTypeBuilder.append("AND (");
        boolean filterTypeAdded = false;
        if (reportTypeFilter.get(Report.Type.BUG)) {
            filterTypeBuilder.append(prefix).append("type = 'BUG' ");
            filterTypeAdded = true;
        }
        if (reportTypeFilter.get(Report.Type.FEATURE)) {
            if (filterTypeAdded) {
                filterTypeBuilder.append("OR ");
            }
            filterTypeBuilder.append(prefix).append("type = 'FEATURE' ");
            filterTypeAdded = true;
        }
        if (reportTypeFilter.get(Report.Type.HINT)) {
            if (filterTypeAdded) {
                filterTypeBuilder.append("OR ");
            }
            filterTypeBuilder.append(prefix).append("type = 'HINT' ");
        }
        filterTypeBuilder.append(") ");
        String filterType = filterTypeBuilder.toString();

        StringBuilder filterSeverityBuilder = new StringBuilder();
        filterSeverityBuilder.append("AND (");
        boolean filterSeverityAdded = false;
        if (severityFilter.get(Report.Severity.MINOR)) {
            filterSeverityBuilder.append(prefix).append("severity = 'MINOR' ");
            filterSeverityAdded = true;
        }
        if (severityFilter.get(Report.Severity.RELEVANT)) {
            if (filterSeverityAdded) {
                filterSeverityBuilder.append("OR ");
            }
            filterSeverityBuilder.append(prefix).append("severity = 'RELEVANT' ");
            filterSeverityAdded = true;
        }
        if (severityFilter.get(Report.Severity.SEVERE)) {
            if (filterSeverityAdded) {
                filterSeverityBuilder.append("OR ");
            }
            filterSeverityBuilder.append(prefix).append("severity = 'SEVERE' ");
        }
        filterSeverityBuilder.append(") ");
        String filterSeverity = filterSeverityBuilder.toString();

        return filterClosedAt + filterDuplicate + filterType + filterSeverity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfUserResults(final String query, final boolean showAdmins, final boolean showNonAdmins) {
        if (query == null) {
            log.error("The selection or query cannot be null!");
            throw new IllegalArgumentException("The selection or query cannot be null!");
        }

        int users = 0;

        if (!showAdmins && !showNonAdmins) {
            return 0;
        }
        String adminFilter = "";
        if (showAdmins) {
            adminFilter = "AND is_admin = true";
        }
        if (showNonAdmins) {
            if (showAdmins) {
                adminFilter = "";
            } else {
                adminFilter = "AND is_admin = false";
            }
        }
        try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) AS num_users FROM \"user\" "
                + "WHERE TRIM(LOWER(username)) LIKE CONCAT('%',?,'%') " + adminFilter)) {
            ResultSet rs = new StatementParametrizer(stmt)
                    .string(query)
                    .toStatement().executeQuery();

            while (rs.next()) {
                users = rs.getInt("num_users");
            }
        } catch (SQLException e) {
            log.error("Error while loading the user search suggestions for the query " + query, e);
            throw new StoreException("Error while loading the user search suggestions for the query " + query, e);
        }

        return users;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfTopicResults(final String query) {
        if (query == null) {
            log.error("The selection or query cannot be null!");
            throw new IllegalArgumentException("The selection or query cannot be null!");
        }

        int topics = 0;

        try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) AS num_topics FROM topic "
                + "WHERE TRIM(LOWER(title)) LIKE CONCAT('%',?,'%');")) {
            ResultSet rs = new StatementParametrizer(stmt)
                    .string(query)
                    .toStatement().executeQuery();
            while (rs.next()) {
                topics = rs.getInt("num_topics");
            }
        } catch (SQLException e) {
            log.error("Error while loading the topic search suggestions for the query " + query, e);
            throw new StoreException("Error while loading the topic search suggestions for the query " + query, e);
        }

        return topics;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfReportResults(final String query, final OffsetDateTime latestOpeningDateTime,
                                        final OffsetDateTime earliestClosingDateTime, final boolean showOpenReports,
                                        final boolean showClosedReports, final boolean showDuplicates,
                                        final boolean fulltext, final String topic,
                                        final Map<Report.Type, Boolean> reportTypeFilter,
                                        final Map<Report.Severity, Boolean> severityFilter) {
        if (query == null || severityFilter == null || reportTypeFilter == null) {
            log.error("The query cannot be null!");
            throw new IllegalArgumentException("The query cannot be null!");
        }

        if (!reportTypeFilter.get(Report.Type.BUG) && !reportTypeFilter.get(Report.Type.HINT)
                && !reportTypeFilter.get(Report.Type.FEATURE)) {
            return 0;
        }
        if (!severityFilter.get(Report.Severity.RELEVANT) && !severityFilter.get(Report.Severity.MINOR)
                && !severityFilter.get(Report.Severity.SEVERE)) {
            return 0;
        }
        if (!showClosedReports && !showOpenReports) {
            return 0;
        }

        int reports = 0;
        String filter = getFilter("r.", showOpenReports, showClosedReports, showDuplicates,
                reportTypeFilter, severityFilter);

        try (PreparedStatement stmt = conn.prepareStatement("SELECT Count(*) AS num_reports FROM \"report\" AS r "
                + "JOIN topic AS t "
                + "ON r.topic = t.id WHERE (TRIM(LOWER(r.title)) LIKE CONCAT('%',?,'%') "
                + "OR (SELECT COUNT(*) FROM post p WHERE ? AND p.report = r.id AND TRIM(LOWER(p.content)) LIKE "
                + "CONCAT('%',?,'%')) > 0) "
                + "AND r.created_at <= COALESCE(?, r.created_at) "
                + "AND (r.closed_at >= COALESCE(?, r.closed_at) OR r.closed_at IS NULL) " + filter + ' '
                + "AND t.title = COALESCE(?, t.title);")) {
            ResultSet rs = new StatementParametrizer(stmt)
                    .string(query)
                    .bool(fulltext)
                    .string(query)
                    .object(latestOpeningDateTime)
                    .object(earliestClosingDateTime)
                    .string(topic)
                    .toStatement().executeQuery();
            while (rs.next()) {
                reports = rs.getInt("num_reports");
            }
        } catch (SQLException e) {
            log.error("Error while loading the topic search suggestions for the query " + query, e);
            throw new StoreException("Error while loading the topic search suggestions for the query " + query, e);
        }

        return reports;
    }

}
