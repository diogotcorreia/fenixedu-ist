package pt.ist.fenixedu.delegates.ui;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accessControl.StudentGroup;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import java.util.Objects;
import java.util.StringJoiner;

import static org.fenixedu.bennu.FenixEduDelegatesConfiguration.BUNDLE;

public class DelegateCycleStudentGroupBean extends DelegateStudentGroupBean {
    private Degree degree;
    private CycleType cycle;
    private ExecutionYear year;

    public DelegateCycleStudentGroupBean(Degree degree, CycleType cycle, ExecutionYear year) {
        this.degree = degree;
        this.cycle = cycle;
        this.year = year;
    }

    public Degree getDegree() {
        return degree;
    }

    public CycleType getCycle() {
        return cycle;
    }

    public ExecutionYear getYear() {
        return year;
    }

    @Override
    public StudentGroup getStudentGroup() {
        return StudentGroup.get(null, this.degree, this.cycle, null, null, null, this.year);
    }

    @Override
    public String getName() {
        if (this.cycle != null) {
            return BundleUtil.getString(BUNDLE, "delegate.messaging.cycle.students",
                    getCycle().getDescription(), getDegree().getSigla(), getYear().getName());
        } else {
            return BundleUtil.getString(BUNDLE, "delegate.messaging.degree.students",
                    getDegree().getDegreeType().getName().getContent(), getDegree().getSigla(), getYear().getName());
        }
    }

    @Override
    public String serialize() {
        return new StringJoiner(":")
                .add("cycleOrDegree")
                .add(degree.getExternalId())
                .add(cycle.toString())
                .add(year.getExternalId())
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DelegateCycleStudentGroupBean that = (DelegateCycleStudentGroupBean) o;
        return Objects.equals(degree, that.degree) && cycle == that.cycle && Objects.equals(year, that.year);
    }

    @Override
    public int hashCode() {
        return Objects.hash(degree, cycle, year);
    }
}
