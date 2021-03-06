package tech.bugger.persistence.gateway;

import tech.bugger.global.transfer.Token;
import tech.bugger.global.util.Log;
import tech.bugger.persistence.exception.NotFoundException;
import tech.bugger.persistence.exception.StoreException;
import tech.bugger.persistence.util.StatementParametrizer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Token gateway that gives access to verification tokens stored in a database.
 */
public class TokenDBGateway implements TokenGateway {

    /**
     * The {@link Log} instance associated with this class for logging purposes.
     */
    private static final Log log = Log.forClass(TokenDBGateway.class);

    /**
     * Database connection used by this gateway.
     */
    private final Connection conn;

    /**
     * Constructs a new token gateway with the given database connection.
     *
     * @param conn The database connection to use for the gateway.
     */
    public TokenDBGateway(final Connection conn) {
        this.conn = conn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Token createToken(final Token token) throws NotFoundException {
        if (token.getUser() == null) {
            throw new IllegalArgumentException("User may not be null!");
        } else if (token.getUser().getId() == null) {
            throw new IllegalArgumentException("User ID may not be null!");
        } else if (token.getValue() == null) {
            throw new IllegalArgumentException("Token value may not be null!");
        } else if (token.getType() == null) {
            throw new IllegalArgumentException("Token type may not be null!");
        }

        Token ret;

        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO token (value, type, meta, verifies) "
                + "VALUES (?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS)) {

            new StatementParametrizer(stmt)
                    .string(token.getValue())
                    .object(token.getType(), Types.OTHER)
                    .string(token.getMeta())
                    .integer(token.getUser().getId())
                    .toStatement().executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                ret = findToken(rs.getString("value"));
            } else {
                log.error("Couldn't read new token data.");
                throw new StoreException("Couldn't read new token data.");
            }
        } catch (SQLException e) {
            if ("23503".equals(e.getSQLState())) {
                // 23503 states that "insert value of foreign key is invalid", hence the user is non-existent.
                log.error("Couldn't find user when inserting token into database.", e);
                throw new NotFoundException(e);
            } else {
                log.error("Couldn't insert token into database.", e);
                throw new StoreException(e);
            }
        }

        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Token findToken(final String value) throws NotFoundException {
        Token token;

        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM token t "
                + "JOIN \"user\" u on t.verifies = u.id WHERE t.value = ?")) {
            ResultSet rs = new StatementParametrizer(stmt).string(value).toStatement().executeQuery();
            if (rs.next()) {
                token = new Token(rs.getString("value"), Token.Type.valueOf(rs.getString("type")),
                        rs.getObject("timestamp", OffsetDateTime.class),
                        rs.getString("meta"), UserDBGateway.getUserFromResultSet(rs));
            } else {
                log.debug("Searched token by value could not be found.");
                throw new NotFoundException("Searched token by value could not be found!");
            }
        } catch (SQLException e) {
            log.error("Couldn't search the token by value due to a database error.", e);
            throw new StoreException(e);
        }

        return token;
    }

    /**
     * {@inheritDoc}
     */
    public void cleanExpiredTokens(final Duration expirationAge) {
        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM token WHERE \"timestamp\" < ?;")) {
            new StatementParametrizer(stmt)
                    .object(OffsetDateTime.now().minus(expirationAge))
                    .toStatement().executeUpdate();
        } catch (SQLException e) {
            log.error("Error when cleaning expired verification tokens.", e);
            throw new StoreException("Error when cleaning expired verification tokens.", e);
        }
    }

}
