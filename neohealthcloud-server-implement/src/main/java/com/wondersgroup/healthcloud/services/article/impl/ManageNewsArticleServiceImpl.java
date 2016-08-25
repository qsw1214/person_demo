package com.wondersgroup.healthcloud.services.article.impl;

import com.wondersgroup.healthcloud.common.utils.AppUrlH5Utils;
import com.wondersgroup.healthcloud.jpa.entity.article.ArticleArea;
import com.wondersgroup.healthcloud.jpa.entity.article.NewsArticle;
import com.wondersgroup.healthcloud.jpa.repository.article.NewsArticleRepo;
import com.wondersgroup.healthcloud.services.article.ManageNewsArticleService;
import com.wondersgroup.healthcloud.services.article.dto.NewsArticleListAPIEntity;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.*;

/**
 * Created by Administrator on 2015/12/30.
 */
@Service("manageNewsArticleServiceImpl")
public class ManageNewsArticleServiceImpl implements ManageNewsArticleService{

    @Autowired
    private NewsArticleRepo newsArticleRepo;

    @Autowired
    private AppUrlH5Utils appUrlH5Utils;

    @Autowired
    private DataSource dataSource;
    private JdbcTemplate jt;


    @Override
    public NewsArticle findArticleInfoById(int id) {
        NewsArticle newsArticle = newsArticleRepo.queryArticleById(id);
        if (newsArticle==null) return null;
        String url=appUrlH5Utils.buildNewsArticleView(id);
        newsArticle.setUrl(url);
        return newsArticleRepo.queryArticleById(id);
    }

    @Override
    public List<NewsArticle> findArticleListByIds(List<Integer> ids) {
        return null;
    }

    @Override
    public List<NewsArticle> findArtileListByKeys(Map<String, Object> parm) {
        return null;
    }


    @Override
    public int updateNewsAritile(NewsArticle article) {
        Date date=new Date();
        article.setUpdate_time(date);
        return newsArticleRepo.saveAndFlush(article).getId();
    }

    @Override
    public List<NewsArticle> findListByCategoryId(String categoryId, int pageNo, int pageSize) {

        return newsArticleRepo.queryNewsArticleByCatId(categoryId,pageNo,pageSize);
    }

    @Override
    public List<NewsArticle> findAppShowListByCategoryId(String categoryId, int pageNo, int pageSize) {
        return newsArticleRepo.queryNewsArticleByCatId(categoryId,pageNo,pageSize);
    }

    @Override
    public int countArticleByCategoryId(String categoryId) {
        return 0;
    }
    

    @Override
    public int addViewPv(Integer id) {
        return 0;
    }

    @Override
    public List<NewsArticle> findAppShowListByTitle(String title, int pageNo, int pageSize) {
        return newsArticleRepo.queryNewsArticleByTitle(title,pageNo,pageSize);
    }

    @Override
    public List<NewsArticleListAPIEntity> findArticleForFirst(String areaId, int pageNo, int pageSize) {
        List<NewsArticle> list=newsArticleRepo.queryNewsArticleByAreaId(areaId,pageNo*pageSize,pageSize);

        return getArticleEntityList(list);
    }

    @Override
    public List<NewsArticleListAPIEntity> findCollectionArticle(String uid,int pageNo,int pageSize) {

        List<NewsArticle> newsArticles = newsArticleRepo.queryCollectionNewsArticle(uid, pageNo * pageSize, pageSize);

        return getArticleEntityList(newsArticles);
    }

    @Override
    public List<Map<String, Object>> queryArticleList(Map<String, Object> param) {

        String sql=makeSql(param,1);

        List<Map<String,Object>> results = this.getJt().queryForList(sql);
        return results;
    }

    @Override
    public int getCount(Map param) {
        String sql = makeSql(param,2);
        return this.getJt().queryForObject(sql,Integer.class);
    }


    private List<NewsArticleListAPIEntity> getArticleEntityList(List<NewsArticle> resourceList){

        if(null == resourceList || resourceList.size() == 0){
            return null;
        }
        List<NewsArticleListAPIEntity> list = new ArrayList<>();
        for (NewsArticle article : resourceList){
            list.add(new NewsArticleListAPIEntity(article, appUrlH5Utils));
        }
        return list;
    }

    //组装sql
    private String makeSql(Map searchParam,int type) {
        StringBuffer sql = new StringBuffer();
        int pageSize = (Integer) searchParam.get("pageSize");
        int pageNo = (Integer) searchParam.get("pageNo");
        sql.append("select ");
        if(type == 2){
            sql.append(" count(*) ");
        }else {
            sql.append(" * ");
        }
        if(null==searchParam.get("area_code")){
            sql.append(" from app_tb_neoarticle where 1=1");
            Iterator it = searchParam.keySet().iterator();
            while (it.hasNext()) {

                String key = (String) it.next();

                if ("is_visable".equals(key)) {
                    sql.append(" and "+key+"="+searchParam.get(key));
                }if("start_time".equals(key)){
                    sql.append(" and update_time"+">=,"+searchParam.get(key)+"'");
                }if("end_time".equals(key)){
                    sql.append(" and update_time"+"=<'"+searchParam.get(key)+"'");
                }if("title".equals(key)){
                    sql.append(" and "+key+"='"+searchParam.get(key)+"'");
                }

            }
            sql.append(" order by update_time limit "+(pageNo-1)*pageSize+","+pageSize);
        }else {//分区域查询文章
            sql.append(" from app_tb_neoarticle t1 left join app_tb_neoarticle_area t2 on t1.id=t2.article_id where 1=1");
            Iterator it = searchParam.keySet().iterator();
            while (it.hasNext()) {

                String key = (String) it.next();

                if ("is_visable".equals(key)) {
                    sql.append(" and t2."+key+"="+searchParam.get(key));
                }if("start_time".equals(key)){
                    sql.append(" and t2.update_time"+">="+searchParam.get(key));
                }if("end_time".equals(key)){
                    sql.append(" and t2.update_time"+"=<"+searchParam.get(key));
                }if("title".equals(key)){
                    sql.append(" and "+key+"='"+searchParam.get(key)+"'");
                }
            }
            sql.append(" order by t2.update_time limit "+(pageNo-1)*pageSize+","+pageSize);
        }

        return sql.toString();
    }

    private JdbcTemplate getJt() {
        if (jt == null) {
            jt = new JdbcTemplate(dataSource);
        }
        return jt;
    }
}
