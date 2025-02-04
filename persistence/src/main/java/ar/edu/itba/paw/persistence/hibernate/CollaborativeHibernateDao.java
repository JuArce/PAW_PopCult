package ar.edu.itba.paw.persistence.hibernate;

import ar.edu.itba.paw.interfaces.CollaborativeListsDao;
import ar.edu.itba.paw.interfaces.exceptions.CollaboratorRequestAlreadyExistsException;
import ar.edu.itba.paw.models.PageContainer;
import ar.edu.itba.paw.models.collaborative.CollabRequest;
import ar.edu.itba.paw.models.lists.MediaList;
import ar.edu.itba.paw.models.user.User;
import ar.edu.itba.paw.persistence.hibernate.utils.PaginationValidator;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Primary
@Repository
public class CollaborativeHibernateDao implements CollaborativeListsDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public CollabRequest makeNewRequest(MediaList mediaList, User user) throws CollaboratorRequestAlreadyExistsException {
        if (collabRequestAlreadyExists(mediaList, user)) {
            throw new CollaboratorRequestAlreadyExistsException();
        }
        final CollabRequest request = new CollabRequest(user, mediaList);
        em.persist(request);
        return request;
    }

    private boolean collabRequestAlreadyExists(MediaList mediaList, User user) {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) " +
                        "FROM collaborative " +
                        "WHERE listid = :listId AND collaboratorid = :userId")
                .setParameter("listId", mediaList.getMediaListId())
                .setParameter("userId", user.getUserId())
                .getSingleResult()).intValue() != 0;
    }

    @Override
    public Optional<CollabRequest> getById(int collabId) {
        return Optional.ofNullable(em.find(CollabRequest.class, collabId));
    }

    @Override
    public Optional<CollabRequest> getUserListCollabRequest(MediaList mediaList, User user) {
        final TypedQuery<CollabRequest> query = em.createQuery("FROM CollabRequest " +
                        "WHERE mediaList = :mediaList AND collaborator = :user", CollabRequest.class)
                .setParameter("mediaList", mediaList)
                .setParameter("user", user);
        return query.getResultList().stream().findFirst();
    }

    @Override
    public PageContainer<CollabRequest> getListCollaborators(MediaList mediaList, int page, int pageSize) {
        PaginationValidator.validate(page, pageSize);
        final Query nativeQuery = em.createNativeQuery("SELECT collabid " +
                        "FROM (medialist m JOIN collaborative c ON m.medialistid = c.listid AND medialistid = :listId) " +
                        "WHERE accepted = :status " +
                        "OFFSET :offset LIMIT :limit")
                .setParameter("listId", mediaList.getMediaListId())
                .setParameter("status", true)
                .setParameter("offset", (page - 1) * pageSize)
                .setParameter("limit", pageSize);
        @SuppressWarnings("unchecked")
        List<Long> collabIds = nativeQuery.getResultList();

        final Query countQuery = em.createNativeQuery("SELECT COUNT(collabid) " +
                        "FROM (medialist m JOIN collaborative c ON m.medialistid = c.listid AND medialistid = :listId) " +
                        "WHERE accepted = :status")
                .setParameter("listId", mediaList.getMediaListId())
                .setParameter("status", true);
        long count = ((Number) countQuery.getSingleResult()).longValue();

        final TypedQuery<CollabRequest> typedQuery = em.createQuery("FROM CollabRequest " +
                        "WHERE collabId IN (:collabIds)", CollabRequest.class)
                .setParameter("collabIds", collabIds);
        List<CollabRequest> requestList = collabIds.isEmpty() ? Collections.emptyList() : typedQuery.getResultList();

        return new PageContainer<>(requestList, page, pageSize, count);
    }

    @Override
    public PageContainer<CollabRequest> getRequestsByUser(User user, int page, int pageSize) {
        PaginationValidator.validate(page, pageSize);
        final Query nativeQuery = em.createNativeQuery("SELECT collabid " +
                        "FROM (medialist m JOIN collaborative c ON m.medialistid = c.listid) JOIN users u on u.userid = c.collaboratorid AND m.userid = :userId " +
                        "WHERE accepted = :status " +
                        "OFFSET :offset LIMIT :limit")
                .setParameter("userId", user.getUserId())
                .setParameter("status", false)
                .setParameter("offset", (page - 1) * pageSize)
                .setParameter("limit", pageSize);
        @SuppressWarnings("unchecked")
        List<Long> collabIds = nativeQuery.getResultList();

        final Query countQuery = em.createNativeQuery("SELECT COUNT(collabid) " +
                        "FROM (medialist m JOIN collaborative c ON m.medialistid = c.listid) JOIN users u on u.userid= c.collaboratorid AND m.userid = :userId " +
                        "WHERE accepted = :status")
                .setParameter("userId", user.getUserId())
                .setParameter("status", false);
        long count = ((Number) countQuery.getSingleResult()).longValue();

        final TypedQuery<CollabRequest> typedQuery = em.createQuery("FROM CollabRequest " +
                        "WHERE collabId IN (:collabIds)", CollabRequest.class)
                .setParameter("collabIds", collabIds);
        List<CollabRequest> requestList = collabIds.isEmpty() ? Collections.emptyList() : typedQuery.getResultList();

        return new PageContainer<>(requestList, page, pageSize, count);
    }

    @Override
    public void rejectRequest(CollabRequest request) {
        em.remove(request);
    }

    @Override
    public void addCollaborator(MediaList mediaList, User user) {
        if (user.equals(mediaList.getUser()) || userCollaboratesInList(mediaList, user)) {
            return;
        }
        Optional<CollabRequest> request = getUserListCollabRequest(mediaList, user);
        if (request.isPresent()) {
            request.get().setAccepted(true);
            return;
        }
        em.persist(new CollabRequest(user, mediaList, true));
    }

    private boolean userCollaboratesInList(MediaList mediaList, User user) {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) " +
                        "FROM collaborative " +
                        "WHERE listid = :mediaListId AND collaboratorid = :userId AND accepted = true")
                .setParameter("mediaListId", mediaList.getMediaListId())
                .setParameter("userId", user.getUserId())
                .getSingleResult()).intValue() != 0;
    }

    @Override
    public void addCollaborators(MediaList mediaList, List<User> users) {
        users.forEach(u -> addCollaborator(mediaList, u));
    }

    @Override
    public void deleteCollaborators(MediaList mediaList, List<User> users) {
        users.forEach(u -> getUserListCollabRequest(mediaList, u).ifPresent(this::rejectRequest));
    }
}
