<%@taglib prefix="c" uri="http://java.sun.com/jstl/core_rt" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<html>
<head>
    <jsp:include page="/resources/externalResources.jsp"/>
    <!-- favicon -->
    <link rel="shortcut icon" href="<c:url value='/resources/images/favicon.ico'/>" type="image/x-icon">
    <title><c:out value="${list.listName}"/> &#8226; PopCult</title>
</head>
<c:url value="/lists/${list.mediaListId}" var="forkPath"/>
<c:url value="/lists/edit/${list.mediaListId}/manageMedia" var="editListMediaPath"/>
<body class="bg-gray-50">
<div class="flex flex-col min-h-screen">
    <jsp:include page="/WEB-INF/jsp/components/navbar.jsp"/>
    <div class="col-8 offset-2 flex-grow">
        <div class="flex justify-content-start content-center pt-4">
            <div class="col-md-auto">
                <h2 class="display-5 fw-bolder"><c:out value="${list.listName}"/></h2>
                <h4 class="py-2 pb-2.5"><a class="hover:text-gray-800" href="<c:url value="/user/${user.username}"/>">
                    by: <b><c:out value="${user.username}"/></b>
                </a></h4>
            </div>
            <div class="pt-2.5">
                <jsp:include page="/WEB-INF/jsp/components/favorite.jsp">
                    <jsp:param name="URL" value="/lists/${list.mediaListId}"/>
                    <jsp:param name="favorite" value="${isFavoriteList}"/>
                </jsp:include>
            </div>
        </div>
        <p class="lead text-justify pb-2"><c:out value="${list.description}"/></p>
        <!-- Media icons -->
        <div class="flex flex-wrap justify-start">
            <jsp:include page="/WEB-INF/jsp/components/share.jsp"/>
            <div class="flex justify-center py-2">
                <c:choose>
                    <c:when test="${list.userId == currentUser.userId}">
                        <a href="${editListMediaPath}">
                            <button type="button" class="btn btn-link text-purple-500 hover:text-purple-900 btn-rounded">
                                <i class="far fa-edit pr-2 text-purple-500 hover:text-purple-900"></i>
                                Edit list
                            </button>
                        </a>
                    </c:when>
                    <c:otherwise>
                        <form:form cssClass="m-0" action="${forkPath}" method="POST">
                            <button type="submit" id="fork" name="fork" class="btn btn-link text-purple-500 hover:text-purple-900 btn-rounded">
                                <i class="far fa-copy pr-2 text-purple-500 hover:text-purple-900"></i>
                                Fork this list
                            </button>
                        </form:form>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
        <!-- Films and Series in the list -->
        <div class="row">
            <c:forEach var="media" items="${media}">
                <div class="col-12 col-sm-12 col-md-6 col-lg-4 col-xl-3 py-2">
                    <jsp:include page="/WEB-INF/jsp/components/card.jsp">
                        <jsp:param name="image" value="${media.image}"/>
                        <jsp:param name="title" value="${media.title}"/>
                        <jsp:param name="releaseDate" value="${media.releaseYear}"/>
                        <jsp:param name="mediaId" value="${media.mediaId}"/>
                    </jsp:include>
                </div>
            </c:forEach>
        </div>
    </div>
    <jsp:include page="/WEB-INF/jsp/components/footer.jsp"/>
</div>
</body>
</html>
