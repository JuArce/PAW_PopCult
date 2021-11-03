package ar.edu.itba.paw.services;

import ar.edu.itba.paw.interfaces.SearchDao;
import ar.edu.itba.paw.interfaces.SearchService;
import ar.edu.itba.paw.models.PageContainer;
import ar.edu.itba.paw.models.lists.MediaList;
import ar.edu.itba.paw.models.media.Genre;
import ar.edu.itba.paw.models.media.Media;
import ar.edu.itba.paw.models.media.MediaType;
import ar.edu.itba.paw.models.search.SortType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private SearchDao searchDAO;

    @Transactional(readOnly = true)
    @Override
    public PageContainer<Media> searchMediaByTitle(String title, int page, int pageSize, List<MediaType> mediaType, SortType sort, List<Genre> genre, String fromDate, String toDate) throws ParseException {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        Date fDate = null;
        Date tDate = null;
        if(fromDate != null && toDate != null){
                fDate = f.parse(fromDate+ "-01-01");
                tDate = f.parse(toDate + "-12-31");
        }
        return searchDAO.searchMediaByTitle(title,page,pageSize,mediaType,sort,genre, fDate, tDate);
    }

    @Transactional(readOnly = true)
    @Override
    public PageContainer<MediaList> searchListMediaByName(String name, int page, int pageSize, SortType sort, List<Genre> genre, int minMatches) {
        return searchDAO.searchListMediaByName(name,page,pageSize,sort,genre, minMatches);
    }

    @Transactional(readOnly = true)
    @Override
    public PageContainer<Media> searchMediaByTitleNotInList(MediaList mediaList, String title, int page, int pageSize, List<MediaType> mediaType, SortType sort) {
        return searchDAO.searchMediaByTitleNotInList(mediaList, title, page, pageSize, mediaType, sort);
    }

}
