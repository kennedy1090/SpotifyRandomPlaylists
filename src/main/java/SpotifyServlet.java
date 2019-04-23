import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.specification.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static data.SpotifyData.Album;
import static data.SpotifyData.Artist;
import static data.SpotifyData.Playlist;
import static data.SpotifyData.*;
import static java.time.temporal.ChronoUnit.MILLIS;

@WebServlet("/connect")
public class SpotifyServlet extends HttpServlet {
    private static final int GET_SEVERAL_TRACKS_LIMIT = 50;
    private static final int ADD_TO_PLAYLIST_LIMIT = 100;

    private static final String client_id = "e569f82d833549abba621d21f430b026";
    private static final HashMap<String, Song> songsGlobal = new HashMap<>();
    private static final HashMap<String, Map<String, String[]>> storedSessions = new HashMap<>();
    private static final HashMap<String, AccessToken> tokensById = new HashMap<>();
    private static final Timer timer = new Timer(true);
    private final Map<String, SpotifyState> states = new HashMap<>();

    @Override
    public void init() {
        states.put(null, this::sendResponse);
        states.put("response", this::sendResponse);
        states.put("result", this::sendResult);
        states.put("save_playlist", this::sendPlaylist);
    }

    private void sendReauth(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("client_id", client_id);
        req.getRequestDispatcher("/WEB-INF/spotify-redirect.jsp").forward(req, resp);
    }
    private void sendResponse(HttpServletRequest req, HttpServletResponse resp,
                              SpotifyApi spotifyApi) throws ServletException, IOException {
        HashMap<String, Album> albums = new HashMap<>();
        HashMap<String, Song> songs = new HashMap<>();
        HashMap<String, Artist> artists = new HashMap<>();
        HashMap<String, Playlist> playlists = new HashMap<>();
        try {
            Paging<SavedTrack> tracks;
            int offset = 0;
            do {
                tracks = spotifyApi.getUsersSavedTracks()
                        .limit(50)
                        .offset(offset)
                        .build()
                        .execute();
                for (SavedTrack st : tracks.getItems()) {

                    Song s = getOrAdd(songsGlobal, st.getTrack().getId(),
                            Song::new, st.getTrack());
                    songs.put(s.getId(), s);

                    getOrAdd(albums, st.getTrack().getAlbum().getId(), Album::new,
                            st.getTrack().getAlbum()).getSongs().add(s);

                    for(ArtistSimplified as : st.getTrack().getArtists()) {
                        getOrAdd(artists, as.getId(), Artist::new, as).getSongs().add(s);
                    }
                }
                offset += tracks.getLimit();
            } while(tracks.getNext() != null);

            Paging<PlaylistSimplified> playlistsBuffer;
            offset = 0;
            do {
                playlistsBuffer = spotifyApi.getListOfCurrentUsersPlaylists()
                        .limit(50)
                        .offset(offset)
                        .build()
                        .execute();
                for(PlaylistSimplified p : playlistsBuffer.getItems()) {
                    playlists.put(p.getId(), new Playlist(p, songs, spotifyApi));
                }
                offset += playlistsBuffer.getLimit();
            } while (playlistsBuffer.getNext() != null);
        } catch (SpotifyWebApiException e) {
            e.printStackTrace();
        }
        Comparator<Named> comp = (a, b) -> a.getName().compareToIgnoreCase(b.getName());
        req.setAttribute("songs", songs.values().stream().sorted(comp).toArray(Song[]::new));
        req.setAttribute("albums", albums.values().stream().sorted(comp).toArray(Album[]::new));
        req.setAttribute("artists", artists.values().stream().sorted(comp).toArray(Artist[]::new));
        req.setAttribute("playlists", playlists.values().stream().sorted(comp).toArray(Playlist[]::new));
        req.getRequestDispatcher("/WEB-INF/spotify-response.jsp").forward(req, resp);
    }
    private void sendResult(HttpServletRequest req, HttpServletResponse resp,
                            SpotifyApi spotifyApi) throws ServletException, IOException {
        String songsArr = req.getParameter("songs_array");
        String len = req.getParameter("length");
        int length;
        if (len != null) {
            length = Integer.parseInt(len);
        } else {
            String hours = req.getParameter("hours"),
                    minutes = req.getParameter("minutes");
            length = (Integer.parseInt(hours) * 60 + Integer.parseInt(minutes)) * 60 * 1000;
        }
        String[] ss = songsArr.split("_");
        String[] missing = Arrays.stream(ss)
                .unordered()
                .filter(s -> !songsGlobal.containsKey(s))
                .toArray(String[]::new);
        for(int i = 0; i < missing.length; i += GET_SEVERAL_TRACKS_LIMIT) {
            String[] temp = Arrays.copyOfRange(missing, i, Math.min(missing.length, i + GET_SEVERAL_TRACKS_LIMIT));
            try {
                final Track[] tracks = spotifyApi.getSeveralTracks(temp).build().execute();
                log(Arrays.toString(tracks));
                log(Arrays.toString(temp));
                for(Track tr : tracks) {
                    songsGlobal.put(tr.getId(), new Song(tr));
                }
            } catch (SpotifyWebApiException e) {
                e.printStackTrace();
            }
        }
        PlaylistOption[] options = new PlaylistOption[5];
        int resLength;
        for(int i = 0; i < options.length; i++) {
            ArrayList<Song> playlist = new ArrayList<>();
            List<String> temp;
            do {
                playlist.clear();
                //removing from temp ensures no duplicates
                temp = new ArrayList<>(Arrays.asList(ss));
                int current, next = length;
                do {
                    Song s = songsGlobal.get(temp.remove((int) (Math.random() * temp.size())));
                    current = next;
                    next -= s.getLength();
                    if (Math.abs(next) < current) {
                        playlist.add(s);
                    }
                } while (temp.size() > 0 && Math.abs(next) < current);
                resLength = length - current;
            } while (Math.abs(resLength - length) > 0.05 * length);
            options[i] = new PlaylistOption("Option #"+i+": "+ msToHMS(resLength), playlist);
        }
        //set jsp data
        req.setAttribute("options", options);
        req.setAttribute("length", length);
        req.setAttribute("songs_array", songsArr);
        req.getRequestDispatcher("/WEB-INF/spotify-result.jsp").forward(req, resp);
    }
    private void sendPlaylist(HttpServletRequest req, HttpServletResponse resp,
                              SpotifyApi spotifyApi) throws IOException {
        try {
            String userId = spotifyApi.getCurrentUsersProfile().build().execute().getId();
            String playlistId = spotifyApi.createPlaylist(userId, req.getParameter("playlist_name"))
                    .public_(false)
                    .build()
                    .execute()
                    .getId();
            log("created playlist " + playlistId + " for user " + userId);
            String[] tracks = req.getParameter("playlist_songs").split("_");
            for (int i = 0; i < tracks.length; i++) {
                tracks[i] = "spotify:track:"+tracks[i];
            }
            for(int i = 0; i < tracks.length; i += ADD_TO_PLAYLIST_LIMIT) {
                String[] temp = Arrays.copyOfRange(tracks, i, Math.min(i + ADD_TO_PLAYLIST_LIMIT, tracks.length));
                log(Arrays.toString(temp));
                spotifyApi.addTracksToPlaylist(playlistId, temp).build().execute();
            }
            resp.setContentType("text/plain");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write("https://open.spotify.com/embed/user/"+userId+"/playlist/"+playlistId);
        } catch (SpotifyWebApiException e) {
            log("Create Playlist Error", e);
        }
    }

    private String msToHMS(int ms) {
        int sec = ms/1000;
        int mins = sec/60;
        return (mins/60) + "h" + (mins % 60) + "m" + (sec % 60) + "s";
    }


    private <T, R> R getOrAdd(HashMap<String, R> map, String s, Function<T, R> ctor, T arg) {
        R item = map.get(s);
        if(item == null) {
            item = ctor.apply(arg);
            map.put(s, item);
        }
        return item;
    }
    private void saveSession(HttpServletRequest req) {
        storedSessions.put(req.getSession().getId(), new HashMap<>(req.getParameterMap()));
    }

    private void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String sessionId = req.getSession().getId();
        //specifically use req.getParameter so it's not cached
        AccessToken token = tokensById.get(sessionId);
        String accessToken = req.getParameter("access_token");
        String expiresIn = req.getParameter("expires_in");
        if(accessToken != null && expiresIn != null) {
            token = new AccessToken(accessToken, expiresIn);
            tokensById.put(sessionId, token);
            log("created token for session " + sessionId + " from params");
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    tokensById.remove(sessionId);
                    AccessToken.remove(accessToken);
                }
            }, Instant.now().until(token.expires(), MILLIS));
        }
        if(token == null && accessToken != null) {
            token = AccessToken.get(accessToken);
        }
        log("using token " + token);
        if(token != null && !token.expired()) {
            if(!storedSessions.containsKey(sessionId)) {
                SpotifyApi spotifyApi = new SpotifyApi.Builder()
                        .setAccessToken(token.getToken())
                        .build();
                String state = req.getParameter("state");
                log("state " + state);
                states.get(state).doState(req, resp, spotifyApi);
            } else {
                //redirect so that we can add the saved session to the client-side query string
                String query = storedSessions.remove(sessionId).entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue()[0])
                        .collect(Collectors.joining("&"));
                resp.sendRedirect("connect?"+query);
            }
        } else {
            if("result".equals(req.getParameter("state"))) saveSession(req);
            req.setAttribute("request_url", req.getRequestURL());
            sendReauth(req, resp);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req, resp);
    }
}
