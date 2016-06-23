package com.spark.service;

import com.spark.model.recommendmovie;

/**
 * Created by luxianglin on 16/6/23.
 */
public interface recommendService {
    public recommendmovie getMoviesById(String userid);
}
