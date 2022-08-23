/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Tutorship.
 *
 * FenixEdu IST Tutorship is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Tutorship is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Tutorship.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.tutorship.ui.Action.pedagogicalCouncil;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.EntryPoint;
import org.fenixedu.bennu.struts.portal.StrutsFunctionality;
import org.fenixedu.commons.StringNormalizer;
import org.fenixedu.commons.spreadsheet.StyledExcelSpreadsheet;

import pt.ist.fenixWebFramework.rendererExtensions.converters.DomainObjectKeyConverter;
import pt.ist.fenixWebFramework.renderers.DataProvider;
import pt.ist.fenixWebFramework.renderers.components.converters.Converter;
import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;
import pt.ist.fenixedu.tutorship.domain.Tutorship;
import pt.ist.fenixedu.tutorship.domain.TutorshipIntention;
import pt.ist.fenixedu.tutorship.ui.TutorshipApplications.TutorshipApp;
import pt.ist.fenixframework.FenixFramework;

@StrutsFunctionality(app = TutorshipApp.class, path = "view-tutors", titleKey = "title.tutorship.view")
@Mapping(path = "/viewTutors", module = "pedagogicalCouncil")
@Forwards({ @Forward(name = "viewTutors", path = "/pedagogicalCouncil/tutorship/viewTutors.jsp"),
        @Forward(name = "viewStudentOfTutor", path = "/pedagogicalCouncil/tutorship/viewStudentsOfTutor.jsp") })
public class ViewTutorsDA extends FenixDispatchAction {

    @EntryPoint
    public ActionForward listTutors(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        ViewTutorsBean bean = getRenderedObject("tutorsBean");
        if (bean == null) {
            bean = new ViewTutorsBean();
        }
        request.setAttribute("tutorsBean", bean);
        RenderUtils.invalidateViewState();
        return mapping.findForward("viewTutors");
    }

    public ActionForward exportToExcel(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        
        ExecutionDegree executionDegree = getDomainObject(request, "executionDegree");
        ViewTutorsBean bean = new ViewTutorsBean();
        bean.setExecutionDegree(executionDegree);
        List<TutorshipIntention> tutorsList = bean.getTutors();

        StyledExcelSpreadsheet spreadsheet = new StyledExcelSpreadsheet("Tutores-Tutorandos", 15);
        spreadsheet.newHeaderRow();
        spreadsheet.addHeader("Tutor-Username");
        spreadsheet.addHeader("Tutor-Nome", 12500);
        spreadsheet.addHeader("Tutor-Email");
        spreadsheet.addHeader("Tutorando-Nº");
        spreadsheet.addHeader("Tutorando-Nome", 12500);
        spreadsheet.addHeader("Tutorando-Telemóvel", 7000);
        spreadsheet.addHeader("Tutorando-Email");
        spreadsheet.addHeader("Tutorando-Ciclo");

        for (TutorshipIntention tutorshipIntention : tutorsList) {
            if (tutorshipIntention.getTutorships().isEmpty()) {
                spreadsheet.newRow();
                spreadsheet.addCell(tutorshipIntention.getTeacher().getPerson().getUsername());
                spreadsheet.addCell(tutorshipIntention.getTeacher().getPerson().getName());
                spreadsheet.addCell(tutorshipIntention.getTeacher().getPerson().getEmailForSendingEmails());
            }

            for (Tutorship tutorship : tutorshipIntention.getTutorships()) {
                spreadsheet.newRow();
                spreadsheet.addCell(tutorshipIntention.getTeacher().getPerson().getUsername());
                spreadsheet.addCell(tutorshipIntention.getTeacher().getPerson().getName());
                spreadsheet.addCell(tutorshipIntention.getTeacher().getPerson().getEmailForSendingEmails());
                spreadsheet.addCell(tutorship.getStudent().getNumber());
                spreadsheet.addCell(tutorship.getStudent().getName());
                spreadsheet.addCell(tutorship.getStudent().getPerson().getDefaultMobilePhoneNumber());
                spreadsheet.addCell(tutorship.getStudent().getPerson().getDefaultEmailAddressValue());

                CycleType studentCycle = tutorship.getStudent().getCycleType(executionDegree.getExecutionYear());
                if (studentCycle != null) {
                    spreadsheet.addCell(studentCycle.getDescription());
                } else {
                    spreadsheet.addCell("N/A");
                }
            }
        }

        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename="
                + StringNormalizer.slugify(executionDegree.getExecutionYear().getQualifiedName() + " "
                        + executionDegree.getPresentationName())
                + ".xls");
        final ServletOutputStream writer = response.getOutputStream();
        spreadsheet.getWorkbook().write(writer);
        writer.flush();
        response.flushBuffer();
        return null;
    }

    public ActionForward exportToExcelAllDegrees(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
                                       HttpServletResponse response) throws IOException {

        ExecutionSemester executionSemester = getDomainObject(request, "executionSemester");
        ExecutionYear executionYear = executionSemester.getExecutionYear();

        Map<ExecutionDegree, List<TutorshipIntention>> tutorsListByDegree = TutorshipIntention.getTutorshipIntentionsWithExecutionDegrees(executionYear);

        StyledExcelSpreadsheet spreadsheet = new StyledExcelSpreadsheet("Tutores-Tutorandos", 15);
        spreadsheet.newHeaderRow();
        spreadsheet.addHeader("Tutor-Username");
        spreadsheet.addHeader("Tutor-Nome", 12500);
        spreadsheet.addHeader("Tutor-Email");
        spreadsheet.addHeader("Curso", 17000);
        spreadsheet.addHeader("Tutorando-Nº");
        spreadsheet.addHeader("Tutorando-Nome", 12500);
        spreadsheet.addHeader("Tutorando-Telemóvel", 7000);
        spreadsheet.addHeader("Tutorando-Email");
        spreadsheet.addHeader("Tutorando-Ciclo");
        spreadsheet.addHeader("Curso-Ciclos");

        for (Map.Entry<ExecutionDegree, List<TutorshipIntention>> entry : tutorsListByDegree.entrySet()) {
            ExecutionDegree degree = entry.getKey();

            String degreeName = degree.getPresentationName();

            List<CycleType> degreeCycles = degree.getDegree().getCycleTypes().stream().collect(Collectors.toList());
            degreeCycles.sort(CycleType.COMPARATOR_BY_LESS_WEIGHT);
            String degreeCyclesStr = degreeCycles.stream().map(c -> c.getDescription()).collect(Collectors.joining(", ")); 
            

            if (entry.getValue().isEmpty()) {
                spreadsheet.newRow();

                spreadsheet.addCell("");
                spreadsheet.addCell("");
                spreadsheet.addCell("");
                
                spreadsheet.addCell(degreeName);

                spreadsheet.addCell("");
                spreadsheet.addCell("");
                spreadsheet.addCell("");
                spreadsheet.addCell("");
                spreadsheet.addCell("");
                
                spreadsheet.addCell(degreeCyclesStr);
            }

            for (TutorshipIntention tutorshipIntention : entry.getValue()) {
                if (tutorshipIntention.getTutorships().isEmpty()) {
                    spreadsheet.newRow();

                    spreadsheet.addCell(tutorshipIntention.getTeacher().getPerson().getUsername());
                    spreadsheet.addCell(tutorshipIntention.getTeacher().getPerson().getName());
                    spreadsheet.addCell(tutorshipIntention.getTeacher().getPerson().getEmailForSendingEmails());

                    spreadsheet.addCell(degreeName);

                    spreadsheet.addCell("");
                    spreadsheet.addCell("");
                    spreadsheet.addCell("");
                    spreadsheet.addCell("");
                    spreadsheet.addCell("");
                    
                    spreadsheet.addCell(degreeCyclesStr);
                }

                for (Tutorship tutorship : tutorshipIntention.getTutorships()) {
                    spreadsheet.newRow();

                    spreadsheet.addCell(tutorshipIntention.getTeacher().getPerson().getUsername());
                    spreadsheet.addCell(tutorshipIntention.getTeacher().getPerson().getName());
                    spreadsheet.addCell(tutorshipIntention.getTeacher().getPerson().getEmailForSendingEmails());

                    spreadsheet.addCell(degreeName);

                    spreadsheet.addCell(tutorship.getStudent().getNumber());
                    spreadsheet.addCell(tutorship.getStudent().getName());
                    spreadsheet.addCell(tutorship.getStudent().getPerson().getDefaultMobilePhoneNumber());
                    spreadsheet.addCell(tutorship.getStudent().getPerson().getDefaultEmailAddressValue());
                    CycleType studentCycle = tutorship.getStudent().getCycleType(executionYear);
                    if (studentCycle != null) {
                        spreadsheet.addCell(studentCycle.getDescription());
                    } else {
                        spreadsheet.addCell("N/A");
                    }
                    
                    spreadsheet.addCell(degreeCyclesStr);
                }
            }
        }

        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename="
                + StringNormalizer.slugify(executionYear.getQualifiedName() + " "
                + "todos os cursos")
                + ".xls");
        final ServletOutputStream writer = response.getOutputStream();
        spreadsheet.getWorkbook().write(writer);
        writer.flush();
        response.flushBuffer();
        return null;
    }

    public ActionForward viewStudentsOfTutorship(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        String tutorshipIntentionID = (String) getFromRequest(request, "tutorshipIntentionID");

        TutorshipIntention tutorshipIntention = FenixFramework.getDomainObject(tutorshipIntentionID);
        request.setAttribute("tutorshipIntention", tutorshipIntention);

        return mapping.findForward("viewStudentOfTutor");
    }

    public ActionForward backToTutors(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        String tutorshipIntentionID = (String) getFromRequest(request, "tutorshipIntentionID");

        TutorshipIntention tutorshipIntention = FenixFramework.getDomainObject(tutorshipIntentionID);
        ExecutionDegree executionDegree =
                tutorshipIntention.getDegreeCurricularPlan().getExecutionDegreeByAcademicInterval(
                        tutorshipIntention.getAcademicInterval());
        ExecutionSemester firstExecutionPeriod = executionDegree.getExecutionYear().getFirstExecutionPeriod();

        ViewTutorsBean bean = new ViewTutorsBean();
        bean.setExecutionDegree(executionDegree);
        bean.setExecutionSemester(firstExecutionPeriod);

        request.setAttribute("tutorsBean", bean);
        return mapping.findForward("viewTutors");
    }

    public static class ContextDegreesProvider implements DataProvider {

        @Override
        public Converter getConverter() {
            return new DomainObjectKeyConverter();
        }
        @Override
        public Object provide(Object source, Object arg1) {
            final List<ExecutionDegree> executionDegrees = new ArrayList<ExecutionDegree>();
            final ViewTutorsBean bean = (ViewTutorsBean) source;
            final ExecutionSemester executionPeriod = bean.getExecutionSemester();
            if (executionPeriod != null) {
                final ExecutionYear executionYear = executionPeriod.getExecutionYear();
                for (ExecutionDegree executionDegree : executionYear.getExecutionDegreesSet()) {
                    DegreeType degreeType = executionDegree.getDegreeType();
                    if (degreeType.isIntegratedMasterDegree() || degreeType.isBolonhaDegree() || degreeType.isBolonhaMasterDegree()) {
                        executionDegrees.add(executionDegree);
                    }
                }
            }
            Collections.sort(executionDegrees, ExecutionDegree.EXECUTION_DEGREE_COMPARATORY_BY_DEGREE_TYPE_AND_NAME);
            return executionDegrees;
        }
    }

    public static class ExecutionSemestersProvider implements DataProvider {

        @Override
        public Object provide(Object source, Object currentValue) {
            List<ExecutionSemester> executionSemesters = new ArrayList<ExecutionSemester>();
            for (ExecutionSemester executionSemester : Bennu.getInstance().getExecutionPeriodsSet()) {
                if (executionSemester.isFirstOfYear()) {
                    executionSemesters.add(executionSemester);
                }
            }
            Collections.sort(executionSemesters, Comparator.reverseOrder());
            return executionSemesters;
        }

        @Override
        public Converter getConverter() {
            return new DomainObjectKeyConverter();
        }
    }

    public static class ViewTutorsBean implements Serializable {

        private ExecutionSemester executionSemester;
        private ExecutionDegree executionDegree;

        public List<TutorshipIntention> getTutors() {
            if (getExecutionDegree() != null) {
                return TutorshipIntention.getTutorshipIntentions(getExecutionDegree());
            }
            return null;
        }

        public ExecutionSemester getExecutionSemester() {
            return executionSemester;
        }

        public void setExecutionSemester(ExecutionSemester executionSemester) {
            this.executionSemester = executionSemester;
        }

        public ExecutionDegree getExecutionDegree() {
            return executionDegree;
        }

        public void setExecutionDegree(ExecutionDegree executionDegree) {
            this.executionDegree = executionDegree;
        }
    }
}
