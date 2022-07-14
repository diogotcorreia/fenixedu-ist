package pt.ist.fenixedu.tutorship.ui.spring;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.PathParam;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.security.SkipCSRF;
import org.fenixedu.bennu.spring.portal.SpringApplication;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.fenixedu.commons.spreadsheet.ExcelStyle;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import pt.ist.fenixframework.FenixFramework;

@SpringApplication(path = "students-performances", title = "title.tutorship.spring", group = "#tutorship",
        hint = "Students Low Performances")
@SpringFunctionality(app = StudentsPerformanceController.class, title = "title.students.performance",
        accessGroup = "#tutorship")

@Controller
@RequestMapping("/students-performances")
public class StudentsPerformanceController {

    @SkipCSRF
    @RequestMapping(method = RequestMethod.GET)
    public String home(final Model model) {

        List<ExecutionYear> executionYears = new ArrayList<ExecutionYear>();
        executionYears = Bennu.getInstance().getExecutionYearsSet().stream().filter(y->!y.isAfter(ExecutionYear.readCurrentExecutionYear()))
                .sorted(ExecutionYear.REVERSE_COMPARATOR_BY_YEAR).collect(Collectors.toList());
        
        model.addAttribute("executionYears", executionYears);
        return "students-performances/home";
    }

    @SkipCSRF
    @RequestMapping(value = "download", method = RequestMethod.GET)
    public String download(@PathParam(value = "executionId") String executionId, final HttpServletResponse response,
            final Model model) {

        if (executionId.isEmpty()) {
            return  home(model);
        }

        final ExecutionYear executionYear = FenixFramework.getDomainObject(executionId);        
        final String filename = "StudentsPerformance" + executionYear.getName().replace("/", "_")  + ".xlsx";
        try {
            final HSSFWorkbook workbook = new HSSFWorkbook();
            final ExcelStyle excelStyle = new ExcelStyle(workbook);
            response.setHeader("Content-Disposition", "inline; filename=" + filename);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
            
            final Spreadsheet spreadsheet = dumpInformation(executionYear);
            spreadsheet.exportToXLSSheet(workbook, (HSSFCellStyle) excelStyle.getHeaderStyle(), (HSSFCellStyle) excelStyle.getStringStyle());
            final ServletOutputStream writer = response.getOutputStream();
            workbook.write(writer);
            writer.flush();
            writer.close();
           
            return  home(model);
        } catch (IOException e) {
            throw new Error(e);
        }
    }


 

    public Spreadsheet dumpInformation(ExecutionYear executionYear) throws IOException {
        final Spreadsheet spreadsheet = new Spreadsheet("StudentsPerformance");

        executionYear.getRegistrationDataByExecutionYearSet().stream().filter(
                data -> data.getRegistration().getDegree().isFirstCycle() || data.getRegistration().getDegree().isSecondCycle())
                .forEach(data -> {
                    final Registration registration = data.getRegistration();
                    final Student student = registration.getStudent();
                    final Person person = student.getPerson();
                    final StudentCurricularPlan studentCurricularPlan = registration.getStudentCurricularPlan(executionYear);
                    if (studentCurricularPlan != null) {

                        final DegreeCurricularPlan degreeCurricularPlan = studentCurricularPlan.getDegreeCurricularPlan();
                        final Degree degree = degreeCurricularPlan.getDegree();
                        final CycleType cycleType = registration.getCycleType(executionYear);
                        final Double maxCredits = data.getMaxCreditsPerYear();
                        final ExecutionSemester allowedSemester = data.getAllowedSemesterForEnrolments();
                        final ExecutionYear ingressionYear =
                                executionYears(registration).min(ExecutionYear::compareTo).orElse(null);

                        final Supplier<Stream<Enrolment>> enrolments = () -> studentCurricularPlan.getEnrolmentStream()
                                .filter(enrolment -> enrolment.getExecutionYear() == executionYear);

                        spreadsheet.addRow().setCell("User", person.getUsername()).setCell("Name", person.getName())
                                .setCell("Degree Type", degree.getDegreeType().getName().getContent())
                                .setCell("Curricular Plan", degreeCurricularPlan.getName())
                                .setCell("Ingression Year", ingressionYear == null ? null : ingressionYear.getName())
                                .setCell("Protocol",
                                        registration.getRegistrationProtocol() == null ? "" : registration
                                                .getRegistrationProtocol().getDescription().getContent())
                                .setCell("Ingression Type",
                                        registration.getIngressionType() == null ? "" : registration.getIngressionType()
                                                .getLocalizedName())
                                .setCell("Cycle", cycleType == null ? null : cycleType.getDescriptionI18N().getContent())
                                .setCell("Enrolment Date",
                                        data.getEnrolmentDate() == null ? "" : data.getEnrolmentDate().toString("yyyy-MM-dd"))
                                .setCell("Max Credits Allowed", maxCredits == null ? "" : maxCredits.toString())
                                .setCell("Allowed Semester",
                                        allowedSemester == null ? "" : allowedSemester.getSemester().toString())

                                .setCell("Enrolment Count", Long.toString(enrolments.get().count()))
                                .setCell("Enrolment ECTS",
                                        Double.toString(enrolments.get()
                                                .mapToDouble(e -> e.getEctsCreditsForCurriculum().doubleValue()).sum()))
                                .setCell("Approved Count", Long.toString(enrolments.get().filter(e -> e.isApproved()).count()))
                                .setCell("Approved ECTS",
                                        Double.toString(enrolments.get().filter(e -> e.isApproved())
                                                .mapToDouble(e -> e.getEctsCreditsForCurriculum().doubleValue()).sum()))

                                .setCell("Enrolment Count Sem 1",
                                        Long.toString(enrolments.get()
                                                .filter(e -> e.getExecutionPeriod().getSemester().intValue() == 1).count()))
                                .setCell("Enrolment ECTS Sem 1",
                                        Double.toString(
                                                enrolments.get().filter(e -> e.getExecutionPeriod().getSemester().intValue() == 1)
                                                        .mapToDouble(e -> e.getEctsCreditsForCurriculum().doubleValue()).sum()))
                                .setCell("Approved Count Sem 1",
                                        Long.toString(
                                                enrolments.get().filter(e -> e.getExecutionPeriod().getSemester().intValue() == 1)
                                                        .filter(e -> e.isApproved()).count()))
                                .setCell("Approved ECTS Sem 1",
                                        Double.toString(
                                                enrolments.get().filter(e -> e.getExecutionPeriod().getSemester().intValue() == 1)
                                                        .filter(e -> e.isApproved())
                                                        .mapToDouble(e -> e.getEctsCreditsForCurriculum().doubleValue()).sum()))

                                .setCell("Enrolment Count Sem 2",
                                        Long.toString(enrolments.get()
                                                .filter(e -> e.getExecutionPeriod().getSemester().intValue() == 2).count()))
                                .setCell("Enrolment ECTS",
                                        Double.toString(
                                                enrolments.get().filter(e -> e.getExecutionPeriod().getSemester().intValue() == 2)
                                                        .mapToDouble(e -> e.getEctsCreditsForCurriculum().doubleValue()).sum()))
                                .setCell("Approved Count Sem 2",
                                        Long.toString(
                                                enrolments.get().filter(e -> e.getExecutionPeriod().getSemester().intValue() == 2)
                                                        .filter(e -> e.isApproved()).count()))
                                .setCell("Approved ECTS",
                                        Double.toString(
                                                enrolments.get().filter(e -> e.getExecutionPeriod().getSemester().intValue() == 2)
                                                        .filter(e -> e.isApproved())
                                                        .mapToDouble(e -> e.getEctsCreditsForCurriculum().doubleValue()).sum()));
                    }
                });
        return spreadsheet;
    }

    private Stream<ExecutionYear> executionYears(final Registration registration) {
        Stream<ExecutionYear> stream =
                registration.getRegistrationDataByExecutionYearSet().stream().map(data -> data.getExecutionYear());
        final Registration source = registration.getSourceRegistration();
        return executionYears(stream, source == null || source.getSourceRegistration() != registration ? source : null);
    }

    private Stream<ExecutionYear> executionYears(final Stream<ExecutionYear> stream, final Registration registration) {
        return registration == null ? stream : Stream.concat(stream, executionYears(registration));
    }

}