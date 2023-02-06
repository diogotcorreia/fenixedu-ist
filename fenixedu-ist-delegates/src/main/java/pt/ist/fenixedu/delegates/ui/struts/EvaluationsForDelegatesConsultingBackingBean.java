/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Delegates.
 *
 * FenixEdu IST Delegates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Delegates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Delegates.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.delegates.ui.struts;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.struts.util.MessageResources;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.CurricularYear;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Evaluation;
import org.fenixedu.academic.domain.Exam;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.OccupationPeriod;
import org.fenixedu.academic.domain.OccupationPeriodType;
import org.fenixedu.academic.domain.Project;
import org.fenixedu.academic.domain.WrittenEvaluation;
import org.fenixedu.academic.ui.faces.bean.base.FenixBackingBean;
import org.fenixedu.academic.ui.faces.components.util.CalendarLink;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.academic.util.PeriodState;
import org.fenixedu.commons.i18n.I18N;

import pt.ist.fenixedu.delegates.domain.student.Delegate;
import pt.ist.fenixframework.DomainObject;

public class EvaluationsForDelegatesConsultingBackingBean extends FenixBackingBean {

    private static final MessageResources messages = MessageResources.getMessageResources(Bundle.DELEGATE);

    private static final DateFormat hourFormat = new SimpleDateFormat("HH:mm");

    private Degree degree;

    private Collection<DegreeCurricularPlan> degreeCurricularPlanOptions;
    private Collection<ExecutionSemester> executionSemesterOptions;
    private Collection<CurricularYear> curricularYearOptions;

    private DegreeCurricularPlan selectedDegreeCurricularPlan;
    private ExecutionSemester selectedExecutionSemester;
    private CurricularYear selectedCurricularYear;

    private String getViewStateAttribute(String attributeName) {
        return (String) getViewState().getAttribute(attributeName);
    }

    public String getDegreeCurricularPlanID() {
        String id = getViewStateAttribute("degreeCurricularPlanID");
        if (id == null) {
            id = getMostRecentDegreeCurricularPlan().getExternalId();
            setDegreeCurricularPlanID(id);
        }
        return id;
    }

    public String getExecutionPeriodID() {
        String id = getViewStateAttribute("executionPeriodID");
        if (id == null) {
            id = getCurrentOrMostRecentExecutionPeriod().getExternalId();
            setExecutionPeriodID(id);
        }
        return id;
    }

    public String getCurricularYearID() {
        String id = getViewStateAttribute("curricularYearID");
        return id == null ? "" : id;
    }

    private Collection<DegreeCurricularPlan> getDegreeCurricularPlanOptions() {
        if (degreeCurricularPlanOptions == null) {
            degreeCurricularPlanOptions = getDegree()
                    .getActiveDegreeCurricularPlans()
                    .stream()
                    .sorted(DegreeCurricularPlan.COMPARATOR_BY_PRESENTATION_NAME)
                    .collect(Collectors.toList());
        }
        return degreeCurricularPlanOptions;
    }

    private Collection<ExecutionSemester> getExecutionSemesterOptions() {
        if (executionSemesterOptions == null) {
            executionSemesterOptions = getDegreeCurricularPlan()
                    .getExecutionDegreesSet()
                    .stream()
                    .flatMap(degree -> degree.getExecutionYear().getExecutionPeriodsSet().stream())
                    .sorted(ExecutionSemester.COMPARATOR_BY_SEMESTER_AND_YEAR.reversed())
                    .collect(Collectors.toList());
        }
        return executionSemesterOptions;
    }

    private Collection<CurricularYear> getCurricularYearOptions() {
        if (curricularYearOptions == null) {
            curricularYearOptions = IntStream.rangeClosed(1, getDegreeCurricularPlan().getDurationInYears())
                    .boxed()
                    .map(CurricularYear::readByYear)
                    .collect(Collectors.toList());
        }
        return curricularYearOptions;
    }

    public Degree getDegree() {
        if (degree == null) {
            degree = getUserView().getDelegatesSet().stream()
                    .filter(Delegate::isActive)
                    .map(Delegate::getDegree)
                    .findFirst()
                    .orElse(null);
        }
        return degree;
    }

    /**
     * Helper function to validate selected option against available options, calculating the default if the value
     * is not set, or isn't contained in the available options.
     * The result of the function is saved, so repeated calls don't have to compare to the available options list again,
     * that is, the result is memoized.
     *
     * @param selected The currently selected (memoized) object
     * @param selectedId The ID selected in the select form field
     * @param options The available options for this field
     * @param defaultSupplier A supplier for the default option for this field
     * @param setSelectedId The setter for this form field
     * @return The (memoized) selected object, that may or may not be the same as the selected argument.
     * @param <T> The type of the selected object. Must be a DomainObject.
     */
    private <T extends DomainObject> T getLazySelectedObjectFromOptions(T selected, String selectedId, Collection<T> options,
                                                                        Supplier<T> defaultSupplier, Consumer<String> setSelectedId) {
        if (selected != null && selected.getExternalId().equals(selectedId)) {
            return selected;
        }

        return options
                .stream()
                .filter(obj -> obj.getExternalId().equals(selectedId))
                .findAny()
                .orElseGet(() -> {
                    T defaultValue = defaultSupplier.get();
                    setSelectedId.accept(defaultValue == null ? "" : defaultValue.getExternalId());
                    return defaultValue;
                });
    }

    public DegreeCurricularPlan getDegreeCurricularPlan() {
        this.selectedDegreeCurricularPlan = getLazySelectedObjectFromOptions(
                this.selectedDegreeCurricularPlan,
                getDegreeCurricularPlanID(),
                getDegreeCurricularPlanOptions(),
                this::getMostRecentDegreeCurricularPlan,
                this::setDegreeCurricularPlanID
        );
        return this.selectedDegreeCurricularPlan;
    }

    public ExecutionSemester getExecutionPeriod() {
        this.selectedExecutionSemester = getLazySelectedObjectFromOptions(
                this.selectedExecutionSemester,
                getExecutionPeriodID(),
                getExecutionSemesterOptions(),
                this::getCurrentOrMostRecentExecutionPeriod,
                this::setExecutionPeriodID
        );
        return this.selectedExecutionSemester;
    }

    public CurricularYear getCurricularYear() {
        this.selectedCurricularYear = getLazySelectedObjectFromOptions(
                this.selectedCurricularYear,
                getCurricularYearID(),
                getCurricularYearOptions(),
                () -> null,
                this::setCurricularYearID
        );
        return this.selectedCurricularYear;
    }

    private DegreeCurricularPlan getMostRecentDegreeCurricularPlan() {
        return getDegree().getMostRecentDegreeCurricularPlan();
    }

    private ExecutionSemester getCurrentOrMostRecentExecutionPeriod() {
        ExecutionSemester currentExecutionPeriod = ExecutionSemester.readActualExecutionSemester();
        ExecutionDegree currentExecutionDegree = getDegreeCurricularPlan()
                .getExecutionDegreeByAcademicInterval(currentExecutionPeriod.getExecutionYear().getAcademicInterval());

        if (currentExecutionDegree == null) {
            return getMostRecentExecutionPeriod();
        }
        return currentExecutionPeriod;
    }

    public ExecutionSemester getMostRecentExecutionPeriod() {
        ExecutionSemester mostRecentExecutionPeriod = null;

        final DegreeCurricularPlan degreeCurricularPlan = getDegreeCurricularPlan();
        if (degreeCurricularPlan != null) {
            for (final ExecutionDegree executionDegree : degreeCurricularPlan.getExecutionDegreesSet()) {
                final ExecutionYear executionYear = executionDegree.getExecutionYear();
                for (final ExecutionSemester executionSemester : executionYear.getExecutionPeriodsSet()) {
                    if (executionSemester.getState() != PeriodState.CLOSED) {
                        if (mostRecentExecutionPeriod == null) {
                            mostRecentExecutionPeriod = executionSemester;
                        } else {
                            final ExecutionYear mostRecentExecutionYear = mostRecentExecutionPeriod.getExecutionYear();
                            if (executionYear.getYear().compareTo(mostRecentExecutionYear.getYear()) > 0
                                    || (executionYear == mostRecentExecutionYear && executionSemester.getSemester().compareTo(
                                            mostRecentExecutionPeriod.getSemester()) > 0)) {
                                mostRecentExecutionPeriod = executionSemester;
                            }
                        }
                    }
                }
            }
        }
        return mostRecentExecutionPeriod;
    }

    public List<SelectItem> getDegreeCurricularPlanSelectItems() {
        return getDegreeCurricularPlanOptions().stream()
                .map(degreeCurricularPlan -> new SelectItem(degreeCurricularPlan.getExternalId(), degreeCurricularPlan.getPresentationName()))
                .collect(Collectors.toList());
    }

    public List<SelectItem> getExecutionPeriodSelectItems() {
        return getExecutionSemesterOptions().stream()
                .map(period -> new SelectItem(period.getExternalId(), period.getQualifiedName()))
                .collect(Collectors.toList());
    }

    public List<SelectItem> getCurricularYearSelectItems() {
        return getCurricularYearOptions().stream()
                .map(curricularYear -> new SelectItem(curricularYear.getExternalId(), String.valueOf(curricularYear.getYear())))
                .collect(Collectors.toList());
    }

    public List<CalendarLink> getCalendarLinks() {
        List<CalendarLink> calendarLinks = new ArrayList<>();

        final DegreeCurricularPlan degreeCurricularPlan = getDegreeCurricularPlan();
        final CurricularYear curricularYear = getCurricularYear();
        final ExecutionSemester executionSemester = getExecutionPeriod();
        for (final CurricularCourse curricularCourse : degreeCurricularPlan.getCurricularCoursesSet()) {
            if (curricularYear == null
                    || curricularCourse.hasScopeInGivenSemesterAndCurricularYearInDCP(curricularYear, degreeCurricularPlan,
                            executionSemester)) {
                for (final ExecutionCourse executionCourse : curricularCourse.getAssociatedExecutionCoursesSet()) {
                    if (executionCourse.getExecutionPeriod() == executionSemester) {
                        for (final Evaluation evaluation : executionCourse.getAssociatedEvaluationsSet()) {

                            if (evaluation instanceof WrittenEvaluation) {
                                if (!(evaluation instanceof Exam) || ((Exam) evaluation).isExamsMapPublished()) {
                                    final WrittenEvaluation writtenEvaluation = (WrittenEvaluation) evaluation;
                                    CalendarLink calendarLink =
                                            new CalendarLink(executionCourse, writtenEvaluation, I18N.getLocale());
                                    calendarLink.setAsLink(false);
                                    calendarLinks.add(calendarLink);
                                    calendarLink.setLinkParameters(constructLinkParameters(executionCourse));
                                }

                            } else if (evaluation instanceof Project) {
                                final Project project = (Project) evaluation;
                                CalendarLink calendarLinkBegin = new CalendarLink(false);
                                calendarLinks.add(calendarLinkBegin);
                                calendarLinkBegin.setObjectOccurrence(project.getBegin());
                                calendarLinkBegin.setObjectLinkLabel(constructCalendarPresentation(executionCourse,
                                        project.getBegin(), messages.getMessage(I18N.getLocale(), "label.evaluation.project.begin")));
                                calendarLinkBegin.setLinkParameters(constructLinkParameters(executionCourse));

                                CalendarLink calendarLinkEnd = new CalendarLink(false);
                                calendarLinks.add(calendarLinkEnd);
                                calendarLinkEnd.setObjectOccurrence(project.getEnd());
                                calendarLinkEnd.setObjectLinkLabel(constructCalendarPresentation(executionCourse,
                                        project.getEnd(), messages.getMessage(I18N.getLocale(), "label.evaluation.project.end")));
                                calendarLinkEnd.setLinkParameters(constructLinkParameters(executionCourse));
                            }
                        }
                    }
                }
            }
        }

        return calendarLinks;
    }

    private Map<String, String> constructLinkParameters(final ExecutionCourse executionCourse) {
        final Map<String, String> linkParameters = new HashMap<>();
        linkParameters.put("method", "evaluations");
        linkParameters.put("executionCourseID", executionCourse.getExternalId());
        return linkParameters;
    }

    private String constructCalendarPresentation(final ExecutionCourse executionCourse, final Date time, final String tail) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(messages.getMessage(I18N.getLocale(), "label.evaluation.shortname.project"));
        stringBuilder.append(" ");
        stringBuilder.append(executionCourse.getSigla());
        stringBuilder.append(" (");
        stringBuilder.append(hourFormat.format(time));
        stringBuilder.append(") ");
        stringBuilder.append(tail);
        return stringBuilder.toString();
    }

    public String getApplicationContext() {
        return getRequest().getContextPath();
    }

    public void setDegreeCurricularPlanID(String degreeCurricularPlanID) {
        this.getViewState().setAttribute("degreeCurricularPlanID", degreeCurricularPlanID);
    }

    public void resetExecutionPeriodAndCurricularYear(ValueChangeEvent event) {
        this.getViewState().removeAttribute("executionPeriodID");
        this.getViewState().removeAttribute("curricularYearID");
        this.selectedExecutionSemester = null;
        this.executionSemesterOptions = null;
        this.selectedCurricularYear = null;
        this.curricularYearOptions = null;
    }

    public void setExecutionPeriodID(String executionPeriodID) {
        this.getViewState().setAttribute("executionPeriodID", executionPeriodID);
    }

    public void setCurricularYearID(String curricularYearID) {
        this.getViewState().setAttribute("curricularYearID", curricularYearID);
    }

    public Date getBeginDate() {
        final ExecutionSemester executionSemester = getExecutionPeriod();
        final DegreeCurricularPlan degreeCurricularPlan = getDegreeCurricularPlan();

        final ExecutionYear executionYear = executionSemester.getExecutionYear();

        return degreeCurricularPlan.getExecutionDegreesSet().stream()
                .filter(degree -> degree.getExecutionYear() == executionYear)
                .findAny()
                .flatMap(executionDegree ->
                        executionDegree.getPeriods(OccupationPeriodType.LESSONS, executionSemester.getSemester())
                                .map(OccupationPeriod::getStart)
                                .min(Comparator.naturalOrder())
                )
                .orElseGet(executionSemester::getBeginDate);
    }

    public Date getEndDate() {
        final ExecutionSemester executionSemester = getExecutionPeriod();
        final DegreeCurricularPlan degreeCurricularPlan = getDegreeCurricularPlan();

        final ExecutionYear executionYear = executionSemester.getExecutionYear();

        return degreeCurricularPlan.getExecutionDegreesSet().stream()
                .filter(degree -> degree.getExecutionYear() == executionYear)
                .findAny()
                .flatMap(executionDegree ->
                        executionDegree.getPeriods(OccupationPeriodType.EXAMS, executionSemester.getSemester())
                                .map(OccupationPeriod::getLastOccupationPeriodOfNestedPeriods)
                                .map(OccupationPeriod::getEnd)
                                .max(Comparator.naturalOrder())
                )
                .orElseGet(executionSemester::getEndDate);
    }

}
