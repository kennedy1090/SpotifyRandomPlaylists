<%--
  Created by IntelliJ IDEA.
  User: will
  Date: 4/18/19
  Time: 3:03 AM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Spotify Random Playlists</title>
</head>
<script>
    var scopes = ["playlist-read-private", "playlist-modify-private", "user-library-read"];
    var redirectUri = "${request_url}";

    var authorizeUrl = "https://accounts.spotify.com/authorize" +
        "?client_id=${client_id}" +
        "&response_type=token" +
        "&scope=" + scopes.join("%20") +
        "&redirect_uri="+redirectUri.replace("/", "%2F");
    function initSession() {
        if (location.hash && location.hash.includes("access_token")) {
            var url = window.location.toString();
            window.location = url.replace("#", "?") + "&state=response";
        } else {
            window.location = authorizeUrl;
        }
    }
    window.onload = initSession;
</script>
<body>
Redirecting...
</body>
</html>
