/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST CMS Components.
 *
 * FenixEdu IST CMS Components is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST CMS Components is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST CMS Components.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.cmscomponents.domain.homepage.components;

import com.google.common.hash.Hashing;
import org.fenixedu.cms.domain.CloneCache;
import org.fenixedu.cms.domain.Page;
import org.fenixedu.cms.domain.Site;
import org.fenixedu.cms.domain.component.Component;
import org.fenixedu.cms.domain.component.ComponentParameter;
import org.fenixedu.cms.domain.component.ComponentType;
import org.fenixedu.cms.domain.component.DynamicComponent;
import org.fenixedu.cms.rendering.TemplateContext;
import org.fenixedu.commons.i18n.I18N;

import com.google.gson.JsonObject;

import pt.ist.fenixedu.cmscomponents.FenixEduIstCmsComponentsConfiguration;
import pt.ist.fenixedu.cmscomponents.domain.homepage.HomepageSite;

import java.nio.charset.StandardCharsets;

@ComponentType(name = "Researcher Section Data Component",
        description = "Provides homepage owner's researcher section page data.")
public class ResearcherComponent extends ResearcherComponent_Base {

    @DynamicComponent
    public ResearcherComponent(@ComponentParameter("Title Key") String titleKey,
            @ComponentParameter("Title Bundle") String titleBundle, @ComponentParameter("Data Key") String dataKey) {
        setTitleKey(titleKey);
        setDataKey(dataKey);
        setTitleBundle(titleBundle);
    }

    @Override
    public void handle(Page page, TemplateContext local, TemplateContext global) {
        String userId = Hashing.sha256().hashString(
                page.getSite().getOwner().getUsername() + "@tecnico.ulisboa.pt",
                        StandardCharsets.UTF_8).toString();
        global.put("bundle", getTitleBundle());
        global.put("researcher", userId);
        global.put("sotisUrl", FenixEduIstCmsComponentsConfiguration.getConfiguration().sotisURL());
        global.put("language", I18N.getLocale().toLanguageTag());
        global.put("dataKey", getDataKey());
        global.put("titleKey", getTitleKey());
    }

    public static boolean supportsSite(Site site) {
        return site.getHomepageSite()!=null;
    }

    @Override
    public Component clone(CloneCache cloneCache) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JsonObject json() {
        // TODO Auto-generated method stub
        return null;
    }

}
