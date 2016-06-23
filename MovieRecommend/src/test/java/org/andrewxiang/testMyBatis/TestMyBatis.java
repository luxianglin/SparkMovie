package org.andrewxiang.testMyBatis;


import com.alibaba.fastjson.JSON;
import com.spark.model.recommendmovie;
import com.spark.service.recommendService;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

@RunWith(SpringJUnit4ClassRunner.class)     //��ʾ�̳���pringJUnit4ClassRunner��
@ContextConfiguration(locations = {"classpath:spring-mybatis.xml"})

public class TestMyBatis {
    private static Logger logger = Logger.getLogger(TestMyBatis.class);
    //  private ApplicationContext ac = null;
    @Resource
    private recommendService Service = null;

    //  @Before
    //  public void before() {  
    //	    ac = new ClassPathXmlApplicationContext("applicationContext.xml");
    //	    userService = (IUserService) ac.getBean("userService");
    //  }

    @Test
    public void test1() {
        recommendmovie user = Service.getMoviesById("moiselletea");
        // System.out.println(user.getUserName());
        // logger.info("ֵ:"+user.getUserName());
        logger.info(JSON.toJSONString(user));
    }
}
