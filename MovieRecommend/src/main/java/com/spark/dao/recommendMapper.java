package com.spark.dao;

import com.spark.model.recommendmovie;

/**
 * Created by luxianglin on 16/6/23.
 */
public interface recommendMapper {
    recommendmovie SelectRecommendByUser(String id);
}
