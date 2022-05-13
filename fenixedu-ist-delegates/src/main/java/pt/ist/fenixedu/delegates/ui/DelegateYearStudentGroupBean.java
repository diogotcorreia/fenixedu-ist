package pt.ist.fenixedu.delegates.ui;

import org.fenixedu.academic.domain.CurricularYear;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accessControl.StudentGroup;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import java.util.Objects;
import java.util.StringJoiner;

import static org.fenixedu.bennu.FenixEduDelegatesConfiguration.BUNDLE;

public class DelegateYearStudentGroupBean extends DelegateStudentGroupBean {
    private Degree degree;
    private CurricularYear curricularYear;
    private ExecutionYear year;

    public DelegateYearStudentGroupBean(Degree degree, CurricularYear curricularYear, ExecutionYear year) {
        this.degree = degree;
        this.curricularYear = curricularYear;
        this.year = year;
    }

    public Degree getDegree() {
        return degree;
    }

    public CurricularYear getCurricularYear() {
        return curricularYear;
    }

    public ExecutionYear getYear() {
        return year;
    }

    @Override
    public StudentGroup getStudentGroup() {
        return StudentGroup.get(this.degree, this.curricularYear, this.year);
    }

    @Override
    public String getName() {
        return BundleUtil.getString(BUNDLE, "delegate.messaging.year.students",
                getCurricularYear().getYear().toString(), getDegree().getSigla(), getYear().getName());
    }

    @Override
    public String serialize() {
        return new StringJoiner(":")
                .add("curricularYear")
                .add(degree.getExternalId())
                .add(curricularYear.getExternalId())
                .add(year.getExternalId())
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DelegateYearStudentGroupBean that = (DelegateYearStudentGroupBean) o;
        return Objects.equals(degree, that.degree) && Objects.equals(curricularYear, that.curricularYear) && Objects.equals(year, that.year);
    }

    @Override
    public int hashCode() {
        return Objects.hash(degree, curricularYear, year);
    }
}
