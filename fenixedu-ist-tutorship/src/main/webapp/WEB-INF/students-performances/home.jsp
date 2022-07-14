<%--

    Copyright © 2017 Instituto Superior Técnico

    This file is part of FenixEdu Academic.

    FenixEdu Academic is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FenixEdu Academic is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FenixEdu Academic.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers"
	prefix="fr"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ page trimDirectiveWhitespaces="true"%>
<%@ page
	import="org.fenixedu.academic.domain.degreeStructure.CurricularStage"%>

${portal.angularToolkit()}


<div class="page-header">
	<h1>
		<spring:message code="tilte.students.with.low.performance"
			text="Students Performance" />
	</h1>
</div>

<spring:url var="downloadUrl"
	value="/students-performances/download">
	<spring:param name="executionId" value="executionId" />
</spring:url>

<section>
	<form class="form-horizontal" id="download" role="form"  method="GET"  action="${downloadUrl}">
		${csrf.field()}

		
		<div class="form-group">
			<label for="executionYear" class="control-label col-sm-1"> 
			   <spring:message code="label.curricularYear" text="Curricular Year" />
			</label>
			<div class="col-sm-2">
				<select class="form-control" name="executionId" id="executionId">
					<option label="${i18n.message('label.choose.executionYear')}" value="" selected="selected" />
					<c:forEach var="executionYear" items="${executionYears}">					
						<option value="${executionYear.externalId}"><c:out value="${executionYear.year}"></c:out>
					</option>
					</c:forEach>
				</select>
			</div>
		</div>
	
		<div class="form-group">
			<div class="col-sm-offset-1 col-sm-2">
				<button type="submit" class="btn btn-default" id="download">
					<spring:message code="label.download" />
				</button>
			</div>
		</div>
	</form>
</section>



