package ar.edu.itba.paw.services;

import ar.edu.itba.paw.interfaces.SearchDAO;
import ar.edu.itba.paw.interfaces.SearchService;
import ar.edu.itba.paw.models.PageContainer;
import ar.edu.itba.paw.models.lists.MediaList;
import ar.edu.itba.paw.models.media.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private SearchDAO searchDAO;

    @Override
    public PageContainer<Media> searchMediaByTitle(String title, int page, int pageSize, int mediaType) {
        return searchDAO.searchMediaByTitle(title,page,pageSize, mediaType);
    }

    @Override
    public Optional<Integer> getCountSearchMediaByTitle(String title, int mediaType) {
        return searchDAO.getCountSearchMediaByTitle(title, mediaType);
    }

    @Override
    public PageContainer<Media> searchMediaByTitle(String title, int page, int pageSize) {
        return searchDAO.searchMediaByTitle(title,page,pageSize);
    }

    @Override
    public Optional<Integer> getCountSearchMediaByTitle(String title) {
        return searchDAO.getCountSearchMediaByTitle(title);
    }

    @Override
    public PageContainer<Media> searchMediaByTitle(String title, int page, int pageSize, int mediaType, int sort) {
        return searchDAO.searchMediaByTitle(title,page,pageSize,mediaType);
    }

    @Override
    public PageContainer<MediaList> searchListMediaByName(String name, int page, int pageSize, int sort) {
        return searchDAO.searchListMediaByName(name,page,pageSize, sort);
    }

}
