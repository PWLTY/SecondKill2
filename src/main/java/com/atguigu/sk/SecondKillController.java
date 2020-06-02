package com.atguigu.sk;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import redis.clients.jedis.Jedis;

@Controller
public class SecondKillController {
    static String secKillScript = "local userid=KEYS[1];\r\n"
            + "local prodid=KEYS[2];\r\n"
            + "local qtkey='sk:'..prodid..\":qt\";\r\n"
            + "local usersKey='sk:'..prodid..\":usr\";\r\n"
            + "local userExists=redis.call(\"sismember\",usersKey,userid);\r\n"
            + "if tonumber(userExists)==1 then \r\n"
            + "   return 2;\r\n"
            + "end\r\n"
            + "local num= redis.call(\"get\" ,qtkey);\r\n"
            + "if tonumber(num)<=0 then \r\n"
            + "   return 0;\r\n"
            + "else \r\n"
            + "   redis.call(\"decr\",qtkey);\r\n"
            + "   redis.call(\"sadd\",usersKey,userid);\r\n"
            + "end\r\n"
            + "return 1";

    @PostMapping(value = "/sk/doSecondKill",produces = "text/html;charset=UTF-8")
    public String doSkByLUA(Integer id){
        //随机生成用户id
        Integer usrid = (int)(10000*Math.random());
        Jedis jedis = new Jedis("192.168.150.128", 6379);
        //加载LUA脚本
        String sha1 = jedis.scriptLoad(secKillScript);
        //将LUA脚本和LUA脚本需要的参数传给redis执行：keyCount：lua脚本需要的参数数量，params：参数列表
        Object obj = jedis.evalsha(sha1, 2, usrid + "", id + "");
        // Long 强转为Integer会报错  ，Lange和Integer没有父类和子类的关系
        int result = (int)((long)obj);
        if(result==1){
            System.out.println("秒杀成功");
            return "ok";
        }else if(result==2){
            System.out.println("重复秒杀");
            return "重复秒杀";
        }else{
            System.out.println("库存不足");
            return "库存不足";
        }
    }

//    @ResponseBody
//    @PostMapping(value = "/sk/doSecondKill",produces = "text/html;charset=UTF-8")
//    public String doSK(Integer id)
//    {
//        //随机生成用户id
//        Integer usrid = (int) (10000 * Math.random());
//        //秒杀商品的id
//        Integer pid=id;
//        //拼接商品库存的key和用户列表集合的key
//        String qtKey="sk:"+pid+":qt";
//        String usrsKey="sk:"+pid+":usr";
//
//        Jedis jedis = new Jedis("192.168.150.128", 6379);
//        //判断该用户是否已经秒杀过
//        if(jedis.sismember(usrsKey, usrid+""))
//        {
//            System.out.println("重复秒杀："+usrid);
//            return "该用户已经秒杀过，请勿重复秒杀";
//        }
//        //2.获取redis中的库存，判断是否足够
//        //对库存的key 进行watch
//        jedis.watch(qtKey);
//        String qtStr=jedis.get(qtKey);
//        if(StringUtils.isEmpty(qtStr))
//        {
//            System.out.println("秒杀尚未开始");
//            return "秒杀尚未开始";
//        }
//        int qtNum=Integer.parseInt(qtStr);
//        System.out.println("库存="+qtNum);
//        if(qtNum<=0)
//        {
//            System.out.println("库存不足");
//            return "库存不足";
//        }
//        //库存足够，秒杀业务
//        //开启redis的组队
//        Transaction multi=jedis.multi();
//        multi.decr(qtKey);
//        multi.sadd("usrsKey", usrid+"");
//        multi.exec();
//        System.out.println("秒杀成功："+usrid);
//        jedis.close();
//        return "ok";
//    }
}
