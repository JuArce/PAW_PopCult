package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.interfaces.*;
import ar.edu.itba.paw.interfaces.exceptions.MediaAlreadyInListException;
import ar.edu.itba.paw.models.PageContainer;
import ar.edu.itba.paw.models.comment.Comment;
import ar.edu.itba.paw.models.lists.ListCover;
import ar.edu.itba.paw.models.lists.MediaList;
import ar.edu.itba.paw.models.media.Media;
import ar.edu.itba.paw.models.media.MediaType;
import ar.edu.itba.paw.models.search.SortType;
import ar.edu.itba.paw.models.user.User;
import ar.edu.itba.paw.webapp.exceptions.ListNotFoundException;
import ar.edu.itba.paw.webapp.exceptions.NoUserLoggedException;
import ar.edu.itba.paw.webapp.exceptions.UserNotFoundException;
import ar.edu.itba.paw.webapp.form.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

import static ar.edu.itba.paw.webapp.utilities.ListCoverImpl.getListCover;


@Controller
public class ListsController {
    @Autowired
    private UserService userService;
    @Autowired
    private MediaService mediaService;
    @Autowired
    private ListsService listsService;
    @Autowired
    private FavoriteService favoriteService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private CollaborativeListService collaborativeListService;

    private static final int itemsPerPage = 6;
    private static final int discoveryListsAmount = 4;
    private static final int lastAddedAmount = 6;
    private static final int defaultValue = 1;
    private static final int searchAmount = 12;
    private static final int collaboratorsAmount = 20;

    @RequestMapping("/lists")
    public ModelAndView lists(@RequestParam(value = "page", defaultValue = "1") final int page) {
        final ModelAndView mav = new ModelAndView("lists");
        final PageContainer<MediaList> allLists = listsService.getAllLists(page - 1, itemsPerPage);
        final List<ListCover> discoveryCovers = generateCoverList(listsService.getDiscoveryMediaLists(discoveryListsAmount));
        final List<ListCover> mostLikedLists = generateCoverList(listsService.getMostLikedLists(defaultValue - 1, lastAddedAmount).getElements());
        final List<ListCover> allListsCovers = generateCoverList(allLists.getElements());
        mav.addObject("discovery", discoveryCovers);
        mav.addObject("mostLikedLists", mostLikedLists);
        mav.addObject("allLists", allListsCovers);
        mav.addObject("allListContainer", allLists);
        return mav;
    }

    private List<ListCover> generateCoverList(List<MediaList> MediaListLists) {
        return getListCover(MediaListLists, listsService);
    }

    @RequestMapping(value = "/lists/{listId}", method = {RequestMethod.GET})
    public ModelAndView listDescription(@PathVariable("listId") final int listId, @ModelAttribute("commentForm") CommentForm commentForm) {
        final ModelAndView mav = new ModelAndView("listDescription");
        final MediaList mediaList = listsService.getMediaListById(listId).orElseThrow(ListNotFoundException::new);
        final User u = listsService.getListOwner(mediaList.getMediaListId()).orElseThrow(UserNotFoundException::new);
        final List<Media> mediaFromList = listsService.getMediaIdInList(listId);
        final PageContainer<Comment> listCommentsContainer = commentService.getListComments(listId, defaultValue - 1, itemsPerPage);
        mav.addObject("list", mediaList);
        mav.addObject("media", mediaFromList);
        mav.addObject("user", u);
        mav.addObject("listCommentsContainer", listCommentsContainer);
        userService.getCurrentUser().ifPresent(user -> {
            mav.addObject("currentUser", user);
            mav.addObject("isFavoriteList", favoriteService.isFavoriteList(listId, user.getUserId()));
            mav.addObject("canEdit", listsService.canEditList(user.getUserId(), listId));
        });
        return mav;
    }

    @RequestMapping(value = "/lists/{listId}/comment", method = {RequestMethod.POST})
    public ModelAndView addComment(@PathVariable("listId") final int listId, @RequestParam("userId") int userId, @Valid @ModelAttribute("searchForm") final CommentForm form, final BindingResult errors) {
        if (errors.hasErrors())
            return listDescription(listId, form);
        commentService.addCommentToList(userId, listId, form.getBody());
        return new ModelAndView("redirect:/lists/" + listId);
    }

    @RequestMapping(value = "/lists/{listId}/deleteComment", method = {RequestMethod.DELETE, RequestMethod.POST})
    public ModelAndView deleteComment(@PathVariable("listId") final int listId, @RequestParam("commentId") int commentId) {
        commentService.deleteCommentFromList(commentId);
        return new ModelAndView("redirect:/lists/" + listId);
    }

    @RequestMapping(value = "/lists/{listId}/sendRequest", method = {RequestMethod.POST})
    public ModelAndView sendRequestToCollab(@PathVariable("listId") final int listId, @RequestParam("userId") int userId) {
        collaborativeListService.makeNewRequest(listId, userId);
        return new ModelAndView("redirect:/lists/" + listId).addObject("successfulRequest", true); //TODO mensaje de que salio todo ok
    }

    @RequestMapping(value = "/lists/{listId}/cancelCollab", method = {RequestMethod.POST})
    public ModelAndView cancelCollabPermissions(@PathVariable("listId") final int listId, @RequestParam("collabId") int collabId) {
        collaborativeListService.deleteCollaborator(collabId);
        return new ModelAndView("redirect:/lists/edit/" + listId+"/manageMedia");
    }

    //CREATE A NEW LIST - PART 1
    @RequestMapping(value = "/lists/new", method = {RequestMethod.GET})
    public ModelAndView createListForm(@ModelAttribute("createListForm") final ListForm form) {
        return new ModelAndView("createListForm");
    }

    @RequestMapping(value = "/lists/new", method = {RequestMethod.POST})
    public ModelAndView postListForm(@Valid @ModelAttribute("createListForm") final ListForm form, final BindingResult errors) {
        if (errors.hasErrors())
            return createListForm(form);
        User user = userService.getCurrentUser().orElseThrow(NoUserLoggedException::new);
        final MediaList mediaList = listsService.createMediaList(user.getUserId(), form.getListTitle(), form.getDescription(), form.isVisible(), form.isCollaborative());
        return new ModelAndView("redirect:/lists/edit/" + mediaList.getMediaListId() + "/manageMedia");
    }

    // MANAGE MEDIA IN LIST
    @RequestMapping(value = "/lists/edit/{listId}/manageMedia", method = {RequestMethod.GET})
    public ModelAndView manageMediaFromList(@PathVariable("listId") Integer mediaListId, @RequestParam(value = "page", defaultValue = "1") final int page, @ModelAttribute("editListDetails") final ListForm form, @ModelAttribute("mediaForm") ListMediaForm mediaForm) {
        final ModelAndView mav = new ModelAndView("manageMediaFromList");
        return addMediaObjects(mediaListId, mediaForm, page, mav);
    }

    @RequestMapping(value = "/lists/edit/{listId}/deleteMedia", method = {RequestMethod.DELETE, RequestMethod.POST})
    public ModelAndView deleteFromList(@PathVariable("listId") Integer mediaListId, @RequestParam("mediaId") Integer mediaId) {
        listsService.deleteMediaFromList(mediaListId, mediaId);
        return new ModelAndView("redirect:/lists/edit/" + mediaListId + "/manageMedia");
    }

    @RequestMapping(value = "/lists/edit/{listId}/search", method = {RequestMethod.GET}, params = "search")
    public ModelAndView searchMediaToAddToList(@PathVariable("listId") Integer mediaListId, HttpServletRequest request,
                                               @Valid @ModelAttribute("searchForm") final SearchForm searchForm,
                                               final BindingResult errors,
                                               @RequestParam(value = "sort", defaultValue = "title") final String sortType,
                                               @ModelAttribute("editListDetails") final ListForm form, @ModelAttribute("mediaForm") ListMediaForm mediaForm) {

        if (errors.hasErrors()) {
//            LOGGER.info("Redirecting to: {}", request.getHeader("referer"));
            return manageMediaFromList(mediaListId, defaultValue, form, mediaForm);
        }
        final List<Media> searchResults = searchService.searchMediaByTitleNotInList(mediaListId, searchForm.getTerm(), defaultValue - 1, searchAmount, MediaType.MOVIE.ordinal(), SortType.valueOf(sortType.toUpperCase()).ordinal()).getElements();
        searchResults.addAll(searchService.searchMediaByTitleNotInList(mediaListId, searchForm.getTerm(), defaultValue - 1, searchAmount, MediaType.SERIE.ordinal(), SortType.valueOf(sortType.toUpperCase()).ordinal()).getElements());
        return manageMediaFromList(mediaListId, defaultValue, form, mediaForm).addObject("searchTerm", searchForm.getTerm()).addObject("searchResults", mediaForm.generateMediaMap(searchResults));

    }

    @RequestMapping(value = "/lists/edit/{listId}/addMedia", method = {RequestMethod.POST}, params = "add")
    public ModelAndView insertToList(@PathVariable("listId") Integer mediaListId, @ModelAttribute("editListDetails") final ListForm form, @Valid @ModelAttribute("mediaForm") ListMediaForm mediaForm, final BindingResult errors) {
        try {
            listsService.addToMediaList(mediaListId, mediaForm.getMedia());
        } catch (MediaAlreadyInListException e) {
            return manageMediaFromList(mediaListId, defaultValue, form, mediaForm).addObject("alreadyInList", true);//TODO add in jsp message.
        }
        return new ModelAndView("redirect:/lists/edit/" + mediaListId + "/manageMedia");
    }

    @RequestMapping(value = "/lists/edit/{listId}/delete", method = {RequestMethod.DELETE, RequestMethod.POST}, params = "delete")
    public ModelAndView deleteList(@PathVariable("listId") final int listId) {
        listsService.deleteList(listId);
        return new ModelAndView("redirect:/lists");
    }

    @RequestMapping(value = "/lists/edit/{listId}/update", method = {RequestMethod.POST}, params = "save")
    public ModelAndView submitList(@PathVariable("listId") final int listId, @Valid @ModelAttribute("editListDetails") final ListForm form, final BindingResult errors, @ModelAttribute("mediaForm") ListMediaForm mediaForm) {
        if (errors.hasErrors()) {
            System.out.println(errors.hasErrors());
            return manageMediaFromList(listId, defaultValue, form, mediaForm).addObject("editDetailsErrors", errors.hasErrors());
        }
        listsService.updateList(listId, form.getListTitle(), form.getDescription(), form.isVisible(), form.isCollaborative());
        return new ModelAndView("redirect:/lists/edit/" + listId + "/manageMedia");
    }
    //END EDIT LIST

    @RequestMapping(value = "/lists/{listId}", method = {RequestMethod.POST}, params = "fork")
    public ModelAndView createListCopy(@PathVariable("listId") final int listId) {
        User user = userService.getCurrentUser().orElseThrow(NoUserLoggedException::new);
        final MediaList newList = listsService.createMediaListCopy(user.getUserId(), listId).orElseThrow(ListNotFoundException::new);
        return new ModelAndView("redirect:/lists/" + newList.getMediaListId());
    }

    @RequestMapping(value = "/lists/{listId}", method = {RequestMethod.POST}, params = "addFav")
    public ModelAndView addListToFav(@PathVariable("listId") final int listId) {
        User user = userService.getCurrentUser().orElseThrow(NoUserLoggedException::new);
        favoriteService.addListToFav(listId, user.getUserId());
        return new ModelAndView("redirect:/lists/" + listId);
        //return listDescription(listId);
    }

    @RequestMapping(value = "/lists/{listId}", method = {RequestMethod.POST}, params = "deleteFav")
    public ModelAndView deleteListFromFav(@PathVariable("listId") final int listId) {
        User user = userService.getCurrentUser().orElseThrow(NoUserLoggedException::new);
        favoriteService.deleteListFromFav(listId, user.getUserId());
        return new ModelAndView("redirect:/lists/" + listId);
        //return listDescription(listId);
    }

    private ModelAndView addMediaObjects(@PathVariable("listId") Integer mediaListId, @ModelAttribute("mediaForm") ListMediaForm mediaForm, @RequestParam(value = "page", defaultValue = "1") final int page, ModelAndView mav) {
        PageContainer<Media> pageContainer = listsService.getMediaIdInList(mediaListId, page - 1, itemsPerPage);
        mav.addObject("list", listsService.getMediaListById(mediaListId).orElseThrow(ListNotFoundException::new));
        mav.addObject("mediaContainer", pageContainer);
        mav.addObject("mediaListId", mediaListId);
        mav.addObject("collaboratorsContainer", collaborativeListService.getListCollaborators(mediaListId, defaultValue - 1, collaboratorsAmount));
        mav.addObject("currentUser", userService.getCurrentUser().orElseThrow(UserNotFoundException::new));
        return mav;
    }
}
