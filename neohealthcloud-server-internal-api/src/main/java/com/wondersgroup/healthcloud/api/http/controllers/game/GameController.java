package com.wondersgroup.healthcloud.api.http.controllers.game;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.wondersgroup.healthcloud.api.utils.Pager;
import com.wondersgroup.healthcloud.common.http.dto.JsonResponseEntity;
import com.wondersgroup.healthcloud.common.http.support.misc.JsonKeyReader;
import com.wondersgroup.healthcloud.jpa.entity.game.Game;
import com.wondersgroup.healthcloud.jpa.entity.game.GameScore;
import com.wondersgroup.healthcloud.jpa.entity.user.RegisterInfo;
import com.wondersgroup.healthcloud.jpa.repository.game.GameRepository;
import com.wondersgroup.healthcloud.jpa.repository.game.GameScoreRepository;
import com.wondersgroup.healthcloud.jpa.repository.user.RegisterInfoRepository;
import com.wondersgroup.healthcloud.services.game.GameService;
import com.wondersgroup.healthcloud.services.user.SessionUtil;
import com.wondersgroup.healthcloud.services.user.dto.Session;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

/**
 * Created by zhuchunliu on 2016/8/31.
 */
@RestController
@RequestMapping(value = "/game")
public class GameController {

    @Autowired
    private GameRepository gameRepo;
    @Autowired
    private GameScoreRepository gameScoreRepo;
    @Autowired
    private GameService gameService;
    @Autowired
    private RegisterInfoRepository registerInfoRepo;
    @Autowired
    private SessionUtil sessionUtil;

    /**
     * 获取游戏分数
     * @return
     */
    @PostMapping(path = "/score/list")
    public Pager scoreList(@RequestBody Pager pager){
        Page<GameScore> page = gameService.findAll(pager.getNumber()-1,pager.getSize());
        List<Map> list = Lists.newArrayList();
        for(GameScore gameScore : page.getContent()){
            list.add(ImmutableMap.of("rank",(pager.getNumber()-1) * pager.getSize()+1+list.size(),
                    "nickname",this.getNiceName(gameScore.getRegisterid()),"score",gameScore.getScore()));
        }
        pager.setData(list);
        pager.setTotalElements((int)page.getTotalElements());
        return pager;
    }

    /**
     * 获取分数
     * @param token
     * @return
     */
    @GetMapping(path = "/score/person")
    public JsonResponseEntity getPersonScore(@RequestHeader(name="access-token") String token){
        Session session = sessionUtil.get(token);
        if(null == session || StringUtils.isEmpty(session.getUserId())){
            return new JsonResponseEntity(1001,"token已经过期");
        }
        GameScore gameScore = gameScoreRepo.getByRegisterId(session.getUserId());
        ImmutableBiMap map;
        if(null != gameScore && 0 != gameScore.getScore()){
            float rate = gameService.getScoreRank(session.getUserId(),gameScore.getScore());
            map = ImmutableBiMap.of("score",gameScore.getScore(),"rate",new DecimalFormat("#").format(rate*100)+"%");
        }else{
            map = ImmutableBiMap.of("score","0");
        }
        return new JsonResponseEntity(0,null,map);
    }

    /**
     * 保存用户游戏成绩
     * @param token
     * @return
     */
    @PostMapping(path = "/score/person")
    public JsonResponseEntity setPersonScore(@RequestHeader(name="access-token") String token,@RequestBody String request){
        Session session = sessionUtil.get(token);
        if(null == session || StringUtils.isEmpty(session.getUserId())){
            return new JsonResponseEntity(1001,"token已经过期");
        }
        JsonKeyReader reader = new JsonKeyReader(request);
        Integer score = Integer.parseInt(reader.readString("score", false));
        gameService.updatePersonScore(session.getUserId(),score);
        Float rate = gameService.getScoreRank(session.getUserId(), score);
        return new JsonResponseEntity(0,null,ImmutableBiMap.of("rate",new DecimalFormat("#").format(rate*100)+"%"));
    }

    /**
     * 检测用户是否绑定了手机号
     * @param token
     * @return
     */
    @GetMapping(path = "/check/phone")
    public JsonResponseEntity checkPhone(@RequestHeader(name="access-token") String token){
        Session session = sessionUtil.get(token);
        if(null == session || StringUtils.isEmpty(session.getUserId())){
            return new JsonResponseEntity(1001,"token已经过期");
        }
        RegisterInfo register = registerInfoRepo.findOne(session.getUserId());
        boolean flag = false;
        if(null != register && StringUtils.isNotEmpty(register.getRegmobilephone())){
            flag = true;
        }
        JsonResponseEntity entity = new JsonResponseEntity();
        entity.setData(ImmutableMap.of("flag",flag));
        return entity;
    }

    /**
     * 累计挑战次数
     * plantform 1:app ,2:微信
     * @return
     */
    @PostMapping(path = "/click")
    public JsonResponseEntity click(@RequestBody String request){
        JsonKeyReader reader = new JsonKeyReader(request);
        Integer platform = reader.readInteger("platform", false);
        List<Game> list = gameRepo.findAll();

        Boolean flag = false;
        if(null != list && 0 != list.size()) {
            Game game = list.get(0);
            if(1 == platform){
                game.setAppClick(game.getAppClick() == null ?1:game.getAppClick()+1);
            }else{
                game.setWeixinClick(game.getWeixinClick() == null ? 1 : game.getWeixinClick()+1);
            }
            gameRepo.save(game);
        }
        return new JsonResponseEntity(0,"统计成功");
    }

    /**
     * 累计分享次数
     * plantform 1:app ,2:微信
     * @return
     */
    @PostMapping(path = "/share")
    public JsonResponseEntity share(@RequestBody String request){
        JsonKeyReader reader = new JsonKeyReader(request);
        Integer platform = reader.readInteger("platform", false);
        List<Game> list = gameRepo.findAll();

        Boolean flag = false;
        if(null != list && 0 != list.size()) {
            Game game = list.get(0);
            if(1 == platform){
                game.setAppShare(game.getAppShare() == null ? 1 : game.getAppShare() + 1);
            }else{
                game.setWeixinShare(game.getWeixinShare() == null ? 1 : game.getWeixinShare()+1);
            }
            gameRepo.save(game);
        }
        return new JsonResponseEntity(0,"统计成功");
    }



    /**
     * 获取游戏规则
     * @return
     */
    @GetMapping(path = "/rule")
    public JsonResponseEntity rule(){
        List<Game> list = gameRepo.findAll();
        JsonResponseEntity entity = new JsonResponseEntity();
        entity.setData(ImmutableMap.of("rule",null != list && 0 != list.size()?list.get(0).getRule():""));
        return entity;
    }

    /**
     * 检测是否在游戏时间区间内
     * @return
     */
    @GetMapping(path = "/check")
    public JsonResponseEntity check(){
        List<Game> list = gameRepo.findAll();

        Boolean flag = false;
        if(null != list && 0 != list.size()){
            Game game = list.get(0);
            if(null != game.getStartTime() && null != game.getEndTime()
                    && new DateTime(game.getStartTime()).isBefore(DateTime.now())
                    && new DateTime(game.getEndTime()).isAfter(DateTime.now())){
                flag = true;
            }
        }
        JsonResponseEntity entity = new JsonResponseEntity();
        entity.setData(ImmutableMap.of("flag",flag));
        return entity;
    }

    private Object getNiceName(String registerid) {
        RegisterInfo register = registerInfoRepo.findOne(registerid);
        if(null == register || StringUtils.isEmpty(register.getNickname())){
            return "火星用户";
        }
        String nickName = register.getNickname();
        if(1 == nickName.length()){
            return nickName;
        }else if(2 == nickName.length()){
            return nickName.charAt(0)+"*";
        }else if(3 == nickName.length()){
            return nickName.charAt(0)+"*" + nickName.charAt(2);
        }else if(4 == nickName.length()){
            return nickName.charAt(0)+"**" + nickName.charAt(3);
        }else if(5 == nickName.length()){
            return nickName.charAt(0)+"***" + nickName.charAt(4);
        }else if(6 == nickName.length()){
            return nickName.charAt(0)+"****" + nickName.charAt(5);
        }else if(11 == nickName.length()){
            return nickName.substring(0, 3)+"****" +nickName.substring(7, 11);
        }else{
            StringBuffer sb = new StringBuffer();
            sb.append(nickName.substring(0,4));
            for(int i=0 ;i<nickName.length() - 6; i++){
                sb.append("*");
            }
            return sb.append(nickName.substring(nickName.length()-2)).toString();
        }
    }


}