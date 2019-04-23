<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%--
  Created by IntelliJ IDEA.
  User: will
  Date: 4/18/19
  Time: 4:16 AM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Spotify Random Playlists</title>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/jquery/1.12.1/jquery.min.js"></script>
    <script>
        let options = {};
        let currentTab = "";
        function onLoad() {
            <c:forEach items="${options}" var="option">
            options["option${fn:substring(option.name, 8, 9)}"] = "${option}";
            </c:forEach>
            $("#save").submit(function(event) {
                $("#playlist_songs").val(options[currentTab]);
                console.log(options[currentTab]);
                event.preventDefault();
                $.post("connect", $("#save").serialize(), function(data) {
                    $("#result").attr("src", data);
                });
            })
        }

        function openTab(button) {
            for (let tab of $(".tabcontent")) {
                tab.style.display = "none";
            }
            for (let tab of $(".tablinks")) {
                $(tab).removeClass("active");
            }
            var divId = button.id.substr(4);
            if(currentTab !== divId) {
                var tab = $("#" + divId);
                tab.css("display", "block");
                button.className += " active";
                currentTab = divId;
                $("#submit_save").removeAttr("disabled");
            } else {
                currentTab = "";
                $("#submit_save").attr("disabled", "disabled");
            }
        }
        window.onload = onLoad;
    </script>
    <style>
        /* Style the tab */
        .tab {
            overflow: hidden;
            border: 1px solid #ccc;
            background-color: #f1f1f1;
        }

        /* Style the buttons inside the tab */
        .tab button {
            background-color: inherit;
            float: left;
            border: none;
            outline: none;
            cursor: pointer;
            padding: 14px 16px;
            transition: 0.3s;
            font-size: 17px;
        }

        /* Change background color of buttons on hover */
        .tab button:hover {
            background-color: #ddd;
        }

        /* Create an active/current tablink class */
        .tab button.active {
            background-color: #ccc;
        }

        /* Style the tab content */
        .tabcontent {
            display: none;
            padding: 6px 12px;
            border: 1px solid #ccc;
            border-top: none;
        }
    </style>
</head>
<body>
<div class="tab">
    <c:forEach var="item" items="${options}">
        <button onclick="openTab(this)" class="tablinks" id="btn_option${fn:substring(item.name, 8, 9)}">${item.name}</button>
    </c:forEach>
</div>
<c:forEach var="option" items="${options}">
    <div class="tabcontent" id="option${fn:substring(option.name, 8, 9)}">
            <%--            <iframe src="https://open.spotify.com/embed/track/${item.id}"--%>
            <%--                    width="300" height="80" frameborder="0" allowtransparency="true" allow="encrypted-media"></iframe>--%>
        <ul>
            <c:forEach items="${option.songs}" var="item">
                <li>${item.artist} - ${item.name}</li>
                <br/>
            </c:forEach>
        </ul>
    </div>
</c:forEach>
Playlist Name:<br/>
<form action="connect" name="save" id="save" method="post">
    <input type="text" value="Random Playlist" name="playlist_name" id="playlist_name"> <br/>
    <input type="hidden" name="playlist_songs" id="playlist_songs">
    <input type="hidden" name="state" value="save_playlist">
    <input type="submit" value="Save Playlist" id="submit_save" disabled>
</form>
<form action="connect" name="redo" id="redo" method="get">
    <input type="hidden" name="length" id="length" value="${length}">
    <input type="hidden" name="state" value="result">
    <input type="hidden" name="songs_array" id="songs_array" value="${songs_array}">
    <input type="submit" value="More Options">
</form>
<form action="connect" name="back" id="back" method="get">
    <input type="hidden" name="state" value="response">
    <input type="submit" value="Back">
</form>
<iframe width="300" height="380" frameborder="0" allowtransparency="true" allow="encrypted-media" id="result"></iframe>
</body>
</html>
