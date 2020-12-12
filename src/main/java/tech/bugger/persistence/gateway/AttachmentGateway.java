package tech.bugger.persistence.gateway;

import tech.bugger.global.transfer.Attachment;
import tech.bugger.global.transfer.Post;
import tech.bugger.persistence.exception.NotFoundException;

import java.util.List;

/**
 * An attachment gateway allows to query and modify a persistent storage of post attachments.
 */
public interface AttachmentGateway {

    /**
     * Retrieves an attachment by its ID.
     *
     * @param id The ID of the attachment to look for.
     * @return The attachment identified by the ID.
     * @throws NotFoundException The attachment could not be found.
     */
    public Attachment getContentByID(int id) throws NotFoundException;

    /**
     * Retrieves the list of attachments of a given post.
     *
     * @param post The post whose attachments to look for.
     * @return The list of attachments of the post.
     * @throws NotFoundException The post could not be found.
     */
    public List<Attachment> getAttachmentsForPost(Post post) throws NotFoundException;

    /**
     * Inserts an attachment into the attachment storage.
     *
     * @param attachment The attachment to insert.
     */
    public void create(Attachment attachment);

    /**
     * Updates an attachment's attributes in the attachment storage.
     *
     * @param attachment The attachment to update.
     * @throws NotFoundException The attachment could not be found.
     */
    public void update(Attachment attachment) throws NotFoundException;

    /**
     * Deletes an attachment from the attachment storage.
     *
     * @param attachment The attachment to delete.
     * @throws NotFoundException The attachment could not be found.
     */
    public void delete(Attachment attachment) throws NotFoundException;

}