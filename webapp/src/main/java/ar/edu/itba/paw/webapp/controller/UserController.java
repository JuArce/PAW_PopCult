package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.interfaces.*;
import ar.edu.itba.paw.models.lists.ListCover;
import ar.edu.itba.paw.models.lists.MediaList;
import ar.edu.itba.paw.models.media.Media;
import ar.edu.itba.paw.models.user.User;
import ar.edu.itba.paw.webapp.exceptions.NoUserLoggedException;
import ar.edu.itba.paw.webapp.exceptions.UserNotFoundException;
import ar.edu.itba.paw.webapp.form.ListForm;
import ar.edu.itba.paw.webapp.form.UserForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;
import java.util.List;

import static ar.edu.itba.paw.webapp.utilities.ListCoverImpl.getListCover;


@Controller
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private MediaService mediaService;
    @Autowired
    private ListsService listsService;
    @Autowired
    private FavoriteService favoriteService;
    @Autowired
    private WatchService watchService;

    private static final int listsPerPage = 4;
    private static final int itemsPerPage = 4;


    @RequestMapping("/{username}")
    public ModelAndView userProfile(@PathVariable("username") final String username, @RequestParam(value = "page", defaultValue = "1") final int page) {
        ModelAndView mav = new ModelAndView("userProfile");
        User user = userService.getByUsername(username).orElseThrow(UserNotFoundException::new);
        List<MediaList> userLists = listsService.getMediaListByUserId(user.getUserId(), page - 1, listsPerPage);
        final List<ListCover> userListsCover = getListCover(userLists, listsService, mediaService);
        mav.addObject(user);
        Integer listsAmount = listsService.getListCountFromUserId(user.getUserId()).orElse(0);
        mav.addObject("lists", userListsCover);
        mav.addObject("listsAmount", listsAmount);
        mav.addObject("listsPages", (int) Math.ceil((double) listsAmount / listsPerPage));
        mav.addObject("currentPage", page);
        return mav;
    }

    @RequestMapping("/{username}/favoriteMedia")
    public ModelAndView userFavoriteMedia(@PathVariable("username") final String username, @RequestParam(value = "page", defaultValue = "1") final int page) {
        ModelAndView mav = new ModelAndView("userFavoriteMedia");
        User user = userService.getByUsername(username).orElseThrow(UserNotFoundException::new);
        List<Media> userMedia = mediaService.getById(favoriteService.getUserFavoriteMedia(user.getUserId(), page - 1, itemsPerPage));
        List<Media> suggestedMedia = mediaService.getMediaList(page - 1, itemsPerPage);
        final Integer suggestedMediaCount = mediaService.getMediaCount().orElse(0);
        Integer mediaCount = favoriteService.getFavoriteMediaCount(user.getUserId()).orElse(0);
        mav.addObject("mediaList", userMedia);
        mav.addObject("favoriteAmount", mediaCount);
        mav.addObject("suggestedMedia", suggestedMedia);
        mav.addObject("suggestedMediaPages", (int) Math.ceil((double) suggestedMediaCount / itemsPerPage));
        mav.addObject("mediaPages", (int) Math.ceil((double) mediaCount / itemsPerPage));
        mav.addObject("currentPage", page);
        mav.addObject(user);
        return mav;
    }

    @RequestMapping("/{username}/toWatchMedia")
    public ModelAndView userToWatchMedia(@PathVariable("username") final String username, @RequestParam(value = "page", defaultValue = "1") final int page) {
        ModelAndView mav = new ModelAndView("userToWatchMedia");
        User user = userService.getByUsername(username).orElseThrow(UserNotFoundException::new);
        int userId = user.getUserId();
        List<Media> toWatchMedia = mediaService.getById(watchService.getToWatchMediaId(userId, page - 1, itemsPerPage));
        Integer mediaCount = watchService.getToWatchMediaCount(userId).orElse(0);
        List<Media> suggestedMedia = mediaService.getMediaList(page - 1, itemsPerPage);
        final Integer suggestedMediaCount = mediaService.getMediaCount().orElse(0);
        mav.addObject("suggestedMedia", suggestedMedia);
        mav.addObject("suggestedMediaPages", (int) Math.ceil((double) suggestedMediaCount / itemsPerPage));
        mav.addObject("mediaList", toWatchMedia);
        mav.addObject("mediaPages", (int) Math.ceil((double) mediaCount / itemsPerPage));
        mav.addObject("currentPage", page);
        mav.addObject("mediaCount", mediaCount);

        mav.addObject(user);
        return mav;
    }

    //TODO la idea de estos metodos es pasarle el form de user y que de ahi pueda obtener datos como el userId sin tener que llamar a la bd

    @RequestMapping("/{username}/watchedMedia")
    public ModelAndView userWatchedMedia(@PathVariable("username") final String username, @RequestParam(value = "page", defaultValue = "1") final int page) {
        ModelAndView mav = new ModelAndView("userWatchedMedia");
        User user = userService.getByUsername(username).orElseThrow(UserNotFoundException::new);
        int userId = user.getUserId();
        List<Media> watchedMedia = mediaService.getById(watchService.getWatchedMediaId(userId, page - 1, itemsPerPage));
        Integer mediaCount = watchService.getWatchedMediaCount(userId).orElse(0);
        mav.addObject("mediaList", watchedMedia);
        mav.addObject("mediaPages", (int) Math.ceil((double) mediaCount / itemsPerPage));
        mav.addObject("currentPage", page);
        mav.addObject(user);
        return mav;
    }


    @RequestMapping("/{username}/favoriteLists")
    public ModelAndView userFavoriteLists(@PathVariable("username") final String username, @RequestParam(value = "page", defaultValue = "1") final int page) {
        ModelAndView mav = new ModelAndView("userFavoriteLists");
        User user = userService.getByUsername(username).orElseThrow(UserNotFoundException::new);
        mav.addObject(user);
        List<Integer> userFavListsId = favoriteService.getUserFavoriteLists(user.getUserId(), page - 1, itemsPerPage);
        List<ListCover> favoriteCovers = getListCover(listsService.getMediaListById(userFavListsId), listsService, mediaService);
        Integer favCount = favoriteService.getFavoriteMediaCount(user.getUserId()).orElse(0);
        mav.addObject("favoriteLists", favoriteCovers);
        mav.addObject("currentPage", page);
        mav.addObject("listsPages", (int) Math.ceil((double) favCount / itemsPerPage));
        mav.addObject("listsAmount", favCount);
        return mav;
    }

    @RequestMapping(value = "/settings", method = {RequestMethod.GET})
    public ModelAndView editUserDetails(@ModelAttribute("userSettings") final UserForm form){
        ModelAndView mav = new ModelAndView("userSettings");
        User u = userService.getCurrentUser().orElseThrow(UserNotFoundException::new);
        mav.addObject("user", u);
        return mav;
    }

    @RequestMapping(value = "/settings", method = {RequestMethod.POST})
    public ModelAndView postUserSettings(@Valid @ModelAttribute("userSettings") final UserForm form, final BindingResult errors) {
        if (errors.hasErrors())
            return editUserDetails(form);
        return new ModelAndView("redirect:/"+form.getUsername());
    }

}
