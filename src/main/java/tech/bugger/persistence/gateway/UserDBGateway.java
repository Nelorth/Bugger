package tech.bugger.persistence.gateway;

import com.ocpsoft.pretty.faces.util.StringUtils;
import tech.bugger.global.transfer.Report;
import tech.bugger.global.transfer.Selection;
import tech.bugger.global.transfer.Topic;
import tech.bugger.global.transfer.User;
import tech.bugger.global.util.Log;
import tech.bugger.global.util.Pagitable;
import tech.bugger.persistence.exception.NotFoundException;
import tech.bugger.persistence.exception.StoreException;
import tech.bugger.persistence.util.StatementParametrizer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * User gateway that gives access to user stored in a database.
 */
public class UserDBGateway implements UserGateway {

    /**
     * The {@link Log} instance associated with this class for logging purposes.
     */
    private static final Log log = Log.forClass(UserDBGateway.class);

    /**
     * The database connection used by this gateway.
     */
    private final Connection conn;

    /**
     * Constructs a new user gateway with the given database connection.
     *
     * @param conn The database connection to use for the gateway.
     */
    public UserDBGateway(final Connection conn) {
        this.conn = conn;
    }

    /**
     * Formats the given {@link StatementParametrizer} using the given {@link User} in the order specified in the file
     * {@code setup.sql} (without the ID and registration date).
     *
     * @param parametrizer The {@link StatementParametrizer} to format.
     * @param user         The {@link User} that should be written into the {@code parametrizer}.
     * @return The parametrizer with the given {@code user} inserted.
     * @throws SQLException Some parsing error occurred.
     */
    static StatementParametrizer storeUserInStatement(final StatementParametrizer parametrizer, final User user)
            throws SQLException {
        return parametrizer
                .string(user.getUsername())
                .string(user.getPasswordHash())
                .string(user.getPasswordSalt())
                .string(user.getHashingAlgorithm())
                .string(user.getEmailAddress())
                .string(user.getFirstName())
                .string(user.getLastName())
                .bytes(user.getAvatar())
                .bytes(user.getAvatarThumbnail())
                .string(user.getBiography())
                .string(user.getPreferredLanguage().getLanguage())
                .object(user.getProfileVisibility(), Types.OTHER)
                .object(user.getForcedVotingWeight(), Types.INTEGER)
                .bool(user.isAdministrator());
    }

    /**
     * Parses the given {@link ResultSet} and returns the corresponding {@link User}.
     *
     * @param rs The {@link ResultSet} to parse.
     * @return The parsed {@link User}.
     * @throws SQLException Some parsing error occurred.
     */
    static User getUserFromResultSet(final ResultSet rs) throws SQLException {
        return getUserFromResultSet("", rs);
    }

    /**
     * Parses the given {@link ResultSet} and returns the corresponding {@link User}.
     *
     * @param prefix The prefix to put in front of the column names.
     * @param rs     The {@link ResultSet} to parse.
     * @return The parsed {@link User}.
     * @throws SQLException Some parsing error occurred.
     */
    static User getUserFromResultSet(final String prefix, final ResultSet rs) throws SQLException {
        return new User(rs.getInt(prefix + "id"), rs.getString(prefix + "username"),
                rs.getString(prefix + "password_hash"), rs.getString(prefix + "password_salt"),
                rs.getString(prefix + "hashing_algorithm"), rs.getString(prefix + "email_address"),
                rs.getString(prefix + "first_name"), rs.getString(prefix + "last_name"),
                new byte[0], rs.getBytes(prefix + "avatar_thumbnail"),
                rs.getString(prefix + "biography"),
                Locale.forLanguageTag(rs.getString(prefix + "preferred_language").toUpperCase()),
                User.ProfileVisibility.valueOf(rs.getString(prefix + "profile_visibility").toUpperCase()),
                rs.getObject(prefix + "registered_at", OffsetDateTime.class),
                rs.getObject(prefix + "forced_voting_weight", Integer.class), rs.getBoolean(prefix + "is_admin"
        ));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isModerator(final User user, final Topic topic) {
        if (user.getId() == null || topic.getId() == null) {
            throw new IllegalArgumentException("The user or topic ID cannot be null!");
        }

        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM topic_moderation WHERE moderator = ? "
                + "AND topic = ?;")) {
            ResultSet resultSet = new StatementParametrizer(stmt)
                    .integer(user.getId())
                    .integer(topic.getId())
                    .toStatement().executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            log.error("Error while checking if the user with id " + user.getId() + " is a moderator of the topic "
                    + "with id " + topic.getId(), e);
            throw new StoreException("Error while checking if the user with id " + user.getId() + " is a moderator of "
                    + "the topic with id " + topic.getId(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfAdmins() {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(u.id) AS num_admins FROM \"user\" AS u "
                + "WHERE u.is_admin = true;")) {
            ResultSet resultSet = stmt.executeQuery();
            int numAdmins = 0;
            if (resultSet.next()) {
                numAdmins = resultSet.getInt("num_admins");
            }
            return numAdmins;
        } catch (SQLException e) {
            log.error("Error while counting the number of administrators", e);
            throw new StoreException("Error while counting the number of administrators", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public User getUserByID(final int id) throws NotFoundException {
        User user;

        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM \"user\" WHERE id = ?")) {
            ResultSet rs = new StatementParametrizer(stmt).integer(id).toStatement().executeQuery();

            if (rs.next()) {
                user = getUserFromResultSet(rs);
            } else {
                log.debug("No user with id " + id + " could be found in the database");
                throw new NotFoundException("No user with id " + id + " could be found in the database.");
            }
        } catch (SQLException e) {
            log.error("Error while searching for user with id " + id, e);
            throw new StoreException("Error while searching for user with id " + id, e);
        }
        return user;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public User getUserByUsername(final String username) throws NotFoundException {
        User user;

        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM \"user\" WHERE username = ?")) {
            ResultSet rs = new StatementParametrizer(stmt).string(username).toStatement().executeQuery();

            if (rs.next()) {
                user = getUserFromResultSet(rs);
            } else {
                log.debug("No user with the given username could be found in the database");
                throw new NotFoundException("No user with the given username could be found in the database.");
            }
        } catch (SQLException e) {
            log.error("Error while searching for user by username.", e);
            throw new StoreException("Error while searching for user by username.", e);
        }

        return user;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getAvatarForUser(final int id) throws NotFoundException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT avatar FROM \"user\" WHERE id = ?")) {
            ResultSet rs = new StatementParametrizer(stmt).integer(id).toStatement().executeQuery();

            if (rs.next()) {
                return rs.getBytes("avatar");
            } else {
                log.debug("No user with the given id could be found in the database.");
                throw new NotFoundException("No user with the given id could be found in the database.");
            }
        } catch (SQLException e) {
            log.error("Error while retrieving user avatar.", e);
            throw new StoreException("Error while retrieving user avatar.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public User getUserByEmail(final String emailAddress) throws NotFoundException {
        User user;

        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM \"user\" WHERE email_address = ?")) {
            ResultSet rs = new StatementParametrizer(stmt).string(emailAddress).toStatement().executeQuery();

            if (rs.next()) {
                user = getUserFromResultSet(rs);
            } else {
                log.debug("No user with the given e-mail address could be found in the database");
                throw new NotFoundException("No user with the given e-mail address could be found in the database.");
            }
        } catch (SQLException e) {
            log.error("Error while searching for user by e-mail address.", e);
            throw new StoreException("Error while searching for user by e-mail address.", e);
        }

        return user;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<User> getSelectedModerators(final Topic topic, final Selection selection) {
        validTopicSelection(topic, selection);

        List<User> moderators = new ArrayList<>(Math.max(0, selection.getTotalSize()));
        try (PreparedStatement stmt = conn.prepareStatement("SELECT u.* FROM \"user\" AS u, topic_moderation AS t "
                + "WHERE t.topic = ? AND u.id = t.moderator ORDER BY u.username ASC LIMIT ? OFFSET ?;")) {
            ResultSet rs = new StatementParametrizer(stmt)
                    .integer(topic.getId())
                    .integer(Pagitable.getItemLimit(selection))
                    .integer(Pagitable.getItemOffset(selection))
                    .toStatement().executeQuery();

            while (rs.next()) {
                moderators.add(getUserFromResultSet(rs));
            }
        } catch (SQLException e) {
            log.error("Error while loading the moderators of the topic with id " + topic.getId(), e);
            throw new StoreException("Error while loading the moderators of the topic with id " + topic.getId(), e);
        }
        return moderators;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<User> getSelectedBannedUsers(final Topic topic, final Selection selection) {
        validTopicSelection(topic, selection);

        List<User> banned = new ArrayList<>(Math.max(0, selection.getTotalSize()));
        try (PreparedStatement stmt = conn.prepareStatement("SELECT u.* FROM \"user\" AS u, topic_ban AS t "
                + "WHERE t.topic = ? AND u.id = t.outcast ORDER BY u.username ASC LIMIT ? OFFSET ?;")) {
            ResultSet rs = new StatementParametrizer(stmt)
                    .integer(topic.getId())
                    .integer(Pagitable.getItemLimit(selection))
                    .integer(Pagitable.getItemOffset(selection))
                    .toStatement().executeQuery();

            while (rs.next()) {
                banned.add(getUserFromResultSet(rs));
            }

            if (banned.size() == 0) {
                log.debug("No users banned for the topic with id " + topic.getId());
            }
        } catch (SQLException e) {
            log.error("Error while loading the banned users for the topic with id " + topic.getId(), e);
            throw new StoreException("Error while loading the banned users for the topic with id " + topic.getId(), e);
        }

        return banned;
    }

    /**
     * Checks if a given topic and selection are valid.
     *
     * @param topic     The topic to check.
     * @param selection The selection to check.
     */
    private void validTopicSelection(final Topic topic, final Selection selection) {
        if (selection == null || topic.getId() == null) {
            log.error("The selection or topic ID cannot be null!.");
            throw new IllegalArgumentException("The selection or topic ID cannot be null!.");
        } else if (StringUtils.isBlank(selection.getSortedBy())) {
            log.error("Error when trying to get moderators sorted by nothing.");
            throw new IllegalArgumentException("The selection needs to have a column to sort by.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createUser(final User user) {
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO \"user\" "
                        + "(username, password_hash, password_salt, "
                        + "hashing_algorithm, email_address, first_name, "
                        + "last_name, avatar, avatar_thumbnail, "
                        + "biography, preferred_language, "
                        + "profile_visibility, "
                        + "forced_voting_weight, is_admin) VALUES (?, ?, "
                        + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {

            storeUserInStatement(new StatementParametrizer(stmt), user)
                    .toStatement().executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                user.setId(rs.getInt("id"));
                user.setRegistrationDate(rs.getObject("registered_at", OffsetDateTime.class));
            } else {
                log.error("Couldn't read new user data.");
                throw new StoreException("Couldn't read new user data.");
            }
        } catch (SQLException e) {
            log.error("Couldn't create the user due to a database error.", e);
            throw new StoreException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateUser(final User user) throws NotFoundException {
        if (user.getId() == null) {
            throw new IllegalArgumentException("User ID may not be null!");
        }

        try (PreparedStatement stmt = conn.prepareStatement("UPDATE \"user\" SET "
                        + "username = ?, password_hash = ?, password_salt"
                        + " = ?, hashing_algorithm = ?, "
                        + "email_address = ?, first_name = ?, last_name ="
                        + " ?, avatar = ?, avatar_thumbnail = ?, "
                        + "biography = ?, preferred_language = ?, "
                        + "profile_visibility = ?, "
                        + "forced_voting_weight = ?, is_admin = ? "
                        + "WHERE id = ?",
                Statement.RETURN_GENERATED_KEYS)) {

            StatementParametrizer parametrizer = storeUserInStatement(new StatementParametrizer(stmt), user);
            int changedRows = parametrizer
                    .integer(user.getId())
                    .toStatement().executeUpdate();

            if (changedRows == 0) {
                log.error("No user with id " + user.getId() + " could be found in the database.");
                throw new NotFoundException("No user with id " + user.getId() + " could be found in the database.");
            }
        } catch (SQLException e) {
            log.error("Error while updating the user with id " + user.getId(), e);
            throw new StoreException("Error while updating user with id " + user.getId(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteUser(final User user) throws NotFoundException {
        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM \"user\" WHERE id = ?;")) {
            int modified = new StatementParametrizer(stmt).integer(user.getId()).toStatement().executeUpdate();
            if (modified == 0) {
                log.debug("The user with the id " + user.getId() + " has already been deleted.");
                throw new NotFoundException("The user with the id " + user.getId() + " has already been deleted.");
            }
        } catch (SQLException e) {
            log.error("Error while deleting the user with id " + user.getId(), e);
            throw new StoreException("Error while deleting the user with id " + user.getId(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<User> getSubscribersOf(final User user) {
        if (user == null) {
            log.error("Cannot get subscribers of user null.");
            throw new IllegalArgumentException("User cannot be null.");
        } else if (user.getId() == null) {
            log.error("Cannot get subscribers of user with ID null.");
            throw new IllegalArgumentException("User ID cannot be null.");
        }

        String sql = "SELECT * FROM user_subscription AS s"
                + " JOIN \"user\" u on u.id = s.subscriber"
                + " WHERE s.subscribee = ?;";
        List<User> subscribers;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            PreparedStatement statement = new StatementParametrizer(stmt)
                    .integer(user.getId()).toStatement();
            ResultSet rs = statement.executeQuery();
            subscribers = new ArrayList<>();
            while (rs.next()) {
                subscribers.add(getUserFromResultSet(rs));
            }
        } catch (SQLException e) {
            log.error("Error while retrieving subscribers of user " + user + ".", e);
            throw new StoreException("Error while retrieving subscribers of user " + user + ".", e);
        }
        return subscribers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<User> getSubscribersOf(final Report report) {
        if (report == null) {
            log.error("Cannot get subscribers of report null.");
            throw new IllegalArgumentException("Report cannot be null.");
        } else if (report.getId() == null) {
            log.error("Cannot get subscribers of report with ID null.");
            throw new IllegalArgumentException("Report ID cannot be null.");
        }

        String sql = "SELECT * FROM report_subscription AS s"
                + " JOIN \"user\" u on u.id = s.subscriber"
                + " WHERE s.report = ?;";
        List<User> subscribers;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            PreparedStatement statement = new StatementParametrizer(stmt)
                    .integer(report.getId()).toStatement();
            ResultSet rs = statement.executeQuery();
            subscribers = new ArrayList<>();
            while (rs.next()) {
                subscribers.add(getUserFromResultSet(rs));
            }
        } catch (SQLException e) {
            log.error("Error while retrieving subscribers of report " + report + ".", e);
            throw new StoreException("Error while retrieving subscribers of report " + report + ".", e);
        }
        return subscribers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<User> getSubscribersOf(final Topic topic) {
        if (topic == null) {
            log.error("Cannot get subscribers of topic null.");
            throw new IllegalArgumentException("Topic cannot be null.");
        } else if (topic.getId() == null) {
            log.error("Cannot get subscribers of topic with ID null.");
            throw new IllegalArgumentException("Topic ID cannot be null.");
        }

        String sql = "SELECT * FROM topic_subscription AS s"
                + " JOIN \"user\" u on u.id = s.subscriber"
                + " WHERE s.topic = ?;";
        List<User> subscribers;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            PreparedStatement statement = new StatementParametrizer(stmt)
                    .integer(topic.getId()).toStatement();
            ResultSet rs = statement.executeQuery();
            subscribers = new ArrayList<>();
            while (rs.next()) {
                subscribers.add(getUserFromResultSet(rs));
            }
        } catch (SQLException e) {
            log.error("Error while retrieving subscribers of topic " + topic + ".", e);
            throw new StoreException("Error while retrieving subscribers of topic " + topic + ".", e);
        }
        return subscribers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfPosts(final User user) throws NotFoundException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT num_posts FROM user_num_posts WHERE "
                + "author = ?;")) {
            ResultSet rs = new StatementParametrizer(stmt)
                    .integer(user.getId())
                    .toStatement().executeQuery();
            if (rs.next()) {
                return rs.getInt("num_posts");
            } else {
                log.error("No posts could be found for the user with id " + user.getId());
                throw new NotFoundException("No posts could be found for the user with id " + user.getId());
            }
        } catch (SQLException e) {
            log.error("Error while searching for number of posts of the user with id " + user.getId(), e);
            throw new StoreException("Error while searching for number of posts of the user with id "
                    + user.getId(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfModeratedTopics(final User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("User ID may not be null!");
        }

        int moderatedTopics = 0;
        try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(t.topic) AS num_topics FROM "
                + "topic_moderation AS t WHERE t.moderator = ?;")) {
            ResultSet rs = new StatementParametrizer(stmt)
                    .integer(user.getId())
                    .toStatement().executeQuery();

            if (rs.next()) {
                moderatedTopics = rs.getInt("num_topics");
            }
        } catch (SQLException e) {
            log.error("Error while counting the moderated topics for the user with id " + user.getId(), e);
            throw new StoreException("Error while counting the moderated topics for the user with id " + user.getId(),
                    e);
        }

        return moderatedTopics;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isBanned(final User user, final Topic topic) {
        if (user == null) {
            throw new IllegalArgumentException("Cannot ban user null!");
        } else if (user.getId() == null) {
            throw new IllegalArgumentException("Cannot ban user with ID null!");
        } else if (topic == null) {
            throw new IllegalArgumentException("Cannot ban user from topic null!");
        } else if (topic.getId() == null) {
            throw new IllegalArgumentException("Cannot ban user from topic with ID null!");
        }

        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM topic_ban WHERE outcast = ? "
                + "AND topic = ?;")) {
            ResultSet resultSet = new StatementParametrizer(stmt)
                    .integer(user.getId())
                    .integer(topic.getId())
                    .toStatement().executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            log.error("Error while checking if the user with id " + user.getId() + " is banned from the topic "
                    + "with id " + topic.getId(), e);
            throw new StoreException("Error while checking if the user with id " + user.getId() + " is banned from "
                    + "the topic with id " + topic.getId(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanExpiredRegistrations() {
        // @formatter:off
        String query =
                "DELETE FROM \"user\""
              + "WHERE NULLIF(password_hash, '') IS NULL "
              + "AND NOT EXISTS("
              + "    SELECT * "
              + "    FROM   token "
              + "    WHERE  type = 'REGISTER' "
              + "    AND    verifies = id"
              + ");";
        // @formatter:on
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error when cleaning expired user registrations.", e);
            throw new StoreException("Error when cleaning expired user registrations.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<User> selectSubscribedUsers(final User user, final Selection selection) {
        if (selection == null) {
            log.error("Cannot select subscribed users when selection is null.");
            throw new IllegalArgumentException("Selection cannot be null.");
        } else if (StringUtils.isBlank(selection.getSortedBy())) {
            log.error("Cannot select subscribed users when sorted by is blank.");
            throw new IllegalArgumentException("Cannot sort by nothing.");
        } else if (user == null) {
            log.error("Cannot select subscribed users when user is null.");
            throw new IllegalArgumentException("User cannot be null.");
        } else if (user.getId() == null) {
            log.error("Cannot select subscribed users when user ID is null.");
            throw new IllegalArgumentException("User ID cannot be null.");
        }

        String sql = " SELECT * FROM user_subscription AS s"
                + " LEFT OUTER JOIN \"user\" u ON s.subscribee = u.id"
                + " WHERE s.subscriber = ?"
                + " ORDER BY " + selection.getSortedBy() + (selection.isAscending() ? " ASC" : " DESC")
                + " LIMIT ? OFFSET ?;";
        List<User> selectedUsers =
                new ArrayList<>(Math.min(Pagitable.getItemLimit(selection), Math.max(0, selection.getTotalSize())));
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            PreparedStatement statement = new StatementParametrizer(stmt)
                    .integer(user.getId())
                    .integer(Pagitable.getItemLimit(selection))
                    .integer(Pagitable.getItemOffset(selection)).toStatement();
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                selectedUsers.add(getUserFromResultSet(rs));
            }
        } catch (SQLException e) {
            log.error("Error while selecting subscribed users for user " + user + " with " + selection + ".", e);
            throw new StoreException("Error while selecting subscribed users for user " + user + " with " + selection
                    + ".", e);
        }
        return selectedUsers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countSubscribedUsers(final User user) {
        if (user == null) {
            log.error("Cannot count subscribed users when user is null.");
            throw new IllegalArgumentException("User cannot be null.");
        } else if (user.getId() == null) {
            log.error("Cannot count subscribed users when user ID is null.");
            throw new IllegalArgumentException("User ID cannot be null.");
        }

        String sql = "SELECT COUNT(*) AS count FROM user_subscription WHERE subscriber = ?;";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            PreparedStatement statement = new StatementParametrizer(stmt)
                    .integer(user.getId()).toStatement();
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            } else {
                throw new InternalError("Could not count the number of subscribed users.");
            }
        } catch (SQLException e) {
            log.error("Error while retrieving number of users user " + user + " is subscribed to.", e);
            throw new StoreException("Error while retrieving number of users user " + user + " is subscribed to.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<User> getAllBannedUsers(final Topic topic) {
        if (topic == null) {
            log.error("Cannot retrieve all banned users for topic null.");
            throw new IllegalArgumentException("Topic cannot be null.");
        } else if (topic.getId() == null) {
            log.error("Cannot retrieve all banned users for topic with ID null.");
            throw new IllegalArgumentException("Topic ID cannot be null.");
        }

        List<User> banned = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT u.* FROM \"user\" AS u, topic_ban AS t "
                + "WHERE t.topic = ? AND u.id = t.outcast;")) {
            ResultSet rs = new StatementParametrizer(stmt)
                    .integer(topic.getId())
                    .toStatement().executeQuery();

            while (rs.next()) {
                banned.add(getUserFromResultSet(rs));
            }

            if (banned.size() == 0) {
                log.debug("No users banned for the topic with id " + topic.getId());
            }
        } catch (SQLException e) {
            log.error("Error while loading the banned users for the topic with id " + topic.getId(), e);
            throw new StoreException("Error while loading the banned users for the topic with id " + topic.getId(), e);
        }

        return banned;
    }

}
