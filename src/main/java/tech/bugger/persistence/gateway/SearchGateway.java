package tech.bugger.persistence.gateway;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import tech.bugger.global.transfer.Report;
import tech.bugger.global.transfer.Selection;
import tech.bugger.global.transfer.Topic;
import tech.bugger.global.transfer.User;
import tech.bugger.persistence.exception.NotFoundException;

/**
 * A search gateway allows to make search requests to a persistent storage that stores application data.
 */
public interface SearchGateway {

    /**
     * Searches for users by their username and filters the results according to given selection criteria.
     *
     * @param query         The search string to use.
     * @param selection     The pagination filters to apply.
     * @param showAdmins    Whether to include administrators.
     * @param showNonAdmins Whether to include non-administrators.
     * @return The list of users that match the search criteria.
     */
    List<User> getUserResults(String query, Selection selection, boolean showAdmins, boolean showNonAdmins);

    /**
     * Searches for users by their username and filters the results according to given selection criteria.
     *
     * @param query The search string to use.
     * @param limit The maximum amount of usernames to return.
     * @param topic The topic for which the ban suggestions are to be rendered.
     * @return The list of usernames that match the search criteria.
     */
    List<String> getUserBanSuggestions(String query, int limit, Topic topic);

    /**
     * Searches for users by their username and filters the results according to given selection criteria.
     *
     * @param query The search string to use.
     * @param limit The maximum amount of usernames to return.
     * @param topic The topic for which the unban suggestions are to be rendered.
     * @return The list of usernames that match the search criteria.
     */
    List<String> getUserUnbanSuggestions(String query, int limit, Topic topic);

    /**
     * Searches for users by their username and filters the results according to given selection criteria.
     *
     * @param query The search string to use.
     * @param limit The maximum amount of usernames to return.
     * @param topic The topic for which the moderation suggestions are to be rendered.
     * @return The list of usernames that match the search criteria.
     */
    List<String> getUserModSuggestions(String query, int limit, Topic topic);

    /**
     * Searches for users by their username and filters the results according to given selection criteria.
     *
     * @param query The search string to use.
     * @param limit The maximum amount of usernames to return.
     * @param topic The topic for which the demote moderator suggestions are to be rendered.
     * @return The list of usernames that match the search criteria.
     */
    List<String> getUserUnmodSuggestions(String query, int limit, Topic topic);

    /**
     * Searches for users by their username and filters the results according to given selection criteria.
     *
     * @param query The search string to use.
     * @param limit The maximum amount of usernames to return.
     * @return The list of usernames that match the search criteria.
     */
    List<String> getUserSuggestions(String query, int limit);

    /**
     * Searches for topics by their title and filters the results according to given selection criteria.
     *
     * @param query The search string to use.
     * @param limit The maximum amount of topic titles to return.
     * @return The list of titles that match the search criteria.
     */
    List<String> getTopicSuggestions(String query, int limit);

    /**
     * Searches for reports by their title and filters the results according to given selection criteria.
     *
     * @param query The search string to use.
     * @param limit The maximum amount of report titles to return.
     * @return The list of titles that match the search criteria.
     */
    List<String> getReportSuggestions(String query, int limit);

    /**
     * Searches for topics by their title and returns the requested result page.
     *
     * @param query     The search string to use.
     * @param selection The pagination filters to apply.
     * @return The list of topics that match the search criteria.
     */
    List<Topic> getTopicResults(String query, Selection selection);

    /**
     * Searches for reports by their title and filters the results according to given selection criteria.
     *
     * @param query                   The search string to use.
     * @param selection               The pagination filters to apply.
     * @param latestOpeningDateTime   The date and time before which the search results must have been opened.
     * @param earliestClosingDateTime The date and time after which closed search results must have been closed.
     * @param showOpenReports         Whether to include open reports.
     * @param showClosedReports       Whether to include closed reports.
     * @param showDuplicates          Whether to include reports that were marked as a duplicate of another report.
     * @param fulltext                Whether or not to enable fulltext search in postings.
     * @param topic                   The topic the search results have to belong to. Can be {@code null} to search in
     *                                all topics.
     * @param reportTypeFilter        Map that indicates for each report type whether to include or exclude reports of
     *                                this type.
     * @param severityFilter          Map that indicates for each degree of severity whether to include or exclude
     *                                reports of this severity.
     * @return The list of reports that match the search criteria.
     * @throws NotFoundException The topic could not be found.
     */
    List<Report> getReportResults(String query, Selection selection, OffsetDateTime latestOpeningDateTime,
                                  OffsetDateTime earliestClosingDateTime, boolean showOpenReports,
                                  boolean showClosedReports, boolean showDuplicates, boolean fulltext, String topic,
                                  Map<Report.Type, Boolean> reportTypeFilter,
                                  Map<Report.Severity, Boolean> severityFilter) throws NotFoundException;

    /**
     * Searches for users by their username, filters the results according to given selection criteria, and returns the
     * number of results.
     *
     * @param query         The search string to use.
     * @param showAdmins    Whether to include administrators.
     * @param showNonAdmins Whether to include non-administrators.
     * @return The number of users that match the search criteria.
     */
    int getNumberOfUserResults(String query, boolean showAdmins, boolean showNonAdmins);

    /**
     * Searches for topics by their title and returns the number of results.
     *
     * @param query The search string to use.
     * @return The number of topics that match the search criteria.
     */
    int getNumberOfTopicResults(String query);

    /**
     * Searches for reports by their title, filters the results according to given selection criteria and returns the
     * number of results.
     *
     * @param query                   The search string to use.
     * @param latestOpeningDateTime   The date and time before which the search results must have been opened.
     * @param earliestClosingDateTime The date and time after which closed search results must have been closed.
     * @param showOpenReports         Whether to include open reports.
     * @param showClosedReports       Whether to include closed reports.
     * @param showDuplicates          Whether to include reports that were marked as a duplicate of another report.
     * @param fulltext                Whether or not to enable fulltext search in postings.
     * @param topic                   The topic the search results have to belong to. Can be {@code null} to search in
     *                                all topics.
     * @param reportTypeFilter        Map that indicates for each report type whether to include or exclude reports of
     *                                this type.
     * @param severityFilter          Map that indicates for each degree of severity whether to include or exclude
     *                                reports of this severity.
     * @return The number of reports that match the search criteria.
     * @throws NotFoundException The topic could not be found.
     */
    int getNumberOfReportResults(String query, OffsetDateTime latestOpeningDateTime,
                                 OffsetDateTime earliestClosingDateTime, boolean showOpenReports,
                                 boolean showClosedReports, boolean showDuplicates, boolean fulltext, String topic,
                                 Map<Report.Type, Boolean> reportTypeFilter,
                                 Map<Report.Severity, Boolean> severityFilter) throws NotFoundException;

}
