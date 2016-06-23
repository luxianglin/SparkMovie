package com.spark.serviceImpl;

import com.spark.dao.recommendMapper;
import com.spark.model.recommendmovie;
import com.spark.service.recommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by luxianglin on 16/6/23.
 */
@Service("Recommend")
public class recommendServiceImpl implements recommendService {
    private recommendMapper rDao;

    public recommendMapper getrDao() {
        return rDao;
    }


    @Autowired
    public void setrDao(recommendMapper rDao) {
        this.rDao = rDao;
    }

    public recommendmovie getMoviesById(String userid) {
        return this.rDao.SelectRecommendByUser(userid);
    }
}
