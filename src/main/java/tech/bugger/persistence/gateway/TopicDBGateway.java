package tech.bugger.persistence.gateway;

import com.ocpsoft.pretty.faces.util.StringUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import tech.bugger.global.transfer.Selection;
import tech.bugger.global.transfer.Topic;
import tech.bugger.global.transfer.User;
import tech.bugger.global.util.Log;
import tech.bugger.persistence.exception.NotFoundException;
import tech.bugger.persistence.exception.StoreException;
import tech.bugger.persistence.util.StatementParametrizer;

import java.time.ZoneId;
import java.time.ZonedDateTime;
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
        int numberOfReports = 0;
        if (showOpenReports) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM report "
                    + "WHERE topic = ? AND closed_at IS NULL;")) {
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
        }
        if (showClosedReports) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM report "
                    + "WHERE topic = ? AND closed_at IS NOT NULL;")) {
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
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countBannedUsers(final Topic topic) {
        // TODO Auto-generated method stub
        return 0;
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
                + " LIMIT " + selection.getPageSize().getSize()
                + " OFFSET " + selection.getCurrentPage() * selection.getPageSize().getSize() + ";";

        List<Topic> selectedTopics = new ArrayList<>(Math.max(0, selection.getTotalSize()));
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                ZonedDateTime lastActivity = null;
                if (rs.getTimestamp("last_activity") != null) {
                    lastActivity = rs.getTimestamp("last_activity").toInstant().atZone(ZoneId.systemDefault());
                }
                selectedTopics.add(new Topic(rs.getInt("id"), rs.getString("title"), rs.getString("description"),
                        lastActivity));
            }
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
    public void createTopic(final Topic topic) {
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
    public void banUser(final Topic topic, final User user) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unbanUser(final Topic topic, final User user) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void promoteModerator(final Topic topic, final User user) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void demoteModerator(final Topic topic, final User user) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ZonedDateTime determineLastActivity(final Topic topic) throws NotFoundException {
        if (topic == null) {
            log.error("Error when trying to determine last activity in topic null.");
            throw new IllegalArgumentException("Topic must not be null!");
        } else if (topic.getId() == null) {
            log.error("Error when trying to determine last activity in topic with ID null.");
            throw new IllegalArgumentException("Topic ID must not be null!");
        }

        ZonedDateTime lastActivity = null;
        String sql = "SELECT * FROM topic_last_activity WHERE topic =" + topic.getId();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                if (rs.getTimestamp("last_activity") != null) {
                    lastActivity = rs.getTimestamp("last_activity").toInstant().atZone(ZoneId.systemDefault());
                }
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
            log.error("Cannot count subscribers of topic null.");
            throw new IllegalArgumentException("Topic cannot be null.");
        } else if (topic.getId() == null) {
            log.error("Cannot count subscribers of topic with ID null.");
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
    public int countPosts(final Topic topic) {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> discoverTopics() {
        List<String> topicTitles = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT title FROM topic ORDER BY title;")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                topicTitles.add(rs.getString("title"));
            }
        } catch (SQLException e) {
            log.error("Error while discovering all topics.", e);
            throw new StoreException("Error while discovering all topics.", e);
        }
        return topicTitles;
    }

}
