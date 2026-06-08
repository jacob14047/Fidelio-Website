package it.unisa.fidelio.application.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CommunityController {

    @GetMapping("/communities")
    public String viewCommunitiesPage() {
        return "communities"; // Cerca communities.html in resources/templates
    }

    @GetMapping("/community-details")
    public String viewCommunityDetailPage() {
        return "community-details"; // Cerca community-detail.html
    }
}