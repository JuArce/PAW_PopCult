package ar.edu.itba.paw.interfaces;

import ar.edu.itba.paw.models.PageContainer;
import ar.edu.itba.paw.models.comment.Comment;

import java.util.Optional;

public interface CommentDao {

    Comment addCommentToMedia(int userId, int mediaId, String comment);

    Comment addCommentToList(int userId, int listId, String comment);

    void addCommentNotification(int commentId);

    Optional<Comment> getMediaCommentById(int commentId);

    Optional<Comment> getListCommentById(int commentId);

    PageContainer<Comment> getMediaComments(int mediaId, int page, int pageSize);

    PageContainer<Comment> getListComments(int listId, int page, int pageSize);

    void deleteCommentFromList(int commentId);

    void deleteCommentFromMedia(int commentId);

    PageContainer<Comment> getUserListsCommentsNotifications(int userId, int page, int pageSize);

    void setUserListsCommentsNotificationsAsOpened(int userId);

    void deleteUserListsCommentsNotifications(int userId);

}
