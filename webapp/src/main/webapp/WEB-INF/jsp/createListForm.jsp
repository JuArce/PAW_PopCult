<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <jsp:include page="/resources/externalResources.jsp"/>
    <!-- favicon -->
    <link rel="shortcut icon" href="<c:url value='/resources/images/favicon.ico'/>" type="image/x-icon">
    <title>Create a new list &#8226; PopCult</title>
</head>
<body>
<div class="flex flex-col h-screen bg-gray-50">
    <jsp:include page="/WEB-INF/jsp/components/navbar.jsp"/>
    <c:url value="/createList" var="postPath"/>
    <div class="flex-grow col-8 offset-2">
        <form:form modelAttribute="createListForm" action="${postPath}" method="post" class="row g-3 p-2 my-8 bg-white shadow-lg">
            <h2 class="font-bold text-2xl">Step 1: Create a new list</h2>
            <div class="col-md-6">
                <div>
                    <form:label path="listTitle" for="listName" class="form-label">Name of the list</form:label>
                    <form:input path="listTitle" type="text" class="form-control focus:outline-none focus:ring focus:border-purple-300"
                                id="listName"/>
                    <form:errors path="listTitle" cssClass="formError text-red-500" element="p"/>
                </div>
            </div>
            <div>
                <div>
                    <form:label path="description" for="listDesc" class="form-label">Description</form:label>
                    <form:textarea path="description" type="text" class="form-control h-24 resize-y overflow-clip overflow-auto" id="listDesc"
                                   value=""/>
                    <form:errors path="description" cssClass="formError text-red-500" element="p"/>
                </div>
            </div>
            <div class="col-md-4">
                <div class="form-check">
                    <form:checkbox path="visible" class="form-check-label" for="invalidCheck2"/>
                    Make list public for everyone.
                </div>
            </div>
            <div class="col-md-6">
                <div class="form-check p-0">
                    <form:checkbox path="collaborative" class="form-check-label" for="invalidCheck3"/>
                    Enable others to suggest new movies to add.
                </div>
            </div>
            <div class="flex justify-between px-4">
                <a href=<c:url value="/lists"/>>
                    <button type="button"
                            class="btn btn-warning btn btn-danger bg-gray-300 hover:bg-red-400 text-gray-700 font-semibold hover:text-white">
                        Discard changes
                    </button>
                </a>
                <button class="row btn btn-secondary py-2" type="submit">Create list and continue to Step 2</button>
            </div>
        </form:form>
    </div>
    <jsp:include page="/WEB-INF/jsp/components/footer.jsp"/>
</div>
</body>
</html>
