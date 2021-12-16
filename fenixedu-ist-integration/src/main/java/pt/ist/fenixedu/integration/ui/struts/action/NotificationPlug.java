package pt.ist.fenixedu.integration.ui.struts.action;

import org.fenixedu.bennu.core.domain.User;

import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public interface NotificationPlug {

    Set<NotificationPlug> PLUGS = Collections.synchronizedSet(new HashSet<>());

    boolean showNotification(final User user);

    String redirectUrl(final HttpSession httpSession);

}