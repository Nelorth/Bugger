package tech.bugger.persistence.gateway;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import tech.bugger.global.transfer.Attachment;
import tech.bugger.global.transfer.Post;
import tech.bugger.global.util.Log;
import tech.bugger.persistence.exception.NotFoundException;
import tech.bugger.persistence.exception.StoreException;
import tech.bugger.persistence.util.StatementParametrizer;

/**
 * Attachment gateway that gives access to post attachments stored in a database.
 */
public class AttachmentDBGateway implements AttachmentGateway {

    /**
     * The {@link Log} instance associated with this class for logging purposes.
     */
    private static final Log log = Log.forClass(AttachmentDBGateway.class);

    /**
     * Database connection used by this gateway.
     */
    private final Connection conn;

    /**
     * Constructs a new attachment gateway with the given database connection.
     *
     * @param conn The database connection to use for the gateway.
     */
    public AttachmentDBGateway(final Connection conn) {
        this.conn = conn;
    }

    /**
     * Parses the given {@link ResultSet} and returns the corresponding {@link Attachment}.
     *
     * @param rs The {@link ResultSet} to parse.
     * @return The parsed {@link Attachment} without its (possibly large) content.
     * @throws SQLException Some parsing error occurred.
     */
    private Attachment getAttachmentFromResultSet(final ResultSet rs) throws SQLException {
        return new Attachment(
                rs.getInt("id"),
                rs.getString("name"),
                new byte[0],
                rs.getString("mimetype"),
                rs.getInt("post")
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create(final Attachment attachment) {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO attachment (name, content, mimetype, post)"
                        + "VALUES (?, ?, ?, ?);",
                PreparedStatement.RETURN_GENERATED_KEYS
        )) {
            PreparedStatement statement = new StatementParametrizer(stmt)
                    .string(attachment.getName())
                    .bytes(attachment.getContent())
                    .string(attachment.getMimetype())
                    .integer(attachment.getPost())
                    .toStatement();
            statement.executeUpdate();

            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                attachment.setId(generatedKeys.getInt("id"));
            } else {
                log.error("Error while retrieving new attachment ID.");
                throw new StoreException("Error while retrieving new attachment ID.");
            }
        } catch (SQLException e) {
            log.error("Error while creating attachment.", e);
            throw new StoreException("Error while creating attachment.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final Attachment attachment) throws NotFoundException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE attachment "
                        + "SET name = ?, content = ?, mimetype = ?, post = ? "
                        + "WHERE id = ?;"
        )) {
            int rowsAffected = new StatementParametrizer(stmt)
                    .string(attachment.getName())
                    .bytes(attachment.getContent())
                    .string(attachment.getMimetype())
                    .integer(attachment.getPost())
                    .integer(attachment.getId())
                    .toStatement().executeUpdate();
            if (rowsAffected == 0) {
                log.error("Attachment to be updated could not be found.");
                throw new NotFoundException("Attachment to be updated could not be found.");
            }
        } catch (SQLException e) {
            log.error("Error while updating attachment.", e);
            throw new StoreException("Error while updating attachment.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final Attachment attachment) throws NotFoundException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM attachment "
                        + "WHERE id = ?;"
        )) {
            int rowsAffected = new StatementParametrizer(stmt)
                    .integer(attachment.getId())
                    .toStatement().executeUpdate();
            if (rowsAffected == 0) {
                log.error("Attachment to be deleted could not be found.");
                throw new NotFoundException("Attachment to be deleted could not be found.");
            }
        } catch (SQLException e) {
            log.error("Error while deleting attachment.", e);
            throw new StoreException("Error while deleting attachment.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Attachment find(final int id) throws NotFoundException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, name, mimetype, post FROM attachment WHERE id = ?;"
        )) {
            ResultSet rs = new StatementParametrizer(stmt)
                    .integer(id)
                    .toStatement().executeQuery();
            if (rs.next()) {
                return getAttachmentFromResultSet(rs);
            } else {
                throw new NotFoundException("Attachment could not be found.");
            }
        } catch (SQLException e) {
            throw new StoreException("Error while retrieving attachment.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] findContent(final int id) throws NotFoundException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT content FROM attachment WHERE id = ?;"
        )) {
            ResultSet rs = new StatementParametrizer(stmt)
                    .integer(id)
                    .toStatement().executeQuery();
            if (rs.next()) {
                return rs.getBytes("content");
            } else {
                throw new NotFoundException("Attachment content could not be found.");
            }
        } catch (SQLException e) {
            throw new StoreException("Error while searching for attachment content.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Attachment> getAttachmentsForPost(final Post post) {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, name, mimetype, post FROM attachment WHERE post = ?;"
        )) {
            ResultSet rs = new StatementParametrizer(stmt).integer(post.getId()).toStatement().executeQuery();
            List<Attachment> attachments = new ArrayList<>();
            while (rs.next()) {
                attachments.add(getAttachmentFromResultSet(rs));
            }
            return attachments;
        } catch (SQLException e) {
            throw new StoreException("Error while retrieving attachment.", e);
        }
    }

}
