package com.forever1996Fyk.ai.springai.rag.neo4j.model;

import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/29 22:17
 **/
public class DirectorMoviesDto {
    private String director;
    private List<String> otherMovies;

    public DirectorMoviesDto() {
    }

    public DirectorMoviesDto(String director, List<String> otherMovies) {
        this.director = director;
        this.otherMovies = otherMovies;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public List<String> getOtherMovies() {
        return otherMovies;
    }

    public void setOtherMovies(List<String> otherMovies) {
        this.otherMovies = otherMovies;
    }
}
