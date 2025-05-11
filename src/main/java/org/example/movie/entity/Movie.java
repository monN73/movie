package org.example.movie.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
@Entity
@Table(name = "movielist")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = true, unique = true)
    private String imdbID;

    @Column(nullable = false, unique = true)
    private String uid;

    @Column(nullable = true, unique = true)
    private String tmdbID;

    @Column(nullable = true)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String original_title;

    @Column(nullable = true)
    private String releaseDate;

    @Lob
    @Column(nullable = true,length = 65535)
    private String overview;

    @Column(nullable = true)
    private String posterPath;

    @Column(nullable = true)
    private String backdropPath;

    @Column(nullable = true)
    private int rankToday;

    @Column(nullable = true)
    private int rankWeek;

    @Column(nullable = true)
    private String updateToday;

    @Column(nullable = true)
    private String updateWeek;

    @Column(nullable = true)
    private String boxOffice;

    @Column(nullable = true)
    private String imdbRating;

    @Column(nullable = true)
    private String imdbVotes;

    @Column(nullable = true)
    private String budget;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getImdbID() {
        return imdbID;
    }

    public void setImdbID(String imdbID) {
        this.imdbID = imdbID;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getTmdbID() {
        return tmdbID;
    }

    public void setTmdbID(String tmdbID) {
        this.tmdbID = tmdbID;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOriginal_title() {
        return original_title;
    }

    public void setOriginal_title(String original_title) {
        this.original_title = original_title;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getBackdropPath() {
        return backdropPath;
    }

    public void setBackdropPath(String backdropPath) {
        this.backdropPath = backdropPath;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public void setPosterPath(String posterPath) {
        this.posterPath = posterPath;
    }

    public int getRankToday() {
        return rankToday;
    }

    public void setRankToday(int rankToday) {
        this.rankToday = rankToday;
    }

    public int getRankWeek() {
        return rankWeek;
    }

    public void setRankWeek(int rankWeek) {
        this.rankWeek = rankWeek;
    }

    public String getUpdateToday() {
        return updateToday;
    }

    public void setUpdateToday(String updateToday) {
        this.updateToday = updateToday;
    }

    public String getUpdateWeek() {
        return updateWeek;
    }

    public void setUpdateWeek(String  updateWeek) {
        this.updateWeek = updateWeek;
    }

    public String getBoxOffice() {
        return boxOffice;
    }

    public void setBoxOffice(String boxOffice) {
        this.boxOffice = boxOffice;
    }

    public String getImdbRating() {
        return imdbRating;
    }

    public void setImdbRating(String imdbRating) {
        this.imdbRating = imdbRating;
    }

    public String getImdbVotes() {
        return imdbVotes;
    }

    public void setImdbVotes(String imdbVotes) {
        this.imdbVotes = imdbVotes;
    }

    public String getBudget() {
        return budget;
    }

    public void setBudget(String budget) {
        this.budget = budget;
    }


}