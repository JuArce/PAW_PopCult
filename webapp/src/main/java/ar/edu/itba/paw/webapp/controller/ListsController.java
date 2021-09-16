package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.interfaces.FavoriteService;
import ar.edu.itba.paw.interfaces.ListsService;
import ar.edu.itba.paw.interfaces.MediaService;
import ar.edu.itba.paw.interfaces.UserService;
import ar.edu.itba.paw.models.lists.ListCover;
import ar.edu.itba.paw.models.lists.MediaList;
import ar.edu.itba.paw.models.media.Media;
import ar.edu.itba.paw.models.user.User;
import ar.edu.itba.paw.webapp.exceptions.ListNotFoundException;
import ar.edu.itba.paw.webapp.exceptions.UserNotFoundException;
import ar.edu.itba.paw.webapp.form.ListForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

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

    private static final int itemsPerPage = 4;
    private static final int discoveryListsAmount = 4;
    private static final int lastAddedAmount = 4;

    @RequestMapping("/lists")
    public ModelAndView lists(@RequestParam(value = "page", defaultValue = "1") final int page) {
        final ModelAndView mav = new ModelAndView("lists");
        final List<MediaList> allLists = listsService.getAllLists(page - 1, itemsPerPage);
        final List<ListCover> discoveryCovers = generateCoverList(listsService.getDiscoveryMediaLists(discoveryListsAmount));
        final List<ListCover> recentlyAddedCovers = generateCoverList(listsService.getNLastAddedList(lastAddedAmount));
        final List<ListCover> allListsCovers = generateCoverList(allLists);
        final Integer allListsCount = listsService.getListCount().orElse(0);
        mav.addObject("discovery", discoveryCovers);
        mav.addObject("recentlyAdded", recentlyAddedCovers);
        mav.addObject("allLists", allListsCovers);
        mav.addObject("allListsPages", (int) Math.ceil((double) allListsCount / itemsPerPage));
        mav.addObject("currentPage", page);
        return mav;
    }

    private List<ListCover> generateCoverList(List<MediaList> MediaListLists) {
        return getListCover(MediaListLists, listsService, mediaService);
    }

    @RequestMapping("/lists/{listId}")
    public ModelAndView listDescription(@PathVariable("listId") final int listId) {
        final ModelAndView mav = new ModelAndView("listDescription");
        final MediaList mediaList = listsService.getMediaListById(listId).orElseThrow(ListNotFoundException::new);
        final List<Integer> mediaInList = listsService.getMediaIdInList(listId);
        final List<Media> mediaFromList = mediaService.getById(mediaInList);
        mav.addObject("list", mediaList);
        mav.addObject("media", mediaFromList);
        final User currentUser = userService.getCurrentUser(); //esto despues se reemplaza por el context del current user
        mav.addObject("currentUser", currentUser);
        mav.addObject("isFavoriteList", favoriteService.isFavoriteList(listId, currentUser.getUserId()));
        return mav;
    }


    @RequestMapping(value = "/createList", method = {RequestMethod.GET})
    public ModelAndView createListForm(@ModelAttribute("createListForm") final ListForm form) {
        return new ModelAndView("createListForm");
    }

    @RequestMapping(value = "/createList", method = {RequestMethod.POST})
    public ModelAndView postListForm(@Valid @ModelAttribute("createListForm") final ListForm form, final BindingResult errors) {
        if (errors.hasErrors())
            return createListForm(form);
        User user = userService.getCurrentUser();
        final MediaList mediaList = listsService.createMediaList(user.getUserId(), form.getListTitle(), form.getDescription(), form.isVisible(), form.isCollaborative());
        return new ModelAndView("redirect:/lists/" + mediaList.getMediaListId());
    }

    @RequestMapping(value = "/editList/{listId}", method = {RequestMethod.GET})
    public ModelAndView editList(@PathVariable("listId") final int listId, @ModelAttribute("createListForm") final ListForm form) {
        final ModelAndView mav = new ModelAndView("editList");
        final MediaList mediaList = listsService.getMediaListById(listId).orElseThrow(ListNotFoundException::new);
        final List<Integer> mediaInList = listsService.getMediaIdInList(listId);
        final List<Media> mediaFromList = mediaService.getById(mediaInList);
        mav.addObject("list", mediaList);
        mav.addObject("media", mediaFromList);
        return mav;
    }

    @RequestMapping(value = "/editList/{listId}", method = {RequestMethod.DELETE, RequestMethod.POST}, params = "mediaId")
    public ModelAndView deleteMediaFromList(@PathVariable("listId") final int listId, @RequestParam("mediaId") final int mediaId) {
        listsService.deleteMediaFromList(listId, mediaId);
        return new ModelAndView("redirect:/editList/" + listId);
    }

    @RequestMapping(value = "/editList/{listId}", method = {RequestMethod.DELETE, RequestMethod.POST}, params = "delete")
    public ModelAndView deleteList(@PathVariable("listId") final int listId) {
        listsService.deleteList(listId);
        return new ModelAndView("redirect:/lists");
    }

    @RequestMapping(value = "/editList/{listId}", method = {RequestMethod.POST}, params = "save")
    public ModelAndView submitList(@PathVariable("listId") final int listId, @Valid @ModelAttribute("createListForm") final ListForm form, final BindingResult errors) {
        if (errors.hasErrors())
            return editList(listId, form);
        listsService.updateList(listId, form.getListTitle(), form.getDescription(), form.isVisible(), form.isCollaborative());
        //update stuff
        return listDescription(listId);
    }

    @RequestMapping(value = "/lists/{listId}", method = {RequestMethod.POST})
    public ModelAndView createListCopy(@PathVariable("listId") final int listId, @RequestParam("currentUserId") final int currentUserId) {
        final MediaList newList = listsService.createMediaListCopy(currentUserId, listId);
        return new ModelAndView("redirect:/lists/" + newList.getMediaListId());
    }

    @RequestMapping(value = "/lists/{listId}", method = {RequestMethod.POST}, params = "addFav")
    public ModelAndView addListToFav(@PathVariable("listId") final int listId) {
        User user = userService.getCurrentUser();
        favoriteService.addListToFav(user.getUserId(), listId);
        return listDescription(listId);
    }

    @RequestMapping(value = "/lists/{listId}", method = {RequestMethod.POST}, params = "deleteFav")
    public ModelAndView deleteListFromFav(@PathVariable("listId") final int listId) {
        User user = userService.getCurrentUser();
        favoriteService.deleteListFromFav(user.getUserId(), listId);
        return listDescription(listId);
    }
}
