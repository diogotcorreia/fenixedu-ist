/**
 * Copyright © 2013 Instituto Superior Técnico
 * <p>
 * This file is part of FenixEdu IST Delegates.
 * <p>
 * FenixEdu IST Delegates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * FenixEdu IST Delegates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Delegates.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.delegates.ui;

import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.accessControl.StudentGroup;
import org.fenixedu.academic.domain.accessControl.TeacherResponsibleOfExecutionCourseGroup;
import org.fenixedu.bennu.core.groups.Group;
import org.springframework.util.CollectionUtils;
import pt.ist.fenixedu.delegates.domain.student.Delegate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DelegateStudentSelectBean {

    private List<ExecutionCourse> selectedExecutionCourses;
    private List<String> selectedStudentGroups;
    private Delegate selectedPosition;
    private Set<Delegate> positions;
    private Boolean selectedResponsibleTeachers;

    public DelegateStudentSelectBean(Set<Delegate> delegates) {
        this();
        setInfo(delegates);
    }

    public DelegateStudentSelectBean() {
        selectedExecutionCourses = new ArrayList<>();
        selectedStudentGroups = new ArrayList<>();
        selectedPosition = null;
        positions = null;
        selectedResponsibleTeachers = false;
    }

    public void setInfo(Set<Delegate> delegates) {
        positions = delegates;
        setInfo();
    }

    private void setInfo() {
        if (selectedPosition == null) {
            selectedPosition = positions.iterator().next();
            return;
        }
        if (positions == null) {
            positions = selectedPosition.getUser().getDelegatesSet().stream()
                    .filter(Delegate::isActive).collect(Collectors.toSet());
        }
    }

    public List<ExecutionCourse> getSelectedExecutionCourses() {
        return selectedExecutionCourses;
    }

    public List<String> getSelectedStudentGroups() {
        return this.selectedStudentGroups;
    }

    public Delegate getSelectedPosition() {
        return selectedPosition;
    }

    public Set<Delegate> getPositions() {
        return positions;
    }

    public Boolean getSelectedResponsibleTeachers() {
        return selectedResponsibleTeachers;
    }

    public void setSelectedExecutionCourses(List<ExecutionCourse> selectedExecutionCourses) {
        this.selectedExecutionCourses = selectedExecutionCourses;
    }

    public void setSelectedStudentGroups(List<String> selectedStudentGroups) {
        this.selectedStudentGroups = selectedStudentGroups;
    }

    public void setSelectedPosition(Delegate selectedPosition) {
        this.selectedPosition = selectedPosition;
        if (selectedPosition != null) {
            setInfo();
        }
    }

    public void setPositions(Set<Delegate> positions) {
        this.positions = positions;
    }

    public void setSelectedResponsibleTeachers(Boolean selectedResponsibleTeachers) {
        this.selectedResponsibleTeachers = selectedResponsibleTeachers;
    }

    public Stream<DelegateStudentGroupBean> convertSelectedStudentGroups() {
        return this.selectedStudentGroups.stream()
                .map(DelegateStudentGroupBean::deserialize)
                .filter(Objects::nonNull);
    }

    public List<Group> getRecipients() {
        List<Group> toRet = new ArrayList<>();
        if (!CollectionUtils.isEmpty(selectedExecutionCourses)) {
            List<ExecutionCourse> accessibleStudentCourses = selectedPosition.getDelegateExecutionCourses();
            List<ExecutionCourse> selectedStudentCourses = this.selectedExecutionCourses
                    .stream()
                    .filter(accessibleStudentCourses::contains)
                    .collect(Collectors.toList());

            if (selectedResponsibleTeachers) {
                selectedStudentCourses.stream().map(TeacherResponsibleOfExecutionCourseGroup::get).forEach(toRet::add);
            } else {
                selectedStudentCourses.stream()
                        .map(executionCourse -> StudentGroup.get(null, selectedPosition.getDegree(),
                                null, null, executionCourse, null, null))
                        .forEach(toRet::add);
            }
        }
        if (!CollectionUtils.isEmpty(selectedStudentGroups)) {
            final List<DelegateStudentGroupBean> allowedStudentGroups = selectedPosition.getStudentGroups();
            convertSelectedStudentGroups()
                    .filter(allowedStudentGroups::contains)
                    .map(DelegateStudentGroupBean::getStudentGroup)
                    .forEach(toRet::add);
        }
        return toRet;
    }
}