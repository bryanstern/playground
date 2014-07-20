package com.bryanstern.playground.api;

import com.google.gson.annotations.SerializedName;

public class GitHubUser {
    public String getLogin() {
        return login;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    String login;
    @SerializedName("avatar_url")
    String avatarUrl;
}
