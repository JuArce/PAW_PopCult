package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.interfaces.*;
import ar.edu.itba.paw.interfaces.exceptions.*;
import ar.edu.itba.paw.models.PageContainer;
import ar.edu.itba.paw.models.collaborative.Request;
import ar.edu.itba.paw.models.comment.Notification;
import ar.edu.itba.paw.models.lists.ListCover;
import ar.edu.itba.paw.models.lists.MediaList;
import ar.edu.itba.paw.models.media.Media;
import ar.edu.itba.paw.models.media.WatchedMedia;
import ar.edu.itba.paw.models.user.Token;
import ar.edu.itba.paw.models.user.User;
import ar.edu.itba.paw.webapp.dto.input.UserCreateDto;
import ar.edu.itba.paw.webapp.dto.output.ErrorDto;
import ar.edu.itba.paw.webapp.dto.output.MediaFavoriteDto;
import ar.edu.itba.paw.webapp.dto.output.UserDto;
import ar.edu.itba.paw.webapp.exceptions.*;
import ar.edu.itba.paw.webapp.form.*;
import ar.edu.itba.paw.webapp.utilities.ResponseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import static ar.edu.itba.paw.webapp.utilities.ListCoverImpl.getListCover;

@Path("users")
@Component
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
    @Autowired
    private CollaborativeListService collaborativeListService;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private CommentService commentService;

    @Autowired
    private MessageSource messageSource;

    @Context
    private UriInfo uriInfo;

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    private static final int listsPerPage = 4;
    private static final int itemsPerPage = 4;
    private static final int notificationsPerPage = 16;
    private static final int editablePerPage = 12;

    private static final String defaultPage = "1";
    private static final String defaultPageSize = "12";


    @GET
    @Produces(value = {
            MediaType.APPLICATION_JSON
    })
    public Response listUsers(@QueryParam("page") @DefaultValue(defaultPage) int page,
                              @QueryParam("page-size") @DefaultValue(defaultPageSize) int pageSize) {
        if (page < 1 || pageSize < 1) {
            LOGGER.info("GET /users: Invalid params.");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        final PageContainer<User> users = userService.getUsers(page, pageSize);

        if (users.getElements().isEmpty()) {
            LOGGER.info("GET /users: Returning empty list.");
            return Response.noContent().build();
        }
        final List<UserDto> usersDto = UserDto.fromUserList(uriInfo, users.getElements());
        final Response.ResponseBuilder response = Response.ok(new GenericEntity<List<UserDto>>(usersDto) {
        });
        ResponseUtils.setPaginationLinks(response, users, uriInfo);
        LOGGER.info("GET /users: Returning page {} with {} results.", users.getCurrentPage(), users.getElements().size());
        return response.build();
    }

    @GET
    @Path("/{username}")
    @Produces(value = {
            MediaType.APPLICATION_JSON
    })
    public Response getUser(@PathParam("username") String username) {
        Optional<User> user = userService.getByUsername(username);

        if (!user.isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(UserDto.fromUser(uriInfo, user.get())).build();
    }

    @POST
    @Produces(value = {MediaType.APPLICATION_JSON})
    @Consumes(value = {MediaType.APPLICATION_JSON})
    public Response createUser(@Valid UserCreateDto userDto) {
        if (userDto == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        final User user;
        try {
            user = userService.register(userDto.getEmail(), userDto.getUsername(), userDto.getPassword(), userDto.getName());
        } catch (UsernameAlreadyExistsException e) {
            final ErrorDto errorDto = ErrorDto.fromErrorMsg(messageSource.getMessage("validation.username.alreadyExists", null, Locale.getDefault()), "username");
            return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<ErrorDto>(errorDto) {
            }).build();
        } catch (EmailAlreadyExistsException e) {
            final ErrorDto errorDto = ErrorDto.fromErrorMsg(messageSource.getMessage("validation.email.alreadyExists", null, Locale.getDefault()), "email");
            return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<ErrorDto>(errorDto) {
            }).build();
        }

        return Response.created(uriInfo.getAbsolutePathBuilder().path(String.valueOf(user.getUserId())).build()).build();
    }

    @DELETE
    @Path("/{username}")
    @Produces(value = {MediaType.APPLICATION_JSON})
    public Response deleteUser(@PathParam("username") String username) {
        Optional<User> user = userService.getByUsername(username);

        if (!user.isPresent()) {
            LOGGER.info("DELETE /users/{username}: {} does not exists", username);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        userService.deleteUser(user.get());
        LOGGER.info("DELETE /users/{username}: {} user deleted", username);
        return Response.noContent().build();
    }

    /**
     * Favorite Media
     */
    @GET
    @Path("/{username}/favorite-media")
    @Produces(value = {MediaType.APPLICATION_JSON})
    public Response getUserFavoriteMedia(@PathParam("username") String username,
                                         @QueryParam("page") @DefaultValue(defaultPage) int page,
                                         @QueryParam("page-size") @DefaultValue(defaultPageSize) int pageSize) {
        if (page < 1 || pageSize < 1) {
            LOGGER.info("GET /users/{}/favorite-media: Invalid params.", username);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Optional<User> user = userService.getByUsername(username);

        if (!user.isPresent()) {
            LOGGER.info("GET /users/{}/favorite-media: {} does not exists.", username, username);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        PageContainer<Media> favoriteMedia = favoriteService.getUserFavoriteMedia(user.get(), page, pageSize);

        if (favoriteMedia.getElements().isEmpty()) {
            LOGGER.info("GET /users/{}/favorite-media: Returning empty list.", username);
            return Response.noContent().build();
        }

        final List<MediaFavoriteDto> mediaFavoriteDtoList = MediaFavoriteDto.fromMediaList(uriInfo, favoriteMedia.getElements(), user.get());
        final Response.ResponseBuilder response = Response.ok(new GenericEntity<List<MediaFavoriteDto>>(mediaFavoriteDtoList) {
        });
        ResponseUtils.setPaginationLinks(response, favoriteMedia, uriInfo);
        LOGGER.info("GET /users/{}/favorite-media: Returning page {} with {} results.", username, favoriteMedia.getCurrentPage(), favoriteMedia.getElements().size());
        return response.build();
    }

    @GET
    @Path("/{username}/favorite-media/{mediaId}")
    @Produces(value = {MediaType.APPLICATION_JSON})
    public Response isFavoriteMedia(@PathParam("username") String username,
                                    @PathParam("mediaId") int mediaId) {
        Optional<User> user = userService.getByUsername(username);
        Optional<Media> media = mediaService.getById(mediaId);

        if(!user.isPresent() || !media.isPresent()) {
            LOGGER.info("GET /users/{}/favorite-media/{}: Invalid params.", username, mediaId);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if(!favoriteService.isFavorite(media.get(), user.get())) {
            LOGGER.info("GET /users/{}/favorite-media/{}: media {} is not favorite of {}.", username, mediaId, mediaId, username);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        LOGGER.info("GET /users/{}/favorite-media/{}: media {} is favorite of {}.", username, mediaId, mediaId, username);
        return Response.ok(MediaFavoriteDto.fromMediaAndUser(uriInfo, media.get(), user.get())).build();
    }

    @PUT
    @Path("/{username}/favorite-media/{mediaId}")
    @Produces(value = {MediaType.APPLICATION_JSON})
    public Response addMediaToFavorites(@PathParam("username") String username,
                                    @PathParam("mediaId") int mediaId) {
        Optional<User> user = userService.getByUsername(username);
        Optional<Media> media = mediaService.getById(mediaId);

        if(!user.isPresent() || !media.isPresent()) {
            LOGGER.info("PUT /users/{}/favorite-media/{}: Invalid params.", username, mediaId);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        favoriteService.addMediaToFav(media.get(), user.get());

        LOGGER.info("PUT /users/{}/favorite-media/{}: media {} added to {}'s favorites.", username, mediaId, mediaId, username);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{username}/favorite-media/{mediaId}")
    @Produces(value = {MediaType.APPLICATION_JSON})
    public Response removeMediaFromFavorites(@PathParam("username") String username,
                                        @PathParam("mediaId") int mediaId) {
        Optional<User> user = userService.getByUsername(username);
        Optional<Media> media = mediaService.getById(mediaId);

        if(!user.isPresent() || !media.isPresent()) {
            LOGGER.info("DELETE /users/{}/favorite-media/{}: Invalid params.", username, mediaId);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        favoriteService.deleteMediaFromFav(media.get(), user.get());

        LOGGER.info("DELETE /users/{}/favorite-media/{}: media {} removed from {}'s favorites.", username, mediaId, mediaId, username);
        return Response.noContent().build();
    }

//    @RequestMapping("/user/{username}")
//    public ModelAndView userProfile(@ModelAttribute("imageForm") final ImageForm imageForm,
//                                    @PathVariable("username") final String username,
//                                    @RequestParam(value = "page", defaultValue = "1") final int page) {
//        ModelAndView mav = new ModelAndView("user/userProfile");
//        LOGGER.debug("Trying to access {} profile", username);
//        User user = userService.getByUsername(username).orElseThrow(UserNotFoundException::new);
//        PageContainer<MediaList> userLists = listsService.getMediaListByUser(user, page - 1, listsPerPage);
//        final List<ListCover> userListsCover = getListCover(userLists.getElements(), listsService);
//        mav.addObject("user", user);
//        mav.addObject("lists", userListsCover);
//        mav.addObject("userListsContainer", userLists);
//
//        PageContainer<MediaList> userPublicLists = listsService.getPublicMediaListByUser(user, page - 1, listsPerPage);
//        final List<ListCover> userPublicListCover = getListCover(userPublicLists.getElements(), listsService);
//        mav.addObject("userPublicListCover", userPublicListCover);
//        mav.addObject("userPublicLists", userPublicLists);
//        LOGGER.info("{} profile accessed.", username);
//        return mav;
//    }
//
//    @RequestMapping("/user/{username}/favoriteMedia")
//    public ModelAndView userFavoriteMedia(@ModelAttribute("imageForm") final ImageForm imageForm,
//                                          @PathVariable("username") final String username,
//                                          @RequestParam(value = "page", defaultValue = "1") final int page) {
//        LOGGER.debug("Trying to access {} favorite media.", username);
//        ModelAndView mav = new ModelAndView("user/userFavoriteMedia");
//        User user = userService.getByUsername(username).orElseThrow(UserNotFoundException::new);
//        PageContainer<Media> favoriteMedia = favoriteService.getUserFavoriteMedia(user, page - 1, itemsPerPage);
//        PageContainer<Media> suggestedMedia = favoriteService.getMostLikedMedia(page - 1, itemsPerPage);
//        mav.addObject("user", user);
//        mav.addObject("favoriteMediaContainer", favoriteMedia);
//        mav.addObject("suggestedMediaContainer", suggestedMedia);
//
//        LOGGER.info("{} favorite media accessed.", username);
//        return mav;
//    }
//
//    @RequestMapping("/user/{username}/toWatchMedia")
//    public ModelAndView userToWatchMedia(@ModelAttribute("imageForm") final ImageForm imageForm,
//                                         @PathVariable("username") final String username,
//                                         @RequestParam(value = "page", defaultValue = "1") final int page) {
//        LOGGER.debug("Trying to access {} to watch media.", username);
//        ModelAndView mav = new ModelAndView("user/userToWatchMedia");
//        User user = userService.getByUsername(username).orElseThrow(UserNotFoundException::new);
//        PageContainer<Media> toWatchMediaIds = watchService.getToWatchMediaId(user, page - 1, itemsPerPage);
//        PageContainer<Media> suggestedMedia = favoriteService.getMostLikedMedia(page - 1, itemsPerPage);
//
//        mav.addObject("user", user);
//        mav.addObject("toWatchMediaIdsContainer", toWatchMediaIds);
//        mav.addObject("suggestedMediaContainer", suggestedMedia);
//
//        LOGGER.info("{} to watch media accessed.", username);
//        return mav;
//    }
//
//
//    @RequestMapping("/user/{username}/watchedMedia")
//    public ModelAndView userWatchedMedia(@ModelAttribute("imageForm") final ImageForm imageForm,
//                                         @PathVariable("username") final String username,
//                                         @RequestParam(value = "page", defaultValue = "1") final int page) {
//        LOGGER.debug("Trying to access {} watched media.", username);
//        ModelAndView mav = new ModelAndView("user/userWatchedMedia");
//        User user = userService.getByUsername(username).orElseThrow(UserNotFoundException::new);
//        PageContainer<WatchedMedia> watchedMediaIds = watchService.getWatchedMediaId(user, page - 1, itemsPerPage);
//
//        mav.addObject("user", user);
//        mav.addObject("watchedMediaIdsContainer", watchedMediaIds);
//        mav.addObject("currentDate", LocalDate.now());
//        LOGGER.info("{} watched media accessed.", username);
//        return mav;
//    }
//
//
//    @RequestMapping("/user/{username}/favoriteLists")
//    public ModelAndView userFavoriteLists(@ModelAttribute("imageForm") final ImageForm imageForm,
//                                          @PathVariable("username") final String username,
//                                          @RequestParam(value = "page", defaultValue = "1") final int page) {
//        LOGGER.debug("Trying to access {} favorite lists.", username);
//        ModelAndView mav = new ModelAndView("user/userFavoriteLists");
//        User user = userService.getByUsername(username).orElseThrow(UserNotFoundException::new);
//        mav.addObject(user);
//
//        PageContainer<MediaList> userFavLists = favoriteService.getUserFavoriteLists(user, page - 1, itemsPerPage);
//        List<ListCover> favoriteCovers = getListCover(userFavLists.getElements(), listsService);
//        mav.addObject("favoriteLists", favoriteCovers);
//        mav.addObject("userFavListsContainer", userFavLists);
//
//        PageContainer<MediaList> userPublicFavLists = favoriteService.getUserPublicFavoriteLists(user, page - 1, listsPerPage);
//        final List<ListCover> userPublicFavListCover = getListCover(userPublicFavLists.getElements(), listsService);
//        mav.addObject("userPublicListCover", userPublicFavListCover);
//        mav.addObject("userPublicLists", userPublicFavLists);
//
//        LOGGER.info("{} favorite lists accessed.", username);
//        return mav;
//    }
//
//    @RequestMapping(value = "/settings", method = {RequestMethod.GET})
//    public ModelAndView editUserDetails(@ModelAttribute("userSettings") final UserDataForm form) {
//        ModelAndView mav = new ModelAndView("user/userSettings");
//        LOGGER.debug("Trying to access {} details.", form.getUsername());
//        User user = userService.getCurrentUser().orElseThrow(UserNotFoundException::new);
//        LOGGER.info("{} getting details.", user.getUsername());
//        mav.addObject("user", user);
//        return mav;
//    }
//
//    @RequestMapping(value = "/settings", method = {RequestMethod.POST}, params = "editUser")
//    public ModelAndView postUserSettings(@Valid @ModelAttribute("userSettings") final UserDataForm form,
//                                         final BindingResult errors) {
//        LOGGER.debug("{} trying to update settings", form.getUsername());
//        if (errors.hasErrors()) {
//            LOGGER.error("Form used for user details has errors.");
//            return editUserDetails(form);
//        }
//        User user = userService.getCurrentUser().orElseThrow(UserNotFoundException::new);
//        userService.updateUserData(user, form.getName());
//        LOGGER.info("User {}'s details updated.", user.getUsername());
//        return new ModelAndView("redirect:/user/" + user.getUsername());
//    }
//
//    @RequestMapping(value = "/changePassword", method = {RequestMethod.GET})
//    public ModelAndView changeUserPassword(@ModelAttribute("changePassword") final PasswordForm form) {
//        ModelAndView mav = new ModelAndView("login/changePassword");
//        User user = userService.getCurrentUser().orElseThrow(UserNotFoundException::new);
//        mav.addObject("user", user);
//        return mav;
//    }
//
//    @RequestMapping(value = "/changePassword", method = {RequestMethod.POST}, params = "changePass")
//    public ModelAndView postUserPassword(@Valid @ModelAttribute("changePassword") final PasswordForm form,
//                                         final BindingResult errors) {
//        LOGGER.debug("Trying to change password");
//        if (errors.hasErrors()) {
//            LOGGER.error("Change password form has errors.");
//            return changeUserPassword(form);
//        }
//        User user = userService.getCurrentUser().orElseThrow(UserNotFoundException::new);
//        try {
//            userService.changePassword(user, form.getCurrentPassword(), form.getNewPassword());
//        } catch (InvalidCurrentPasswordException e) {
//            LOGGER.error("Changing password failed.");
//            errors.rejectValue("currentPassword", "validation.email.wrongCurrentPassword");
//            return changeUserPassword(form);
//        }
//        LOGGER.info("{} changed password.", user.getUsername());
//        return new ModelAndView("redirect:/user/" + user.getUsername());
//    }
//
//    @RequestMapping(value = "/forgotPassword", method = {RequestMethod.GET})
//    public ModelAndView forgotPasswordForm(@ModelAttribute("emailForm") final EmailForm emailForm) {
//        return new ModelAndView("login/forgotPassword");
//    }
//
//    @RequestMapping(value = "/forgotPassword", method = {RequestMethod.POST})
//    public ModelAndView forgotPassword(@Valid @ModelAttribute("emailForm") final EmailForm emailForm,
//                                       final BindingResult errors) {
//        LOGGER.debug("{} initializing process to recover password.", emailForm.getEmail());
//        if (errors.hasErrors()) {
//            LOGGER.error("Email form has errors.");
//            return forgotPasswordForm(emailForm);
//        }
//        try {
//            userService.forgotPassword(emailForm.getEmail());
//        } catch (EmailNotExistsException e) {
//            LOGGER.info("{} does not exist. Recovery failed.", emailForm.getEmail());
//            errors.rejectValue("email", "forgotPassword.emailNotExists");
//            return forgotPasswordForm(emailForm);
//        }
//        LOGGER.info("Forgot password email sent to {}.", emailForm.getEmail());
//        return new ModelAndView("login/sentEmail");
//    }
//
//    @RequestMapping(value = "resetPassword", method = {RequestMethod.GET})
//    public ModelAndView resetPasswordForm(@ModelAttribute("resetPasswordForm") final ResetPasswordForm resetPasswordForm,
//                                          @RequestParam(value = "token", defaultValue = "") final String token) {
//        return new ModelAndView("login/resetPassword");
//    }
//
//    @RequestMapping(value = "resetPassword", method = {RequestMethod.POST})
//    public ModelAndView resetPassword(@Valid @ModelAttribute("resetPasswordForm") final ResetPasswordForm resetPasswordForm,
//                                      final BindingResult errors,
//                                      @RequestParam(value = "token", defaultValue = "") final String token) {
//        LOGGER.debug("Trying to rest password.");
//        if (errors.hasErrors()) {
//            LOGGER.error("Reset password form has errors.");
//            return resetPasswordForm(resetPasswordForm, token);
//        }
//        Token resetPasswordToken = tokenService.getToken(token).orElseThrow(TokenNotFoundException::new);
//        if (userService.resetPassword(resetPasswordToken, resetPasswordForm.getNewPassword())) {
//            LOGGER.info("Password reset was successful.");
//            return new ModelAndView("login/login");
//        }
//        LOGGER.info("Token timed out.");
//        return new ModelAndView("redirect:/tokenTimedOut?token=" + token);
//    }
//
//    @RequestMapping("/deleteUser")
//    public ModelAndView deleteUser() {
//        User user = userService.getCurrentUser().orElseThrow(UserNotFoundException::new);
//        userService.deleteUser(user);
//        LOGGER.info("{} user deleted successfully", user.getUsername());
//        return new ModelAndView("redirect:/logout");
//    }
//
//
//    @RequestMapping(value = "/user/{username}", method = {RequestMethod.POST}, params = "uploadImage")
//    public ModelAndView uploadProfilePicture(@PathVariable("username") final String username,
//                                             @Valid @ModelAttribute("imageForm") final ImageForm imageForm,
//                                             final BindingResult error) throws IOException {
//        LOGGER.debug("{} trying to upload profile picture", username);
//        User user = userService.getByUsername(username).orElseThrow(UserNotFoundException::new);
//        if (error.hasErrors()) {
//            LOGGER.error("Uploading profile picture failed.");
//            return userProfile(imageForm, username, 1).addObject("errorUploadingImage", true);
//        }
//        userService.uploadUserProfileImage(user, imageForm.getImage().getBytes());
//        LOGGER.info("{} profile picture uploaded successfully", username);
//        return new ModelAndView("redirect:/user/" + username);
//    }
//
//    @RequestMapping(value = "/user/image/{imageId}", method = RequestMethod.GET, produces = "image/*")
//    public @ResponseBody
//    byte[] getProfileImage(@PathVariable("imageId") final Integer imageId) {
//        LOGGER.debug("Trying to access profile image");
//        byte[] profileImage = new byte[0];
//        try {
//            profileImage = userService.getUserProfileImage(imageId).orElseThrow(ImageNotFoundException::new).getImageBlob();
//        } catch (ImageConversionException e) {
//            LOGGER.error("Error loading image {}", imageId);
//        }
//        LOGGER.info("Profile image accessed.");
//        return profileImage;
//    }
//
//    @RequestMapping(value = "/user/{username}/watchedMedia", method = {RequestMethod.POST}, params = "watchedDate")
//    public ModelAndView editWatchedDate(@PathVariable("username") final String username,
//                                        @RequestParam("watchedDate") String watchedDate,
//                                        @RequestParam("userId") int userId,
//                                        @RequestParam("mediaId") int mediaId) {
//        LOGGER.debug("{} is trying to edit watch date", username);
//        Media media = mediaService.getById(mediaId).orElseThrow(MediaNotFoundException::new);
//        User user = userService.getById(userId).orElseThrow(UserNotFoundException::new);
//        watchService.updateWatchedMediaDate(media, user, LocalDate.parse(watchedDate, DateTimeFormatter.ISO_DATE).atStartOfDay());
//        LOGGER.info("{} updated successfully watched date", username);
//        return new ModelAndView("redirect:/user/" + username + "/watchedMedia");
//    }
//
//    @RequestMapping("/user/{username}/requests")
//    public ModelAndView userCollabRequests(@PathVariable("username") final String username,
//                                           @RequestParam(value = "page", defaultValue = "1") final int page) {
//        LOGGER.debug("{} trying to access collaborations requests", username);
//        ModelAndView mav = new ModelAndView("user/userRequests");
//        User user = userService.getByUsername(username).orElseThrow(UserNotFoundException::new);
//        PageContainer<Request> requestContainer = collaborativeListService.getRequestsByUserId(user, page - 1, itemsPerPage * 4);
//        mav.addObject("username", username);
//        mav.addObject("requestContainer", requestContainer);
//        LOGGER.info("{} accessed succesfully to collaborations requests", username);
//        return mav;
//    }
//
//    @RequestMapping("/user/{username}/requests/accept")
//    public ModelAndView acceptCollabRequests(@PathVariable("username") final String username,
//                                             @RequestParam("collabId") final int collabId) {
//        LOGGER.debug("{} trying to accept collab request", username);
//        Request collab = collaborativeListService.getById(collabId).orElseThrow(RequestNotFoundException::new);
//        collaborativeListService.acceptRequest(collab);
//        LOGGER.info("{} collab request accepted", username);
//        return new ModelAndView("redirect:/user/" + username + "/requests");
//    }
//
//    @RequestMapping("/user/{username}/requests/reject")
//    public ModelAndView rejectCollabRequests(@PathVariable("username") final String username,
//                                             @RequestParam("collabId") final int collabId) {
//        LOGGER.debug("{} trying to reject collab request", username);
//        Request collab = collaborativeListService.getById(collabId).orElseThrow(RequestNotFoundException::new);
//        collaborativeListService.rejectRequest(collab);
//        LOGGER.info("{} collab request rejected", username);
//        return new ModelAndView("redirect:/user/" + username + "/requests");
//    }
//
//    @RequestMapping("user/{username}/lists")
//    public ModelAndView userEditableLists(@PathVariable("username") final String username,
//                                          @RequestParam(value = "page", defaultValue = "1") final int page) {
//        LOGGER.debug("{} trying to access editable lists", username);
//        ModelAndView mav = new ModelAndView("user/userEditableLists");
//        User user = userService.getByUsername(username).orElseThrow(UserNotFoundException::new);
//        PageContainer<MediaList> editableLists = listsService.getUserEditableLists(user, page - 1, editablePerPage);
//        final List<ListCover> editableCovers = getListCover(editableLists.getElements(), listsService);
//        mav.addObject("user", user);
//        mav.addObject("listContainer", editableLists);
//        mav.addObject("covers", editableCovers);
//        LOGGER.info("{} accessed editable lists", username);
//        return mav;
//    }
//
//    @RequestMapping("user/{username}/notifications")
//    public ModelAndView userNotifications(@PathVariable("username") final String username,
//                                          @RequestParam(value = "page", defaultValue = "1") final int page) {
//        LOGGER.debug("{} trying to access notifications", username);
//        ModelAndView mav = new ModelAndView("user/userNotifications");
//        User user = userService.getByUsername(username).orElseThrow(UserNotFoundException::new);
//        PageContainer<Notification> notificationContainer = commentService.getUserListsCommentsNotifications(user, page - 1, notificationsPerPage);
//        mav.addObject("username", username);
//        mav.addObject("notifications", notificationContainer);
//        LOGGER.info("{} accessed notifications", username);
//        return mav;
//    }
//
//    @RequestMapping(value = "user/{username}/notifications", method = {RequestMethod.POST}, params = "setOpen")
//    public ModelAndView setNotificationsAsOpen(@PathVariable("username") final String username) {
//        User user = userService.getByUsername(username).orElseThrow(UserNotFoundException::new);
//        commentService.setUserListsCommentsNotificationsAsOpened(user);
//        return new ModelAndView("redirect:/user/" + username + "/notifications");
//    }
//
//    @RequestMapping(value = "user/{username}/notifications", method = {RequestMethod.POST}, params = "deleteNotifications")
//    public ModelAndView deleteAllNotifications(@PathVariable("username") final String username) {
//        LOGGER.debug("{} trying to delete all notifications", username);
//        User user = userService.getByUsername(username).orElseThrow(UserNotFoundException::new);
//        commentService.deleteUserListsCommentsNotifications(user);
//        LOGGER.info("{} deleted all notifications successfully ", username);
//        return new ModelAndView("redirect:/user/" + username + "/notifications");
//    }
}
