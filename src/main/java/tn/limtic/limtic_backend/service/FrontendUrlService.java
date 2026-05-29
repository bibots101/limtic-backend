package tn.limtic.limtic_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class FrontendUrlService {

    @Value("${app.frontend-url:https://localhost:4200}")
    private String defaultFrontendUrl;

    public String resolveFrontendUrl(HttpServletRequest request) {
        if (request != null) {
            String origin = request.getHeader("Origin");
            if (origin != null && !origin.isBlank()) {
                return normalize(origin);
            }

            String referer = request.getHeader("Referer");
            if (referer != null && !referer.isBlank()) {
                int schemeSeparator = referer.indexOf("://");
                int pathStart = schemeSeparator >= 0 ? referer.indexOf('/', schemeSeparator + 3) : -1;
                if (pathStart > 0) {
                    return normalize(referer.substring(0, pathStart));
                }
            }
        }

        return normalize(defaultFrontendUrl);
    }

    public String getConfiguredFrontendUrl() {
        return normalize(defaultFrontendUrl);
    }

    private String normalize(String url) {
        return url.replaceAll("/+$", "");
    }
}