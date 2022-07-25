package ar.edu.itba.paw.persistence.hibernate;

import ar.edu.itba.paw.interfaces.StudioDao;
import ar.edu.itba.paw.models.PageContainer;
import ar.edu.itba.paw.models.media.Media;
import ar.edu.itba.paw.models.staff.Studio;
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
public class StudioHibernateDao implements StudioDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<Studio> getById(int studioId) {
        return Optional.ofNullable(em.find(Studio.class, studioId));
    }

    @Override
    public PageContainer<Media> getMediaByStudio(Studio studio, int page, int pageSize) {
        PaginationValidator.validate(page, pageSize);

        final Query nativeQuery = em.createNativeQuery("SELECT mediaid " +
                        "FROM mediastudio " +
                        "WHERE studioid = :studioid " +
                        "OFFSET :offset LIMIT :limit")
                .setParameter("studioid", studio.getStudioId())
                .setParameter("offset", (page - 1) * pageSize)
                .setParameter("limit", pageSize);
        @SuppressWarnings("unchecked")
        List<Long> mediaIds = nativeQuery.getResultList();

        final Query countQuery = em.createNativeQuery("SELECT COUNT(*) " +
                        "FROM mediaStudio " +
                        "WHERE studioId = :studioid")
                .setParameter("studioid", studio.getStudioId());
        final long count = ((Number) countQuery.getSingleResult()).longValue();

        final TypedQuery<Media> query = em.createQuery("FROM Media " +
                        "WHERE mediaId " +
                        "IN (:mediaids)", Media.class)
                .setParameter("mediaids", mediaIds);
        List<Media> mediaList = mediaIds.isEmpty() ? Collections.emptyList() : query.getResultList();

        return new PageContainer<>(mediaList, page, pageSize, count);
    }

    @Override
    public PageContainer<Studio> getAllStudios(int page, int pageSize) {
        PaginationValidator.validate(page, pageSize);

        final Query nativeQuery = em.createNativeQuery("SELECT studioid " +
                        "FROM studio " +
                        "OFFSET :offset LIMIT :limit")
                .setParameter("offset", (page - 1) * pageSize)
                .setParameter("limit", pageSize);
        @SuppressWarnings("unchecked")
        List<Long> studioIds = nativeQuery.getResultList();

        final Query countQuery = em.createQuery("SELECT COUNT(*) " +
                "FROM Studio");
        final long count = (long) countQuery.getSingleResult();

        final TypedQuery<Studio> query = em.createQuery("FROM Studio " +
                        "WHERE studioId " +
                        "IN :studioIds", Studio.class)
                .setParameter("studioIds", studioIds);
        List<Studio> studios = studioIds.isEmpty() ? Collections.emptyList() : query.getResultList();

        return new PageContainer<>(studios, page, pageSize, count);
    }
}
