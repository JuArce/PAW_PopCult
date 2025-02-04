package ar.edu.itba.paw.persistence.hibernate;

import ar.edu.itba.paw.interfaces.WatchDao;
import ar.edu.itba.paw.models.PageContainer;
import ar.edu.itba.paw.models.media.Media;
import ar.edu.itba.paw.models.media.WatchedMedia;
import ar.edu.itba.paw.models.user.User;
import ar.edu.itba.paw.persistence.hibernate.utils.PaginationValidator;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Primary
@Repository
public class WatchHibernateDao implements WatchDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void addWatchedMedia(Media media, User user, LocalDateTime date) {
        final WatchedMedia watchedMedia = new WatchedMedia(user, media, date);
        em.persist(watchedMedia);
    }

    @Override
    public void deleteWatchedMedia(Media media, User user) {
        em.createNativeQuery("DELETE FROM towatchmedia WHERE userid = :userId AND mediaid = :mediaId AND watchdate IS NOT NULL")
                .setParameter("mediaId", media.getMediaId())
                .setParameter("userId", user.getUserId())
                .executeUpdate();
    }

    @Override
    public void deleteToWatchMedia(Media media, User user) {
        em.createNativeQuery("DELETE FROM towatchmedia WHERE userid = :userId AND mediaid = :mediaId AND watchdate IS NULL")
                .setParameter("mediaId", media.getMediaId())
                .setParameter("userId", user.getUserId())
                .executeUpdate();
    }

    @Override
    public void updateWatchedMediaDate(Media media, User user, LocalDateTime date) {
        em.createNativeQuery("UPDATE towatchmedia SET watchdate = :date WHERE mediaid = :mediaId AND userid = :userId AND watchdate IS NOT NULL")
                .setParameter("date", date)
                .setParameter("userId", user.getUserId())
                .setParameter("mediaId", media.getMediaId())
                .executeUpdate();
    }

    @Override
    public boolean isWatched(Media media, User user) {
        return !(((Number) em.createNativeQuery("SELECT COUNT(mediaid) " +
                        "FROM towatchmedia " +
                        "WHERE mediaId = :mediaId AND userid = :userId AND watchDate IS NOT NULL")
                .setParameter("mediaId", media.getMediaId())
                .setParameter("userId", user.getUserId()).getSingleResult()).intValue() == 0);
    }

    @Override
    public boolean isToWatch(Media media, User user) {
        return !(((Number) em.createNativeQuery("SELECT COUNT(mediaid) " +
                        "FROM towatchmedia " +
                        "WHERE mediaId = :mediaId AND userid = :userId AND watchDate IS NULL")
                .setParameter("mediaId", media.getMediaId())
                .setParameter("userId", user.getUserId()).getSingleResult()).intValue() == 0);
    }

    @Override
    public Optional<WatchedMedia> getWatchedMedia(User user, Media media) {
        return em.createQuery("FROM WatchedMedia " +
                        "WHERE media = :media AND user = :user AND watchDate IS NOT NULL", WatchedMedia.class)
                .setParameter("media", media)
                .setParameter("user", user)
                .getResultList().stream().findFirst();
    }

    @Override
    public PageContainer<WatchedMedia> getWatchedMedia(User user, int page, int pageSize) {
        PaginationValidator.validate(page, pageSize);

        final Query nativeQuery = em.createNativeQuery("SELECT watchedmediaid " +
                        "FROM towatchmedia NATURAL JOIN media " +
                        "WHERE userId = :userId AND watchDate IS NOT NULL " +
                        "ORDER BY watchDate DESC OFFSET :offset LIMIT :limit")
                .setParameter("userId", user.getUserId())
                .setParameter("offset", (page - 1) * pageSize)
                .setParameter("limit", pageSize);
        @SuppressWarnings("unchecked")
        List<Long> mediaIds = nativeQuery.getResultList();
        final Query countQuery = em.createNativeQuery("SELECT COUNT(watchedmediaid) " +
                        "FROM towatchmedia NATURAL JOIN media " +
                        "WHERE userId = :userId AND watchDate IS NOT NULL")
                .setParameter("userId", user.getUserId());
        long count = ((Number) countQuery.getSingleResult()).longValue();

        final TypedQuery<WatchedMedia> typedQuery = em.createQuery("FROM WatchedMedia " +
                        "WHERE watchedMediaId IN (:mediaIds)", WatchedMedia.class)
                .setParameter("mediaIds", mediaIds);
        List<WatchedMedia> mediaList = mediaIds.isEmpty() ? Collections.emptyList() : typedQuery.getResultList();

        return new PageContainer<>(mediaList, page, pageSize, count);
    }

    @Override
    public PageContainer<Media> getToWatchMedia(User user, int page, int pageSize) {
        PaginationValidator.validate(page, pageSize);

        final Query nativeQuery = em.createNativeQuery("SELECT mediaid " +
                        "FROM towatchmedia NATURAL JOIN media " +
                        "WHERE userId = :userId AND watchDate IS NULL " +
                        "ORDER BY watchDate DESC OFFSET :offset LIMIT :limit")
                .setParameter("userId", user.getUserId())
                .setParameter("offset", (page - 1) * pageSize)
                .setParameter("limit", pageSize);
        @SuppressWarnings("unchecked")
        List<Long> mediaIds = nativeQuery.getResultList();

        final Query countQuery = em.createNativeQuery("SELECT COUNT(mediaid) " +
                        "FROM towatchmedia NATURAL JOIN media " +
                        "WHERE userId = :userId AND watchDate IS NULL")
                .setParameter("userId", user.getUserId());
        long count = ((Number) countQuery.getSingleResult()).longValue();

        final TypedQuery<Media> typedQuery = em.createQuery("FROM Media " +
                        "WHERE mediaId " +
                        "IN (:mediaIds)", Media.class)
                .setParameter("mediaIds", mediaIds);
        List<Media> mediaList = mediaIds.isEmpty() ? Collections.emptyList() : typedQuery.getResultList();

        return new PageContainer<>(mediaList, page, pageSize, count);
    }
}
