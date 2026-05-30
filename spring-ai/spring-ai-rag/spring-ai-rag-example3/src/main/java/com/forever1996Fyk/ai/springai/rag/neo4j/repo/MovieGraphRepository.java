package com.forever1996Fyk.ai.springai.rag.neo4j.repo;

import com.forever1996Fyk.ai.springai.rag.neo4j.model.Movie;
import com.forever1996Fyk.ai.springai.rag.neo4j.model.DirectorMoviesDto;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/29 22:16
 **/
@Repository
public interface MovieGraphRepository extends Neo4jRepository<Movie, String> {

    @Query("""
            MATCH (m:Movie {title: $title}) <-[:DIRECTED]- (d:Director) -[:DIRECTED]-> (other:Movie)
            WHERE other.title <> $title
            RETURN d.name AS director, collect(other.title + ' (' + other.year + ')') AS otherMovies
            """)
    List<DirectorMoviesDto> findOtherMoviesBySameDirector(String title);
}
