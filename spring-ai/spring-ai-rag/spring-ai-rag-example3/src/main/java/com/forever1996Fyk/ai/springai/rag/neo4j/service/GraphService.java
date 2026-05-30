package com.forever1996Fyk.ai.springai.rag.neo4j.service;

import com.forever1996Fyk.ai.springai.rag.neo4j.model.DirectorMoviesDto;
import com.forever1996Fyk.ai.springai.rag.neo4j.repo.MovieGraphRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/29 22:18
 **/
@Service
public class GraphService {
    @Autowired
    private MovieGraphRepository movieGraphRepository;

    public String retrieveContext(String movieName) {
        List<DirectorMoviesDto> result = movieGraphRepository.findOtherMoviesBySameDirector(movieName);
        if (CollectionUtils.isEmpty(result)) {
            return "未找到导演过《" + movieName + "》的导演的其他作品。";
        }
        StringBuilder sb = new StringBuilder();
        for (DirectorMoviesDto directorMoviesDto : result) {
            sb.append(String.format("- 导演 %s 还执导了：%s\n", directorMoviesDto.getDirector(), String.join("、", directorMoviesDto.getOtherMovies())));
        }
        return sb.toString();
    }
}
