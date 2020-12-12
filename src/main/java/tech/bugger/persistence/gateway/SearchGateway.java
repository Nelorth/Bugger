package tech.bugger.persistence.gateway;

import tech.bugger.global.transfer.Report;
import tech.bugger.global.transfer.Selection;
import tech.bugger.global.transfer.Topic;
import tech.bugger.global.transfer.User;
import tech.bugger.persistence.exception.NotFoundException;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;

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
    public List<User> getUserResults(String query, Selection selection, boolean showAdmins, boolean showNonAdmins);

    /**
     * Searches for topics by their title and returns the requested result page.
     *
     * @param query     The search string to use.
     * @param selection The pagination filters to apply.
     * @return The list of topics that match the search criteria.
     */
    public List<Topic> getTopicResults(String query, Selection selection);

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
     * @param topic                   The topic the search results have to belong to. Can be {@code null} to search in
     *                                all topics.
     * @param reportTypeFilter        Map that indicates for each report type whether to include or exclude reports of
     *                                this type.
     * @param severityFilter          Map that indicates for each degree of severity whether to include or exclude
     *                                reports of this severity.
     * @return The list of reports that match the search criteria.
     * @throws NotFoundException The topic could not be found.
     */
    public List<Report> getReportResults(String query, Selection selection, ZonedDateTime latestOpeningDateTime,
                                         ZonedDateTime earliestClosingDateTime, boolean showOpenReports,
                                         boolean showClosedReports, boolean showDuplicates, Topic topic,
                                         HashMap<Report.Type, Boolean> reportTypeFilter,
                                         HashMap<Report.Severity, Boolean> severityFilter) throws NotFoundException;

    /**
     * Searches for reports by the contents of their posts and filters the results according to given selection
     * criteria.
     *
     * @param query                   The search string to use.
     * @param selection               The pagination filters to apply.
     * @param latestOpeningDateTime   The date and time before which the search results must have been opened.
     * @param earliestClosingDateTime The date and time after which closed search results must have been closed.
     * @param showOpenReports         Whether to show open reports.
     * @param showClosedReports       Whether to show closed reports.
     * @param showDuplicates          Whether to show reports that were marked as a duplicate of another report.
     * @param topic                   The topic the search results have to belong to. Can be {@code null} to search in
     *                                all topics.
     * @param reportTypeFilter        Map that indicates for each report type whether to include or exclude reports of
     *                                this type.
     * @param severityFilter          Map that indicates for each degree of severity whether to include or exclude
     *                                reports of this severity.
     * @return The list of reports that match the search criteria.
     * @throws NotFoundException The topic could not be found.
     */
    public List<Report> getFulltextResults(String query, Selection selection, ZonedDateTime latestOpeningDateTime,
                                           ZonedDateTime earliestClosingDateTime, boolean showOpenReports,
                                           boolean showClosedReports, boolean showDuplicates, Topic topic,
                                           HashMap<Report.Type, Boolean> reportTypeFilter,
                                           HashMap<Report.Severity, Boolean> severityFilter) throws NotFoundException;

    /**
     * Searches for users by their username, filters the results according to given selection criteria, and returns the
     * number of results.
     *
     * @param query         The search string to use.
     * @param showAdmins    Whether to include administrators.
     * @param showNonAdmins Whether to include non-administrators.
     * @return The number of users that match the search criteria.
     */
    public int getNumberOfUserResults(String query, boolean showAdmins, boolean showNonAdmins);

    /**
     * Searches for topics by their title and returns the number of results.
     *
     * @param query     The search string to use.
     * @return The number of topics that match the search criteria.
     */
    public int getNumberOfTopicResults(String query);

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
     * @param topic                   The topic the search results have to belong to. Can be {@code null} to search in
     *                                all topics.
     * @param reportTypeFilter        Map that indicates for each report type whether to include or exclude reports of
     *                                this type.
     * @param severityFilter          Map that indicates for each degree of severity whether to include or exclude
     *                                reports of this severity.
     * @return The number of reports that match the search criteria.
     * @throws NotFoundException The topic could not be found.
     */
    public int getNumberOfReportResults(String query, ZonedDateTime latestOpeningDateTime,
                                        ZonedDateTime earliestClosingDateTime, boolean showOpenReports,
                                        boolean showClosedReports,
                                        boolean showDuplicates, Topic topic,
                                        HashMap<Report.Type, Boolean> reportTypeFilter,
                                        HashMap<Report.Severity, Boolean> severityFilter) throws NotFoundException;

    /**
     * Searches for reports by the contents of their posts, filters the results according to given selection criteria
     * and returns the number of results.
     *
     * @param query                   The search string to use.
     * @param latestOpeningDateTime   The date and time before which the search results must have been opened.
     * @param earliestClosingDateTime The date and time after which closed search results must have been closed.
     * @param showOpenReports         Whether to include open reports.
     * @param showClosedReports       Whether to include closed reports.
     * @param showDuplicates          Whether to include reports that were marked as a duplicate of another report.
     * @param topic                   The topic the search results have to belong to. Can be {@code null} to search in
     *                                all topics.
     * @param reportTypeFilter        Map that indicates for each report type whether to include or exclude reports of
     *                                this type.
     * @param severityFilter          Map that indicates for each degree of severity whether to include or exclude
     *                                reports of this severity.
     * @return The number of reports that match the search criteria.
     * @throws NotFoundException The topic could not be found.
     */
    public int getNumberOfFulltextResults(String query, ZonedDateTime latestOpeningDateTime,
                                          ZonedDateTime earliestClosingDateTime, boolean showOpenReports,
                                          boolean showClosedReports,
                                          boolean showDuplicates, Topic topic, HashMap<Report.Type, Boolean> reportTypeFilter,
                                          HashMap<Report.Severity, Boolean> severityFilter) throws NotFoundException;

}