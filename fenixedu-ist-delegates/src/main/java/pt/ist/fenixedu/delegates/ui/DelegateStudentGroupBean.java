package pt.ist.fenixedu.delegates.ui;

import org.fenixedu.academic.domain.accessControl.StudentGroup;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import pt.ist.fenixframework.FenixFramework;

public abstract class DelegateStudentGroupBean {

    public abstract StudentGroup getStudentGroup();

    public abstract String getName();

    public abstract String serialize();

    public static DelegateStudentGroupBean deserialize(String source) {
        String[] sourceStr = source.split(":");

        if (sourceStr[0].equals("year")) {
            if (sourceStr.length < 4) {
                return null;
            }
            return new DelegateYearStudentGroupBean(
                    FenixFramework.getDomainObject(sourceStr[1]),
                    FenixFramework.getDomainObject(sourceStr[2]),
                    FenixFramework.getDomainObject(sourceStr[3])
            );
        } else if (sourceStr[0].equals("cycle")) {
            if (sourceStr.length < 4) {
                return null;
            }
            return new DelegateCycleStudentGroupBean(
                    FenixFramework.getDomainObject(sourceStr[1]),
                    CycleType.valueOf(sourceStr[2]),
                    FenixFramework.getDomainObject(sourceStr[3])
            );
        } else if (sourceStr[0].equals("degree")) {
            if (sourceStr.length < 3) {
                return null;
            }
            return new DelegateCycleStudentGroupBean(
                    FenixFramework.getDomainObject(sourceStr[1]),
                    null,
                    FenixFramework.getDomainObject(sourceStr[2])
            );
        }

        return null;
    }

}
