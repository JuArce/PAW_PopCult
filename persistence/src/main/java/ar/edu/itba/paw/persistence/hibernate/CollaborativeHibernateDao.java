package ar.edu.itba.paw.persistence.hibernate;

import ar.edu.itba.paw.interfaces.CollaborativeListsDao;
import ar.edu.itba.paw.interfaces.exceptions.UserAlreadyCollaboratesInListException;
import ar.edu.itba.paw.models.PageContainer;
import ar.edu.itba.paw.models.collaborative.Request;
import ar.edu.itba.paw.models.lists.MediaList;
import ar.edu.itba.paw.models.media.Media;
import ar.edu.itba.paw.models.user.User;
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
    public Request makeNewRequest(MediaList mediaList, User user) {
        final Request request = new Request(user, mediaList);
        em.persist(request);
        return request;
    }

    @Override
    public PageContainer<Request> getRequestsByUserId(User user, int page, int pageSize) {
        final Query nativeQuery = em.createNativeQuery("SELECT collabid FROM (medialist m JOIN collaborative c ON m.medialistid = c.listid) JOIN users u on u.userid= c.collaboratorid AND m.userid = :userId WHERE accepted = :status OFFSET :offset LIMIT :limit");
        nativeQuery.setParameter("userId", user.getUserId());
        nativeQuery.setParameter("status", false);
        nativeQuery.setParameter("offset", page * pageSize);
        nativeQuery.setParameter("limit", pageSize);
        @SuppressWarnings("unchecked")
        List<Long> collabIds = nativeQuery.getResultList();
        final Query countQuery = em.createNativeQuery("SELECT COUNT(collabid) FROM (medialist m JOIN collaborative c ON m.medialistid = c.listid) JOIN users u on u.userid= c.collaboratorid AND m.userid = :userId WHERE accepted = :status");
        countQuery.setParameter("userId", user.getUserId());
        countQuery.setParameter("status", false);
        long count = ((Number) countQuery.getSingleResult()).longValue();

        final TypedQuery<Request> typedQuery = em.createQuery("FROM Request WHERE collabId IN (:collabIds)", Request.class)
                .setParameter("collabIds", collabIds);
        List<Request> requestList = collabIds.isEmpty() ? Collections.emptyList() : typedQuery.getResultList();
        return new PageContainer<>(requestList, page, pageSize, count);
    }

    @Override
    public void rejectRequest(Request request) {
        em.remove(request);
    }

    @Override
    public PageContainer<Request> getListCollaborators(MediaList mediaList, int page, int pageSize) {
        final Query nativeQuery = em.createNativeQuery("SELECT collabid FROM (medialist m JOIN collaborative c ON m.medialistid = c.listid AND medialistid = :listId) WHERE accepted = :status OFFSET :offset LIMIT :limit");
        nativeQuery.setParameter("listId", mediaList.getMediaListId());
        nativeQuery.setParameter("status", true);
        nativeQuery.setParameter("offset", page * pageSize);
        nativeQuery.setParameter("limit", pageSize);
        @SuppressWarnings("unchecked")
        List<Long> collabIds = nativeQuery.getResultList();
        final Query countQuery = em.createNativeQuery("SELECT COUNT(collabid) FROM (medialist m JOIN collaborative c ON m.medialistid = c.listid AND medialistid = :listId) WHERE accepted = :status");
        countQuery.setParameter("listId", mediaList.getMediaListId());
        countQuery.setParameter("status", true);
        long count = ((Number) countQuery.getSingleResult()).longValue();
        final TypedQuery<Request> typedQuery = em.createQuery("FROM Request WHERE collabId IN (:collabIds)", Request.class)
                .setParameter("collabIds", collabIds);
        List<Request> requestList = collabIds.isEmpty() ? Collections.emptyList() : typedQuery.getResultList();
        return new PageContainer<>(requestList, page, pageSize, count);
    }

    @Override
    public Optional<Request> getById(int collabId) {
        return Optional.ofNullable(em.find(Request.class, collabId));
    }

    private boolean userCollaboratesInList(MediaList mediaList, User user) {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM collaborative WHERE listid = :mediaListId AND collaboratorid = :userId")
                .setParameter("mediaListId", mediaList.getMediaListId())
                .setParameter("userId", user.getUserId())
                .getSingleResult()).intValue() != 0;
    }

    @Override
    public void addCollaborators(MediaList mediaList, List<User> users) throws UserAlreadyCollaboratesInListException {
        for (User user : users) {
            if (userCollaboratesInList(mediaList, user)){
                throw new UserAlreadyCollaboratesInListException();
            }
            em.persist(new Request(user, mediaList, true));
        }
    }

    @Override
    public Optional<Request> getUserListCollabRequest(MediaList mediaList, User user) {
        final TypedQuery<Request> query = em.createQuery("from Request where mediaList = :mediaList AND collaborator = :user", Request.class);
        query.setParameter("mediaList", mediaList);
        query.setParameter("user", user);
        return query.getResultList().stream().findFirst();
    }

}
