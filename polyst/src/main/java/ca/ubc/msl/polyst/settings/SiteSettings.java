package ca.ubc.msl.polyst.settings;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by mjacobson on 22/01/18.
 */
@Component
@ConfigurationProperties(prefix = "polyst.site")
@Getter
@Setter
public class SiteSettings {

    private String title;
    private String subtitle;
    private String msl;
    private String contactEmail;

    private String apiSite;
    private String htmlSite;

}
