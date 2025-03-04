package com.github.fetcher.Service;

import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Service
public class GitHubService {

    @Value("${github.token}")
    private String githubToken;


    @Value("${excluded.files}")
    private String excludedFiles;


    public String fetchEntireRepository(String owner, String repo) {
        try {

            GitHub github = new GitHubBuilder().withOAuthToken(githubToken).build();
            GHRepository repository = github.getRepository(owner + "/" + repo);

            StringBuilder allContent = new StringBuilder();
            allContent.append("Repository: ").append(owner).append("/").append(repo).append("\n\n");

            List<String> allFiles = listAllFiles(repository, "");

            for (String path : allFiles) {
                if (shouldExcludeFile(path)) {
                    continue;
                }
                try {
                    String content = fetchFileContent(repository, path);
                    allContent.append("--- File: ").append(path).append(" ---\n");
                    allContent.append(content).append("\n\n");
                } catch (Exception e) {
                    allContent.append("--- File: ").append(path).append(" ---\n");
                    allContent.append("Error fetching content: ").append(e.getMessage()).append("\n\n");
                }
            }
            return allContent.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch repository content: " + e.getMessage(), e);
        }
    }

    private List<String> listAllFiles(GHRepository repository, String path) throws IOException {
        List<String> files = new ArrayList<>();

        for (GHContent content : repository.getDirectoryContent(path)) {
            String contentPath = content.getPath();
            if (content.getType().equals("dir")) {
                files.addAll(listAllFiles(repository, contentPath));
            } else if (content.getType().equals("file")) {
                files.add(contentPath);
            }
        }
        return files;
    }

    private boolean shouldExcludeFile(String path) {
        List<String> EXCLUDED_FILES = Arrays.asList(excludedFiles.split(","));
        for (String excluded : EXCLUDED_FILES) {
            if (path.contains(excluded)) {
                return true;
            }
        }
        return false;
    }

    private String fetchFileContent(GHRepository repository, String path) throws IOException {
        GHContent content = repository.getFileContent(path);
        if (content.getSize() > 1024 * 1024) {
            return "File too large to display (" + (content.getSize() / 1024) + " KB)";
        }
        try {
            try {
                byte[] contentBytes = Base64.getDecoder().decode(content.getContent());
                return new String(contentBytes);
            } catch (IllegalArgumentException e) {
                try (InputStream is = content.read();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

                    StringBuilder textContent = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        textContent.append(line).append("\n");
                    }
                    return textContent.toString();
                }
            }
        } catch (IOException e) {
            try (InputStream is = new URL(content.getDownloadUrl()).openStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder textContent = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    textContent.append(line).append("\n");
                }
                return textContent.toString();
            }
        }
    }
}
