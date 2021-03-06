package tech.bugger.persistence.gateway;

import com.ocpsoft.pretty.faces.util.StringUtils;
import tech.bugger.global.transfer.Selection;
import tech.bugger.global.transfer.Topic;
import tech.bugger.global.transfer.User;
import tech.bugger.global.util.Log;
import tech.bugger.global.util.Pagitable;
import tech.bugger.persistence.exception.DuplicateException;
import tech.bugger.persistence.exception.NotFoundException;
import tech.bugger.persistence.exception.StoreException;
import tech.bugger.persistence.util.StatementParametrizer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Topic gateway that gives access to topics stored in a database.
 */
public class TopicDBGateway implements TopicGateway {

    /**
     * The {@link Log} instance associated with this class for logging purposes.
     */
    private static final Log log = Log.forClass(TopicDBGateway.class);

    /**
     * Database connection used by this gateway.
     */
    private final Connection conn;

    /**
     * Constructs a new topic gateway with the given database connection.
     *
     * @param conn The database connection to use for the gateway.
     */
    public TopicDBGateway(final Connection conn) {
        this.conn = conn;
    }

    /**
     * Parses the given {@link ResultSet} and returns the corresponding {@link Topic}.
     *
     * @param rs The {@link ResultSet} to parse.
     * @return The parsed {@link Topic}.
     * @throws SQLException Some parsing error occurred.
     */
    static Topic getTopicFromResultSet(final ResultSet rs) throws SQLException {
        return new Topic(rs.getInt("id"), rs.getString("title"),
                rs.getString("description"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countReports(final Topic topic, final boolean showOpenReports, final boolean showClosedReports) {
        if (!showOpenReports && !showClosedReports) {
            return 0;
        }

        int numberOfReports = 0;
        String sql = "SELECT COUNT(*) FROM report WHERE topic = ?";
        if (showOpenReports && !showClosedReports) {
            sql += " AND closed_at IS NULL";
        } else if (!showOpenReports) {
            sql += " AND closed_at IS NOT NULL";
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet resultSet = new StatementParametrizer(stmt)
                    .integer(topic.getId()).toStatement().executeQuery();
            int numReports;
            if (resultSet.next()) {
                numReports = resultSet.getInt(1);
                numberOfReports += numReports;
            }
        } catch (SQLException e) {
            log.error("Error while searching for report by topic.", e);
            throw new StoreException("Error while searching for report by topic.", e);
        }

        return numberOfReports;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countTopics() {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM topic;")) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                throw new InternalError("Could not count the number of topics.");
            }
        } catch (SQLException e) {
            log.error("Error while retrieving number of topics.", e);
            throw new StoreException("Error while retrieving number of topics.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countModerators(final Topic topic) {
        if (topic.getId() == null) {
            log.error("The topic ID cannot be null!.");
            throw new IllegalArgumentException("The topic ID cannot be null!.");
        }

        int moderators = 0;

        try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(t.moderator) AS num_mods FROM "
                + "topic_moderation AS t WHERE t.topic = ?;")) {
            ResultSet rs = new StatementParametrizer(stmt)
                    .integer(topic.getId())
                    .toStatement().executeQuery();

            if (rs.next()) {
                moderators = rs.getInt("num_mods");
            }
        } catch (SQLException e) {
            log.error("Error while counting the number of moderators for the topic with id " + topic.getId(), e);
            throw new StoreException("Error while counting the number of moderators for the topic with id "
                    + topic.getId(), e);
        }

        return moderators;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countBannedUsers(final Topic topic) {
        if (topic.getId() == null) {
            log.error("The topic ID cannot be null!.");
            throw new IllegalArgumentException("The topic ID cannot be null!.");
        }

        int banned = 0;

        try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(t.outcast) AS num_mods FROM "
                + "topic_ban AS t WHERE t.topic = ?;")) {
            ResultSet rs = new StatementParametrizer(stmt)
                    .integer(topic.getId())
                    .toStatement().executeQuery();

            if (rs.next()) {
                banned = rs.getInt("num_mods");
            }

        } catch (SQLException e) {
            log.error("Error while counting the banned users for the topic with id " + topic.getId(), e);
            throw new StoreException("Error while counting the banned users for the topic with id " + topic.getId(), e);
        }

        return banned;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Topic findTopic(final int id) throws NotFoundException {
        Topic topic;

        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM topic WHERE id = ?")) {
            ResultSet rs = new StatementParametrizer(stmt).integer(id).toStatement().executeQuery();

            if (rs.next()) {
                topic = getTopicFromResultSet(rs);
            } else {
                log.error("No topic with id " + id + " could be found in the database");
                throw new NotFoundException("No topic with id " + id + " could be found in the database.");
            }
        } catch (SQLException e) {
            log.error("Error while searching for user with id " + id, e);
            throw new StoreException("Error while searching for user with id " + id, e);
        }

        return topic;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Topic> selectTopics(final Selection selection) {
        if (selection == null) {
            log.error("Error when trying to get topics with selection null.");
            throw new IllegalArgumentException("Selection cannot be null.");
        } else if (StringUtils.isBlank(selection.getSortedBy())) {
            log.error("Error when trying to get topics sorted by nothing.");
            throw new IllegalArgumentException("Cannot sort by nothing.");
        }

        String sql = "SELECT t.*, l.last_activity FROM topic AS t"
                + " LEFT OUTER JOIN topic_last_activity AS l ON t.id = l.topic"
                + " ORDER BY " + selection.getSortedBy() + (selection.isAscending() ? " ASC" : " DESC")
                + " LIMIT " + Pagitable.getItemLimit(selection)
                + " OFFSET " + Pagitable.getItemOffset(selection) + ";";

        List<Topic> selectedTopics = new ArrayList<>(Math.max(0, selection.getTotalSize()));
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            readTopicsFromResultSet(stmt.executeQuery(), selectedTopics);
        } catch (SQLException e) {
            log.error("Error while retrieving topics with " + selection + ".", e);
            throw new StoreException("Error while retrieving topics with " + selection + ".", e);
        }

        return selectedTopics;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createTopic(final Topic topic) throws DuplicateException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO topic (title, description)"
                        + "VALUES (?, ?);",
                PreparedStatement.RETURN_GENERATED_KEYS
        )) {
            PreparedStatement statement = new StatementParametrizer(stmt)
                    .string(topic.getTitle())
                    .string(topic.getDescription())
                    .toStatement();
            statement.executeUpdate();
            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                topic.setId(generatedKeys.getInt("id"));
            } else {
                log.error("Error while retrieving new topic ID.");
                throw new StoreException("Error while retrieving new topic ID.");
            }
        } catch (SQLException e) {
            // The SQLState 23505 signifies that a violation of a unique index or unique constraint occured.
            if (e.getSQLState().equals("23505")) {
                log.warning("The topic with name " + topic.getTitle() + " already exists.");
                throw new DuplicateException("The topic with name " + topic.getTitle() + " already exists.");
            }

            log.error("Error while creating topic.", e);
            throw new StoreException("Error while creating topic.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateTopic(final Topic topic) throws NotFoundException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE topic "
                        + "SET title = ?, description = ?"
                        + "WHERE id = ?;"
        )) {
            int rowsAffected = new StatementParametrizer(stmt)
                    .string(topic.getTitle())
                    .string(topic.getDescription())
                    .integer(topic.getId())
                    .toStatement().executeUpdate();
            if (rowsAffected == 0) {
                throw new NotFoundException("Topic to be updated could not be found.");
            }
        } catch (SQLException e) {
            throw new StoreException("Error while updating report.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteTopic(final Topic topic) {
        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM \"topic\" WHERE id = ?;")) {
            new StatementParametrizer(stmt)
                    .integer(topic.getId()).toStatement().executeUpdate();
        } catch (SQLException e) {
            log.error("Error while deleting the topic with id " + topic.getId(), e);
            throw new StoreException("Error while deleting the topic with id " + topic.getId(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void banUser(final Topic topic, final User user) throws NotFoundException {
        if (user.getId() == null || topic.getId() == null) {
            log.error("The user or topic ID cannot be null!");
            throw new IllegalArgumentException("The user or topic ID cannot be null!");
        }

        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO topic_ban (outcast, topic) "
                + "VALUES (?, ?);")) {
            new StatementParametrizer(stmt)
                    .integer(user.getId())
                    .integer(topic.getId())
                    .toStatement().executeUpdate();
        } catch (SQLException e) {
            // The SQLState 23503 signifies that the insert or update value of a foreign key is invalid.
            if (e.getSQLState().equals("23503")) {
                log.error("No user with id " + user.getId() + " or topic with id " + topic.getId() + " could be "
                        + "found in the database.");
                throw new NotFoundException("No user with id " + user.getId() + " or topic with id " + topic.getId()
                        + " could be found in the database.");
            }

            log.error("Error while trying to ban the user with id " + user.getId() + " from the topic with id "
                    + topic.getId(), e);
            throw new StoreException("Error while trying to ban the user with id " + user.getId() + " from the topic "
                    + "with id " + topic.getId(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unbanUser(final Topic topic, final User user) throws NotFoundException {
        if (user.getId() == null || topic.getId() == null) {
            throw new IllegalArgumentException("The user or topic ID cannot be null!");
        }

        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM topic_ban WHERE topic = ? AND "
                + "outcast = ?;")) {
            int modified = new StatementParametrizer(stmt)
                    .integer(topic.getId())
                    .integer(user.getId())
                    .toStatement().executeUpdate();

            if (modified == 0) {
                log.warning("No user with id " + user.getId() + " is banned from the topic with id "
                        + topic.getId());
                throw new NotFoundException("No user with id " + user.getId() + " is banned from the topic with id "
                        + topic.getId());
            }
        } catch (SQLException e) {
            log.error("Error while trying to unban the user with id " + user.getId() + " for the topic with id "
                    + topic.getId(), e);
            throw new StoreException("Error while trying to unban the user with id " + user.getId() + " for the topic "
                    + "with id " + topic.getId(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void promoteModerator(final Topic topic, final User user) throws NotFoundException {
        if (user.getId() == null || topic.getId() == null) {
            log.error("The user or topic ID cannot be null!");
            throw new IllegalArgumentException("The user or topic ID cannot be null!");
        }

        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO topic_moderation (moderator, topic) "
                + "VALUES (?, ?);")) {
            new StatementParametrizer(stmt)
                    .integer(user.getId())
                    .integer(topic.getId())
                    .toStatement().executeUpdate();
        } catch (SQLException e) {
            // The SQLState 23503 signifies that the insert or update value of a foreign key is invalid.
            if (e.getSQLState().equals("23503")) {
                log.error("No user with id " + user.getId() + " or topic with id " + topic.getId() + " could be "
                        + "found in the database.");
                throw new NotFoundException("No user with id " + user.getId() + " or topic with id " + topic.getId()
                        + " could be found in the database.");
            }

            log.error("Error while trying to promote the user with id " + user.getId() + " as a moderator for the "
                    + "topic with id " + topic.getId(), e);
            throw new StoreException("Error while trying to promote the user with id " + user.getId() + " as a "
                    + "moderator for the topic with id " + topic.getId(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void demoteModerator(final Topic topic, final User user) throws NotFoundException {
        if (user.getId() == null || topic.getId() == null) {
            throw new IllegalArgumentException("The user or topic ID cannot be null!");
        }

        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM topic_moderation WHERE topic = ? AND "
                + "moderator = ?;")) {
            int modified = new StatementParametrizer(stmt)
                    .integer(topic.getId())
                    .integer(user.getId())
                    .toStatement().executeUpdate();

            if (modified == 0) {
                log.warning("No moderator with the user id " + user.getId() + " could be found for the topic with "
                        + "id " + topic.getId());
                throw new NotFoundException("No moderator with the user id " + user.getId() + " could be found for the "
                        + "topic with id " + topic.getId());
            }
        } catch (SQLException e) {
            log.error("Error while trying to demote the user with id " + user.getId() + " as a moderator for the "
                    + "topic with id " + topic.getId(), e);
            throw new StoreException("Error while trying to demote the user with id " + user.getId() + " as a moderator"
                    + " for the topic with id " + topic.getId(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OffsetDateTime determineLastActivity(final Topic topic) throws NotFoundException {
        if (topic == null) {
            log.error("Error when trying to determine last activity in topic null.");
            throw new IllegalArgumentException("Topic must not be null!");
        } else if (topic.getId() == null) {
            log.error("Error when trying to determine last activity in topic with ID null.");
            throw new IllegalArgumentException("Topic ID must not be null!");
        }

        OffsetDateTime lastActivity;
        String sql = "SELECT * FROM topic_last_activity WHERE topic =" + topic.getId();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                lastActivity = rs.getObject("last_activity", OffsetDateTime.class);
            } else {
                log.error("Topic " + topic + " could not be found when trying to determine last activity.");
                throw new NotFoundException("Topic " + topic
                        + " could not be found when trying to determine last activity.");
            }
        } catch (SQLException e) {
            log.error("Error while determining last activity in topic " + topic, e);
            throw new StoreException("Error while determining last activity in topic " + topic, e);
        }

        return lastActivity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countSubscribers(final Topic topic) {
        if (topic == null) {
            log.error("Cannot determine number of subscribers of topic null.");
            throw new IllegalArgumentException("Topic cannot be null.");
        } else if (topic.getId() == null) {
            log.error("Cannot determine number of subscribers of topic with ID null.");
            throw new IllegalArgumentException("Topic ID cannot be null.");
        }

        int count = 0;
        String sql = "SELECT COUNT(*) AS count FROM topic_subscription WHERE topic = ?;";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            PreparedStatement statement = new StatementParametrizer(stmt)
                    .integer(topic.getId()).toStatement();
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                count = rs.getInt("count");
            }
        } catch (SQLException e) {
            log.error("Error when counting subscribers of topic " + topic + ".", e);
            throw new StoreException("Error when counting subscribers of topic " + topic + ".", e);
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Topic> discoverTopics() {
        List<Topic> topics = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM topic ORDER BY title;")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                topics.add(getTopicFromResultSet(rs));
            }
        } catch (SQLException e) {
            log.error("Error while discovering all topics.", e);
            throw new StoreException("Error while discovering all topics.", e);
        }
        return topics;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Topic> getModeratedTopics(final User user, final Selection selection) {
        if (selection == null || user.getId() == null) {
            log.error("The selection or user ID cannot be null!.");
            throw new IllegalArgumentException("The selection or user ID cannot be null!.");
        } else if (StringUtils.isBlank(selection.getSortedBy())) {
            log.error("Error when trying to get topics sorted by nothing.");
            throw new IllegalArgumentException("The selection needs to have a column to sort by.");
        }

        List<Topic> moderatedTopics = new ArrayList<>(Math.max(0, selection.getTotalSize()));
        try (PreparedStatement stmt = conn.prepareStatement("SELECT t.* FROM topic AS t, topic_moderation AS m "
                + "WHERE t.id = m.topic AND m.moderator = ? ORDER BY t.title ASC LIMIT ? OFFSET ?;")) {
            ResultSet rs = new StatementParametrizer(stmt)
                    .integer(user.getId())
                    .integer(Pagitable.getItemLimit(selection))
                    .integer(Pagitable.getItemOffset(selection))
                    .toStatement().executeQuery();

            while (rs.next()) {
                moderatedTopics.add(getTopicFromResultSet(rs));
            }
        } catch (SQLException e) {
            log.error("Error while trying to load the topics moderated by the user with id " + user.getId(), e);
            throw new StoreException("Error while trying to load the topics moderated by the user with id "
                    + user.getId(), e);
        }

        return moderatedTopics;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Topic> selectSubscribedTopics(final User user, final Selection selection) {
        if (selection == null) {
            log.error("Error when trying to get topics with selection null.");
            throw new IllegalArgumentException("Selection cannot be null.");
        } else if (StringUtils.isBlank(selection.getSortedBy())) {
            log.error("Error when trying to get topics sorted by nothing.");
            throw new IllegalArgumentException("Cannot sort by nothing.");
        } else if (user == null) {
            log.error("Cannot select subscribed topics when user is null.");
            throw new IllegalArgumentException("User cannot be null.");
        } else if (user.getId() == null) {
            log.error("Cannot select subscribed topics when user ID is null.");
            throw new IllegalArgumentException("User ID cannot be null.");
        }

        String sql = "SELECT s.*, t.*, l.last_activity FROM topic_subscription AS s"
                + " LEFT OUTER JOIN topic AS t ON s.topic = t.id"
                + " LEFT OUTER JOIN topic_last_activity AS l ON t.id = l.topic"
                + " WHERE s.subscriber = ?"
                + " ORDER BY " + selection.getSortedBy() + (selection.isAscending() ? " ASC" : " DESC")
                + " LIMIT ?"
                + " OFFSET ?;";

        List<Topic> selectedTopics =
                new ArrayList<>(Math.min(Pagitable.getItemLimit(selection), Math.max(0, selection.getTotalSize())));
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = new StatementParametrizer(stmt)
                    .integer(user.getId())
                    .integer(Pagitable.getItemLimit(selection))
                    .integer(Pagitable.getItemOffset(selection))
                    .toStatement().executeQuery();
            readTopicsFromResultSet(rs, selectedTopics);
        } catch (SQLException e) {
            log.error("Error while selecting subscribed topics for user " + user + " with " + selection + ".", e);
            throw new StoreException("Error while selecting subscribed topics for user " + user + " with " + selection
                    + ".", e);
        }
        return selectedTopics;
    }

    private void readTopicsFromResultSet(final ResultSet rs, final List<Topic> topics) throws SQLException {
        while (rs.next()) {
            topics.add(new Topic(rs.getInt("id"), rs.getString("title"), rs.getString("description"),
                    rs.getObject("last_activity", OffsetDateTime.class)));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countSubscribedTopics(final User user) {
        if (user == null) {
            log.error("Cannot count subscribed topics when user is null.");
            throw new IllegalArgumentException("User cannot be null.");
        } else if (user.getId() == null) {
            log.error("Cannot count subscribed topics when user ID is null.");
            throw new IllegalArgumentException("User ID cannot be null.");
        }
        String sql = "SELECT COUNT(*) AS count FROM topic_subscription WHERE subscriber = ?;";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            PreparedStatement statement = new StatementParametrizer(stmt)
                    .integer(user.getId()).toStatement();
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            } else {
                throw new InternalError("Could not count the number of topics.");
            }
        } catch (SQLException e) {
            log.error("Error while retrieving number of topics.", e);
            throw new StoreException("Error while retrieving number of topics.", e);
        }
    }

}
