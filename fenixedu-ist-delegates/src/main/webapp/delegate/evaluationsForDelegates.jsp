<%--

    Copyright © 2013 Instituto Superior Técnico

    This file is part of FenixEdu IST Delegates.

    FenixEdu IST Delegates is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FenixEdu IST Delegates is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FenixEdu IST Delegates.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://fenixedu.org/taglib/jsf-portal" prefix="fp"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/taglib/jsf-fenix" prefix="fc"%>

<fp:select actionClass="pt.ist.fenixedu.delegates.ui.struts.DelegateApplication$EvaluationsForDelegatesAction" />

<f:view>
	<f:loadBundle basename="resources/HtmlaltResources" var="htmlAltBundle"/>
	<f:loadBundle basename="resources/DelegateResources" var="delegatesBundle"/>
	<f:loadBundle basename="resources/ApplicationResources" var="bundleApplication"/>
	<f:loadBundle basename="resources/EnumerationResources" var="bundleEnum"/>

	<h:form>
		<h:outputText escape="false" value="<input alt='input.degreeID' id='degreeID' name='degreeID' type='hidden' value='#{evaluationsForDelegates.degreeID}'/>"/>

		<h:outputText value="<h2>" escape="false"/>
		<h:outputText value="#{delegatesBundle['label.delegates.degreeEvaluations']}"/>
		<h:outputText value="</h2>" escape="false"/>

		<h:outputText rendered="#{empty evaluationsForDelegates.degree.mostRecentDegreeCurricularPlan}" value="<p><em>#{bundleApplication['error.curricularPlanHasNoExecutionDegreesInNotClosedYears']}</em></p>" escape="false"/>

		<h:panelGroup rendered="#{!empty evaluationsForDelegates.degree.mostRecentDegreeCurricularPlan}">
			<h:panelGrid styleClass="tstyle5" columns="2">
				<h:outputText value="#{delegatesBundle['label.delegates.evaluations.curricularPlan']}: "/>
				<h:panelGroup>
					<h:selectOneMenu id="degreeCurricularPlanID" value="#{evaluationsForDelegates.degreeCurricularPlanID}"
							onchange="this.form.submit();">
						<f:selectItems value="#{evaluationsForDelegates.degreeCurricularPlanSelectItems}"/>
					</h:selectOneMenu>
				</h:panelGroup>

				<h:outputText value="#{delegatesBundle['label.delegates.evaluations.execution.period']}: "/>
				<h:panelGroup>
					<h:selectOneMenu id="executionPeriodID" value="#{evaluationsForDelegates.executionPeriodID}"
							onchange="this.form.submit();">
						<f:selectItems value="#{evaluationsForDelegates.executionPeriodSelectItems}"/>
					</h:selectOneMenu>
				</h:panelGroup>

				<h:outputText value="#{delegatesBundle['label.delegates.evaluations.curricular.year']}: "/>
				<h:panelGroup>
					<h:selectOneMenu id="curricularYearID" value="#{evaluationsForDelegates.curricularYearID}"
							onchange="this.form.submit();">
						<f:selectItem itemLabel="#{delegatesBundle['label.delegates.evaluations.allCurricularYears']}" itemValue=""/>
						<f:selectItems value="#{evaluationsForDelegates.curricularYearSelectItems}"/>
					</h:selectOneMenu>
				</h:panelGroup>
			</h:panelGrid>

			<h:outputText value="<br/>" escape="false"/>

		 	<fc:fenixCalendar
			 		begin="#{evaluationsForDelegates.beginDate}"
			 		end="#{evaluationsForDelegates.endDate}"
			 		editLinkPage="#{evaluationsForDelegates.applicationContext}/publico/executionCourse.do"
			 		editLinkParameters="#{evaluationsForDelegates.calendarLinks}"/>
		</h:panelGroup>
	</h:form>
</f:view>
