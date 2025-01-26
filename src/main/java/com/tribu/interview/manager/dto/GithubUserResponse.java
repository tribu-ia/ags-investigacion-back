package com.tribu.interview.manager.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GithubUserResponse {
    private String login;
    private Long id;
    private String nodeId;
    @JsonProperty("avatar_url")
    private String avatarUrl;
    @JsonProperty("gravatar_id")
    private String gravatarId;
    private String url;
    @JsonProperty("html_url")
    private String htmlUrl;
    @JsonProperty("followers_url")
    private String followersUrl;
    @JsonProperty("following_url")
    private String followingUrl;
    @JsonProperty("gists_url")
    private String gistsUrl;
    @JsonProperty("starred_url")
    private String starredUrl;
    @JsonProperty("subscriptions_url")
    private String subscriptionsUrl;
    @JsonProperty("organizations_url")
    private String organizationsUrl;
    @JsonProperty("repos_url")
    private String reposUrl;
    @JsonProperty("events_url")
    private String eventsUrl;
    @JsonProperty("received_events_url")
    private String receivedEventsUrl;
    private String type;
    @JsonProperty("site_admin")
    private boolean siteAdmin;
    private String name;
    private String company;
    private String blog;
    private String location;
    private String email;
    private Boolean hireable;
    private String bio;
    @JsonProperty("twitter_username")
    private String twitterUsername;
    @JsonProperty("public_repos")
    private Integer publicRepos;
    @JsonProperty("public_gists")
    private Integer publicGists;
    private Integer followers;
    private Integer following;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("updated_at")
    private String updatedAt;
} 