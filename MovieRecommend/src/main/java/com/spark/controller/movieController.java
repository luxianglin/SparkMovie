package com.spark.controller;

import com.spark.model.recommendmovie;
import com.spark.service.recommendService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import java.util.Map;

/**
 * Created by luxianglin on 16/6/23.
 */
@Controller
@RequestMapping("/movie")
public class movieController {
    @Resource
    private recommendService service;

    @RequestMapping(value = "/recommend/{userid}")
    public String rmovies(@PathVariable("userid") String userid, Map<String, Object> model) {
        recommendmovie mv = this.service.getMoviesById(userid);
        model.put("movies", mv);
        return "index";
    }
}
