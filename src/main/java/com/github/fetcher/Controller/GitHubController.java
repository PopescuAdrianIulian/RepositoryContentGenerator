package com.github.fetcher.Controller;

import com.github.fetcher.Service.GitHubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GitHubController {

    private final GitHubService gitHubService;

    @Autowired
    public GitHubController(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    @GetMapping("/fetch-all")
    public String fetchEntireRepository(
            @RequestParam String owner,
            @RequestParam String repo) {
        return gitHubService.fetchEntireRepository(owner, repo);
    }
}