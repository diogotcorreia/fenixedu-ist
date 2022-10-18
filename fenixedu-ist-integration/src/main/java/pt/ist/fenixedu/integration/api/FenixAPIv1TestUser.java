package pt.ist.fenixedu.integration.api;

import com.google.common.io.BaseEncoding;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kong.unirest.Unirest;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.util.ContentType;
import org.fenixedu.bennu.core.domain.Avatar;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import pt.ist.fenixedu.integration.api.beans.FenixPerson;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FenixAPIv1TestUser {

    private static final String USERNAME = "connect_0123";

    protected boolean isTestUser() {
        final User user = Authenticate.getUser();
        return user != null && user.getUsername().equals(USERNAME);
    }

    public JsonObject testUserInfo() {
        return new JsonParser().parse(Unirest.get("https://repo.dsi.tecnico.ulisboa.pt/fenixedu/data/-/raw/master/api/" + USERNAME + ".json").asString().getBody()).getAsJsonObject();
    }


    protected FenixPerson personTest() {
        final JsonObject info = testUserInfo();

        final String name = info.get("name").getAsString();
        final String displayName = info.get("displayName").getAsString();
        final String gender = info.get("gender").getAsString();
        final String birthday = info.get("birthday").getAsString();
        final String campus = info.get("campus").getAsString();
        final String email = info.get("email").getAsString();
        final String institutionalEmail = email;
        final List<String> personalEmails = Stream.of(email).collect(Collectors.toList());
        final List<String> workEmails = personalEmails;
        final List<String> personalWebAdresses = Stream.of(info.get("webAdresses").getAsString()).collect(Collectors.toList());

        final List<String> workWebAdresses = personalWebAdresses;
        final Set<FenixPerson.FenixRole> roles = new HashSet<>();
        final JsonArray rolesJson = info.getAsJsonArray("roles");
        if (rolesJson != null && !rolesJson.isJsonNull()) {
            for (final JsonElement e : rolesJson) {
                final String role = e.getAsString();
                if (role.equals(FenixPerson.FenixRoleType.TEACHER.name())) {
                    final Department department = Bennu.getInstance().getDepartmentsSet().stream()
                            .findAny().orElse(null);
                    if (department != null) {
                        roles.add(new FenixPerson.TeacherFenixRole(department));
                    }
                } else if (role.equals(FenixPerson.FenixRoleType.STUDENT.name())) {
                    addRole(roles, Degree::isSecondCycle, new FenixPerson.StudentFenixRole(Collections.emptyList()));
                } else if (role.equals(FenixPerson.FenixRoleType.EMPLOYEE.name())) {
                    roles.add(new FenixPerson.EmployeeFenixRole());
                } else if (role.equals(FenixPerson.FenixRoleType.ALUMNI.name())) {
                    addRole(roles, Degree::isSecondCycle, new FenixPerson.AlumniFenixRole(Collections.emptyList()));
                }
            }
        }

        final byte[] avatar = Avatar.process(getClass().getClassLoader().getResourceAsStream("alfredo.png"), "image/png", 100);
        final FenixPerson.FenixPhoto photo = new FenixPerson.FenixPhoto(ContentType.PNG.getMimeType(), BaseEncoding.base64().encode(avatar));
        return new FenixPerson(campus, roles, photo, name, displayName, gender, birthday, USERNAME, email, institutionalEmail,
                personalEmails,
                workEmails,
                personalWebAdresses, workWebAdresses);
    }

    private void addRole(final Set<FenixPerson.FenixRole> roles, final Predicate<Degree> predicate, final FenixPerson.StudentFenixRole role) {
        final FenixPerson.StudentFenixRole.FenixRegistration fenixRegistration = new FenixPerson.StudentFenixRole.FenixRegistration();
        final Degree degree = Bennu.getInstance().getDegreesSet().stream()
                .filter(predicate)
                .filter(d -> d.isActive())
                .findAny().orElse(null);
        fenixRegistration.setName(degree.getPresentationNameI18N().getContent());
        fenixRegistration.setAcronym(degree.getSigla());
        fenixRegistration.setId(degree.getExternalId());
        final ExecutionSemester semester = ExecutionSemester.readActualExecutionSemester();
        final List<String> terms = Stream.of(semester, semester.getPreviousExecutionPeriod(), semester.getPreviousExecutionPeriod().getPreviousExecutionPeriod())
                .map(s -> s.getQualifiedName())
                .collect(Collectors.toList());
        fenixRegistration.setAcademicTerms(terms);
        role.getRegistrations().add(fenixRegistration);
        roles.add(role);
    }

}
