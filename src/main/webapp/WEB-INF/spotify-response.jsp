<%--
  Created by IntelliJ IDEA.
  User: will
  Date: 4/18/19
  Time: 4:15 AM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
    <title>Spotify Random Playlists</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/jstree/3.2.1/themes/default/style.min.css" />
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
    <script src="https://cdnjs.cloudflare.com/ajax/libs/jquery/1.12.1/jquery.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/jstree/3.2.1/jstree.min.js"></script>
    <script>
        let prefixes = ["song_", "arti_", "play_", "albu_"];
        let divs = {"song_": "Songs", "arti_": "Artists", "play_": "Playlists", "albu_": "Albums"};
        let songs = {};
        let currentTab = "";
        let to = false;
        let search_enabled = false;

        function onLoad() {
            for(let tab of $(".tabcontent")) {
                $(tab).jstree({
                    'checkbox': {
                        tie_selection: false
                    },
                    'search': {
                        show_only_matches: true,
                        show_only_matches_children: true
                    },
                    'plugins': ["checkbox", "search"]
                });
                $(tab).on('check_node.jstree uncheck_node.jstree', function (e, data) {
                    if(data.node.li_attr["class"] === "parent") {
                        for(let child of data.node.children) {
                            var id = child.substr(5);
                            updateSong(id, data.node.state.checked);
                        }
                    } else {
                        updateSong(data.node.id.substr(5), data.node.state.checked);
                    }
                });
                $("#submit_songs").submit(function() {
                    $("#songs_array").val(Object.entries(songs).filter(x => x[1]).map(x => x[0]).join("_"));
                })
            }
            if(search_enabled) {
                $("#search").keyup(function (e) {
                    if (to) {
                        clearTimeout(to);
                    }
                    to = setTimeout(function () {
                        searchCurrentTab();
                    }, 250);
                });
            }
        }
        function searchCurrentTab() {
            var val = $("#search").val();
            var tab = $("#" + currentTab).jstree(true);
            if(val.length < 3) {
                tab.clear_search();
            } else {
                tab.search(val);
            }
        }
        function updateSong(id, checked) {
            console.log("Updated " + id + " to " + checked);
            if(songs[id] !== checked) {
                songs[id] = checked;
                for (let prefix of prefixes) {
                    var node = "#" + prefix + id;
                    var inst = $("#"+divs[prefix]).jstree(true);
                    checked ? inst.check_node(node) : inst.uncheck_node(node);
                }
            }
        }
        function openTab(button) {
            for (let tab of $(".tabcontent")) {
                tab.style.display = "none";
            }
            for (let tab of $(".tablinks")) {
                tab.className = tab.className.replace(" active", "");
            }
            if(currentTab !== button.firstChild.data) {
                var tab = $("#" + button.firstChild.data);
                tab.css("display", "block");
                button.className += " active";
                currentTab = button.firstChild.data;
                if (search_enabled) {
                    searchCurrentTab();
                    $("#search_div").css("display", "block");
                }
            } else {
                currentTab = "";
                $("#search_div").css("display", "none");
            }
        }
        window.onload = onLoad;
    </script>
</head>
<body>
<div id="search_div" style="display:none">Search <br/>
<input id="search" type="text">
</div>
<div class="tab">
    <button class="tablinks" onclick="openTab(this)">Songs</button>
    <button class="tablinks" onclick="openTab(this)">Artists</button>
    <button class="tablinks" onclick="openTab(this)">Albums</button>
    <button class="tablinks" onclick="openTab(this)">Playlists</button>
</div>
<div id="Songs" class="tabcontent">
    <ul>
        <c:forEach items="${songs}" var="song">
            <li id="song_${song.id}">${song.name}</li></c:forEach>
    </ul>
</div>
<div id="Artists" class="tabcontent">
    <ul>
        <c:forEach items="${artists}" var="item">
            <li class="parent">${item.name}
                <ul>
                    <c:forEach items="${item.songs}" var="song">
                        <li id="arti_${song.id}">${song.name}</li></c:forEach>
                </ul>
            </li>
        </c:forEach>
    </ul>
</div>
<div id="Albums" class="tabcontent">
    <ul>
        <c:forEach items="${albums}" var="item">
            <li class="parent">${item.name}
                <ul>
                    <c:forEach items="${item.songs}" var="song">
                        <li id="albu_${song.id}">${song.name}</li></c:forEach>
                </ul>
            </li>
        </c:forEach>
    </ul>
</div>
<div id="Playlists" class="tabcontent">
    <ul>
        <c:forEach items="${playlists}" var="item">
            <li class="parent">${item.name}
                <ul>
                    <c:forEach items="${item.songs}" var="song">
                        <li id="play_${song.id}">${song.name}</li></c:forEach>
                </ul>
            </li>
        </c:forEach>
    </ul>
</div>
<form id="submit_songs" action="connect" method="GET">
    <table>
        <tr>
            <td>Hours:</td>     <td><input type="number" name="hours" id="hours" min="0" value="0" required> </td>
        </tr>
        <tr>
            <td>Minutes:</td>   <td><input type="number" name="minutes" id="minutes" min="0" value="0" required> </td>
        </tr>
    </table>
    <input type="hidden" name="songs_array" id="songs_array"> <br/>
    <input type="hidden" name="state" value="result">
    <input type="submit" value="Submit">
</form>
</body>
</html>
