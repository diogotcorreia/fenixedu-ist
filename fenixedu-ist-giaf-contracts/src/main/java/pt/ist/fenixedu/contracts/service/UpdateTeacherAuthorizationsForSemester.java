package pt.ist.fenixedu.contracts.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.TeacherAuthorization;
import org.fenixedu.academic.domain.TeacherCategory;
import org.fenixedu.academic.domain.organizationalStructure.AccountabilityTypeEnum;
import org.fenixedu.academic.domain.organizationalStructure.DepartmentUnit;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.PeriodType;

import pt.ist.fenixedu.contracts.domain.organizationalStructure.EmployeeContract;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.GiafProfessionalData;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.PersonContractSituation;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.ProfessionalCategory;
import pt.ist.fenixedu.contracts.domain.util.CategoryType;

public class UpdateTeacherAuthorizationsForSemester {

    private static final int minimumDaysForActivity = 90;

    public String updateTeacherAuthorization(ExecutionSemester executionSemester) {
        StringBuilder output = new StringBuilder();
        int countNew = 0;
        int countRevoked = 0;
        int countEdited = 0;
        Interval semesterInterval = executionSemester.getAcademicInterval().toInterval();
        for (GiafProfessionalData giafProfessionalData : Bennu.getInstance().getGiafProfessionalDataSet()) {
            Person person = giafProfessionalData.getPersonProfessionalData().getPerson();
            if (person != null) {
                Department department = getDominantDepartment(person, executionSemester);
                TeacherAuthorization teacherAuthorization = null;
                if (department != null) {
                    SortedSet<PersonContractSituation> validPersonContractSituations =
                            new TreeSet<PersonContractSituation>(new Comparator<PersonContractSituation>() {
                                @Override
                                public int compare(PersonContractSituation c1, PersonContractSituation c2) {
                                    int compare = c1.getBeginDate().compareTo(c2.getBeginDate());
                                    return compare == 0 ? c1.getExternalId().compareTo(c2.getExternalId()) : compare;
                                }
                            });
                    validPersonContractSituations.addAll(giafProfessionalData.getValidPersonContractSituations().stream()
                            .filter(pcs -> pcs.overlaps(semesterInterval)).filter(pcs -> {
                                ProfessionalCategory professionalCategory = pcs.getProfessionalCategory();
                                return professionalCategory != null && professionalCategory.getCategoryType() != null
                                        && professionalCategory.getCategoryType().equals(CategoryType.TEACHER);
                            }).filter(Objects::nonNull).collect(Collectors.toSet()));
                    int activeDays =
                            validPersonContractSituations.stream().mapToInt(s -> getActiveDays(s, semesterInterval)).sum();

                    if (activeDays >= minimumDaysForActivity) {
                        PersonContractSituation situation = getDominantSituation(validPersonContractSituations, semesterInterval);
                        if (situation != null) {
                            Teacher teacher = person.getTeacher();
                            if (person.getTeacher() == null) {
                                teacher = new Teacher(person);
                            }
                            TeacherCategory teacherCategory = situation.getProfessionalCategory().getTeacherCategory();
                            Double lessonHours = situation.getWeeklyLessonHours(semesterInterval);
                            TeacherAuthorization existing =
                                    teacher.getTeacherAuthorization(executionSemester.getAcademicInterval()).orElse(null);
                            if (existing != null) {
                                if (existing.getDepartment().equals(department) && existing.isContracted()
                                        && existing.getLessonHours().equals(lessonHours)
                                        && existing.getTeacherCategory().equals(teacherCategory)) {
                                    teacherAuthorization = existing;
                                } else {
                                    countEdited++;
                                    existing.revoke();
                                }
                            } else {
                                countNew++;
                            }
                            if (teacherAuthorization == null) {
                                teacherAuthorization = TeacherAuthorization.createOrUpdate(teacher, department, executionSemester,
                                        teacherCategory, true, lessonHours);
                            }
                        }
                    }
                }
                if (teacherAuthorization == null && person.getTeacher() != null) {
                    teacherAuthorization =
                            person.getTeacher().getTeacherAuthorization(executionSemester.getAcademicInterval()).orElse(null);
                    if (teacherAuthorization != null && teacherAuthorization.isContracted()) {
                        teacherAuthorization.revoke();
                        countRevoked++;
                    }
                }

            }

        }
        output.append("\n" + countNew + " authorizations created for semester " + executionSemester.getQualifiedName());
        output.append("\n" + countEdited + " authorizations edited for semester " + executionSemester.getQualifiedName());
        output.append("\n" + countRevoked + " authorizations revoked for semester " + executionSemester.getQualifiedName());
        return output.toString();
    }

    private PersonContractSituation getDominantSituation(SortedSet<PersonContractSituation> personContractSituations,
            Interval semesterInterval) {
        for (PersonContractSituation situation : personContractSituations) {
            int activeDays = getActiveDays(situation, semesterInterval);
            if (activeDays > minimumDaysForActivity) {
                return situation;
            }
        }
        return personContractSituations.first();
    }

    private int getActiveDays(PersonContractSituation situation, Interval semesterInterval) {
        LocalDate beginDate = situation.getBeginDate().isBefore(semesterInterval.getStart().toLocalDate()) ? semesterInterval
                .getStart().toLocalDate() : situation.getBeginDate();
        LocalDate endDate = situation.getEndDate() == null
                || situation.getEndDate().isAfter(semesterInterval.getEnd().toLocalDate()) ? semesterInterval.getEnd()
                        .toLocalDate() : situation.getEndDate();

        int activeDays = new Interval(beginDate.toDateTimeAtStartOfDay(), endDate.toDateTimeAtStartOfDay())
                .toPeriod(PeriodType.days()).getDays() + 1;
        return activeDays;
    }

    private Department getDominantDepartment(Person person, ExecutionSemester semester) {
        SortedSet<EmployeeContract> contracts = new TreeSet<EmployeeContract>(new Comparator<EmployeeContract>() {
            @Override
            public int compare(EmployeeContract ec1, EmployeeContract ec2) {
                int compare = ec1.getBeginDate().compareTo(ec2.getBeginDate());
                return compare == 0 ? ec1.getExternalId().compareTo(ec2.getExternalId()) : compare;
            }
        });
        Interval semesterInterval = semester.getAcademicInterval().toInterval();
        contracts.addAll(((Collection<EmployeeContract>) person.getParentAccountabilities(AccountabilityTypeEnum.WORKING_CONTRACT,
                EmployeeContract.class))
                        .stream()
                        .filter(ec -> ec.belongsToPeriod(semesterInterval.getStart().toYearMonthDay(),
                                semesterInterval.getEnd().toYearMonthDay()))
                        .filter(Objects::nonNull).collect(Collectors.toSet()));

        Department firstDepartmentUnit = null;
        for (EmployeeContract employeeContract : contracts) {
            Department employeeDepartmentUnit = getEmployeeDepartmentUnit(employeeContract.getUnit());
            if (employeeDepartmentUnit != null) {
                Interval contractInterval = new Interval(employeeContract.getBeginDate().toLocalDate().toDateTimeAtStartOfDay(),
                        employeeContract.getEndDate() == null ? new DateTime(Long.MAX_VALUE) : employeeContract.getEndDate()
                                .toLocalDate().toDateTimeAtStartOfDay().plusMillis(1));
                Interval overlap = semesterInterval.overlap(contractInterval);
                if (overlap != null) {
                    int days = overlap.toPeriod(PeriodType.days()).getDays() + 1;
                    if (days > minimumDaysForActivity) {
                        return employeeDepartmentUnit;
                    }
                    if (firstDepartmentUnit == null) {
                        firstDepartmentUnit = employeeDepartmentUnit;
                    }
                }
            }
        }
        return firstDepartmentUnit;
    }

    private Department getEmployeeDepartmentUnit(Unit unit) {
        Collection<Unit> parentUnits = unit.getParentUnits();
        if (unitDepartment(unit)) {
            return ((DepartmentUnit) unit).getDepartment();
        } else if (!parentUnits.isEmpty()) {
            for (Unit parentUnit : parentUnits) {
                if (unitDepartment(parentUnit)) {
                    return ((DepartmentUnit) parentUnit).getDepartment();
                } else if (parentUnit.hasAnyParentUnits()) {
                    Department department = getEmployeeDepartmentUnit(parentUnit);
                    if (department != null) {
                        return department;
                    }
                }
            }
        }
        return null;
    }

    private boolean unitDepartment(Unit unit) {
        return unit.isDepartmentUnit() && ((DepartmentUnit) unit).getDepartment() != null;
    }
}
