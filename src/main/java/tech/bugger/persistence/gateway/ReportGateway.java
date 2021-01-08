package tech.bugger.persistence.gateway;

import tech.bugger.global.transfer.Report;
import tech.bugger.global.transfer.Selection;
import tech.bugger.global.transfer.Topic;
import tech.bugger.global.transfer.User;
import tech.bugger.persistence.exception.DuplicateException;
import tech.bugger.persistence.exception.NotFoundException;
import tech.bugger.persistence.exception.SelfReferenceException;

import java.util.List;
import java.util.Optional;

/**
 * A report gateway allows to query and modify a persistent storage of reports.
 */
public interface ReportGateway {

    /**
     * Looks up the number of posts of a given report.
     *
     * @param report The report whose posts to count.
     * @return The number of posts of the report.
     * @throws NotFoundException The report could not be found.
     */
    public int getNumberOfPosts(Report report) throws NotFoundException;

    /**
     * Retrieves a report by its ID.
     *
     * @param id The ID of the report to look for.
     * @return The report identified by the ID.
     * @throws NotFoundException The report could not be found.
     */
    public Report find(int id) throws NotFoundException;

    /**
     * Retrieves a list of reports of a topic that match the given selection criteria, including or excluding open or
     * closed reports.
     *
     * @param topic             The topic whose reports to look for.
     * @param selection         The search criteria to apply.
     * @param showOpenReports   Whether to include open reports in the list.
     * @param showClosedReports Whether to include closed reports in the list.
     * @return The list of reports of {@code topic}, filtered accordingly.
     * @throws NotFoundException The topic could not be found.
     */
    public List<Report> getSelectedReports(Topic topic, Selection selection, boolean showOpenReports,
                                           boolean showClosedReports) throws NotFoundException;

    /**
     * Inserts a report into the report storage.
     *
     * Sets the ID of {@ode report} that was assigned upon insertion by the report storage.
     *
     * @param report The report to insert.
     */
    public void create(Report report);

    /**
     * Updates a report's attributes in the report storage.
     *
     * @param report The report to update.
     * @throws NotFoundException The report could not be found.
     */
    public void update(Report report) throws NotFoundException;

    /**
     * Deletes a report from the report storage.
     *
     * @param report The report to delete.
     * @throws NotFoundException The report could not be found.
     */
    public void delete(Report report) throws NotFoundException;

    /**
     * Closes a report in the report storage.
     *
     * @param report The report to close.
     * @throws NotFoundException The report could not be found.
     */
    public void closeReport(Report report) throws NotFoundException;

    /**
     * Opens a report in the report storage.
     *
     * @param report The report to open.
     * @throws NotFoundException The report could not be found.
     */
    public void openReport(Report report) throws NotFoundException;

    /**
     * Marks a report as a duplicate of another report.
     *
     * @param duplicate  The report to mark as a duplicate.
     * @param originalID The ID of the report that {@code duplicate} is to be marked a duplicate of. Must be different
     *                   from {@code duplicate}'s ID.
     * @throws NotFoundException      The duplicate or the original could not be found.
     * @throws SelfReferenceException The ID of {@code duplicate} is the same as {@code originalID}.
     */
    public void markDuplicate(Report duplicate, int originalID) throws NotFoundException, SelfReferenceException;

    /**
     * Removes the mark of duplication from a report.
     *
     * @param report The report to unmark as duplicate.
     * @throws NotFoundException The report could not be found.
     */
    public void unmarkDuplicate(Report report) throws NotFoundException;

    /**
     * Overwrites the calculated relevance of a report by a fixed relevance value or removes the override, restoring the
     * calculated relevance.
     *
     * @param report    The report whose relevance to overwrite.
     * @param relevance An {@link Optional} that indicates, the new overwriting relevance if present and the removal of
     *                  the override if empty.
     * @throws NotFoundException The report could not be found.
     */
    public void overwriteRelevance(Report report, Optional<Integer> relevance) throws NotFoundException;

    /**
     * Stores an upvote, i.e. a positive vote for the relevance, of a report by a user.
     *
     * @param report The report to upvote.
     * @param user   The upvoting user.
     * @throws DuplicateException A vote on the report by the user already existed.
     * @throws NotFoundException  The report or the user could not be found.
     */
    public void upvote(Report report, User user) throws DuplicateException, NotFoundException;

    /**
     * Stores an downvote, i.e. a negative vote for the relevance, of a report by a user.
     *
     * @param report The report to downvote.
     * @param user   The downvoting user.
     * @throws DuplicateException A vote on the report by the user already existed.
     * @throws NotFoundException  The report or the user could not be found.
     */
    public void downvote(Report report, User user) throws DuplicateException, NotFoundException;

    /**
     * Removes a user's vote on a report.
     *
     * @param report The report to remove the vote from.
     * @param user   The user who gave the vote.
     * @throws NotFoundException The report, the user, or the vote could not be found.
     */
    public void removeVote(Report report, User user) throws NotFoundException;
}
