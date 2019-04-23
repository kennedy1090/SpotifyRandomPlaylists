package data;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.specification.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.SECONDS;


public class SpotifyData {
    @FunctionalInterface
    public interface SpotifyState {
        public void doState(HttpServletRequest req, HttpServletResponse resp,
                            SpotifyApi spotifyApi) throws ServletException, IOException;
    }
    public interface Named {
        String getName();
    }
    public static class PlaylistOption implements Named {
        public PlaylistOption(String name, ArrayList<Song> songs) {
            this.name = name;
            this.songs = songs;
        }

        public String getName() {
            return name;
        }

        public ArrayList<Song> getSongs() {
            return songs;
        }

        @Override
        public String toString() {
            return songs.stream().map(Song::getId).collect(Collectors.joining("_"));
        }

        final String name;
        final ArrayList<Song> songs;
    }

    public static class AccessToken {
        static HashMap<String, AccessToken> tokens = new HashMap<>();
        public AccessToken(String accessToken, String expiresIn) {
            this.token = accessToken;
            this.expires = Instant.now().plusSeconds(Long.parseLong(expiresIn));
            tokens.put(this.token, this);
        }

        public boolean expired() {
            return Instant.now().isAfter(expires);
        }

        public static AccessToken get(String token) {
            return tokens.get(token);
        }
        public static void remove(String token) {
            tokens.remove(token);
        }

        public String getToken() {
            return token;
        }

        public Instant expires() {
            return expires;
        }

        @Override
        public String toString() {
            return token + ", expires in " + Instant.now().until(expires, SECONDS) + "s";
        }

        final String token;
        final Instant expires;
    }

    public static class Song implements Named {
        public Song(Track t) {
            this.name = t.getName();
            this.id = t.getId();
            this.length = t.getDurationMs();
            this.artist = t.getArtists()[0].getName();
        }

        public String getName() {
            return name;
        }

        public String getId() {
            return id;
        }

        public String getArtist() {
            return artist;
        }

        public int getLength() {
            return length;
        }

        final String name;
        final String id;
        final String artist;
        final int length;
    }

    public static class Artist implements Named {
        public Artist(ArtistSimplified as) {
            this.songs = new ArrayList<>();
            this.name = as.getName();
            this.id = as.getId();
        }

        public ArrayList<Song> getSongs() {
            return songs;
        }

        public String getName() {
            return name;
        }

        public String getId() {
            return id;
        }

        final ArrayList<Song> songs;
        final String name;
        final String id;
    }

    public static class Album implements Named {
        public Album(com.wrapper.spotify.model_objects.specification.AlbumSimplified a) {
            this.songs = new ArrayList<>();
            this.name = a.getName();
            this.id = a.getId();
        }

        public ArrayList<Song> getSongs() {
            return songs;
        }

        public String getName() {
            return name;
        }

        public String getId() {
            return id;
        }

        final ArrayList<Song> songs;
        final String name;
        final String id;
    }

    public static class Playlist implements Named {
        public Playlist(PlaylistSimplified p, HashMap<String, Song> ids, SpotifyApi spotifyApi) throws IOException, SpotifyWebApiException {
            songs = new ArrayList<>();
            Paging<PlaylistTrack> paging;
            int offset = 0;
            do {
                paging = spotifyApi.getPlaylistsTracks(p.getId())
                            .limit(50)
                            .offset(offset)
                            .build().execute();
                Arrays.stream(paging.getItems()).map(pt -> {
                    Song s = ids.get(pt.getTrack().getId());
                    if(s == null) {
                        s = new Song(pt.getTrack());
                    }
                    return s;
                }).forEach(songs::add);
                offset += paging.getLimit();
            } while (paging.getNext() != null);
            this.name = p.getName();
            this.id = p.getId();
        }

        public ArrayList<Song> getSongs() {
            return songs;
        }

        public String getName() {
            return name;
        }

        public String getId() {
            return id;
        }

        final ArrayList<Song> songs;
        String name;
        String id;
    }
}
