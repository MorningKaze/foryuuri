package mgKze;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.events.*;
import net.mamoe.mirai.message.data.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;

public final class Foryuuri extends JavaPlugin {
    public static final Foryuuri INSTANCE = new Foryuuri();
    public static final SqlOps SQL = new SqlOps();
    public static final OkHttpClient HTTPCLIENT = new OkHttpClient();
    public static final String OSUKEY = "ce189db07ae6ab04b05ca531a4d1feebeed75856";
    public static final String CATKEY = "live_E8w7qsN2brEx37faBukrJjHoN7bv5zaIiNSsbQ4IUoOISw8eq0MFPLf9MptOWKJs";
    public static final long MYGROUPID = 963712049;

    private Foryuuri() {
        super(new JvmPluginDescriptionBuilder(
                "mgKze.foryuuri",
                "2.1.0"
        )
                .name("Foryuuri")
                .author("mgKze")
                .info("made by mgKze 22.1.8")
                .build());
    }

    @Override
    public void onEnable() {
        GlobalEventChannel.INSTANCE.subscribeAlways(BotOnlineEvent.class, (BotOnlineEvent event) ->{
            sqlInit();
            startCheckVtb();
        });

        GlobalEventChannel.INSTANCE.subscribeAlways(NudgeEvent.class, (NudgeEvent event) ->{

        });

        GlobalEventChannel.INSTANCE.subscribeAlways(MemberJoinRequestEvent.class, (MemberJoinRequestEvent event) ->{
            String request = event.getMessage();
            if(request.contains("mcmod"))
                event.accept();
        });

        GlobalEventChannel.INSTANCE.subscribeAlways(MemberJoinEvent.class, (MemberJoinEvent event) ->{
            if(event.getGroupId() == MYGROUPID) {
                String welcome = "Welcome|･ω･｀)\n" + "请务必仔细阅读群公告\n";
                String ps = "群昵称也麻烦改成游戏ID(mgKze不认识的话清理群的时候可能会被做掉";
                MessageChain messageWelcome = new At(event.getMember().getId()).plus(welcome).plus(ps);
                event.getGroup().sendMessage(messageWelcome);
            }
        });

        GlobalEventChannel.INSTANCE.subscribeAlways(GroupMessageEvent.class, (GroupMessageEvent event) ->{
            String sentence = event.getMessage().contentToString();
            long id = event.getSender().getId();
            long groupId = event.getGroup().getId();
            String checkVtbResult = "";

            //help
            if(sentence.equals("/help")){
                String help = "Foryuuri made by mgKze\n" +
                        "可用指令/功能:\n" +
                        "/list 查询服务器状态\n" +
                        "/osu 查询osu相关数据(\n" +
                        "查询直播状态\n" +
                        "/helpadmin 管理员指令查询\n" +
                        "/meow 发出猫猫图\n" +
                        //"/loafoftheday 今日猫猫面包\n" +
                        "/s 搜mcmod 可能搜不到哦\n" +
                        "解析b站链接";
                event.getSubject().sendMessage(help);
            }
            else if(sentence.equals("/helpadmin")){
                String helpadmin =
                        "Foryuuri made by mgKze\n" +
                        "/delserver 删除可查询服务器\n" +
                        "/addserver 添加可查询服务器\n" +
                        "/delvtb 取消查房\n" +
                        "/addvtb 开始查房";
                event.getSubject().sendMessage(helpadmin);
            }
            else if(sentence.equals("/meow")){
                MessageChain catChain = new MessageChainBuilder().append("喵~\n随着一声猫叫 一张猫猫图掉了出来~\n").append(getCatPic(event.getGroup())).build();
                event.getSubject().sendMessage(catChain);
            }/*
            else if(sentence.equals("/loafoftheday")){
                java.util.Calendar now = java.util.Calendar.getInstance();
                int year = now.get(java.util.Calendar.YEAR);
                int month = now.get(java.util.Calendar.MONTH) + 1;
                int day = now.get(java.util.Calendar.DAY_OF_MONTH);
                MessageChain loafChain = new MessageChainBuilder().append("今日猫猫面包:\n").append(getCatLoaf(year, month, day, event.getGroup())).build();
                event.getSubject().sendMessage(loafChain);
            }*/
            else if(sentence.equals("/osu")){
                String osuHelp = "Foryuuri made by mgKze\n" +
                        "/osu bind xxx 绑定osu! id\n" +
                        "/osu unbind 解绑\n" +
                        "/osu profile 查询osu!信息(仅支持std)\n" +
                        "/osu recent 查询最近游玩数据(24h内)";
                event.getSubject().sendMessage(osuHelp);
            }
            //servercheck
            else if(sentence.equals("/list")){
                StringBuilder result = new StringBuilder("目前可查询服务器: \n");
                String[] servers = SQL.listServer();
                for(int i = 0; i < servers.length; i++)
                    result.append(i + 1).append(". ").append(servers[i]).append("\n");
                result.append("查询指令 /list 服务器名");
                event.getSubject().sendMessage(result.toString());
            }
            else if(sentence.startsWith("/list ")){
                if(sentence.length() == 6 || sentence.lastIndexOf(" ") != 5){
                    event.getSubject().sendMessage("输入错误:(");
                }
                else{
                    String name = sentence.substring(sentence.lastIndexOf(" ") + 1);
                    String ip = SQL.getIP(name);
                    if(ip.equals("null"))
                        event.getSubject().sendMessage("没有找到\"" + name + "\"这个服务器QAQ");
                    else
                        event.getSubject().sendMessage(checkServer(ip, event.getGroup()));
                }
            }
            //vtbcheck
            else if(sentence.equals("/listvtb")){
                long[][] vtbs = SQL.listVtb();
                StringBuilder result = new StringBuilder();
                for (long[] vtb : vtbs)
                    result.append(vtb[0]).append("\n");
                event.getSubject().sendMessage(result.toString());
            }
            else if(sentence.startsWith("/delvtb ")){
                if(isOp(id)){
                    String uid = sentence.substring(sentence.lastIndexOf(" "));
                    if(SQL.delVtb(uid, "" + groupId))
                        event.getSubject().sendMessage("已删除" + uid);
                    else
                        event.getSubject().sendMessage("删除失败");
                }
                else
                    event.getSubject().sendMessage("权限不足");
            }
            else if(sentence.startsWith("/addvtb ")){
                if(isOp(id)){
                    String uid = sentence.substring(sentence.lastIndexOf(" "));
                    if(SQL.addVtb(uid, "" + groupId))
                        event.getSubject().sendMessage("已添加" + uid);
                    else
                        event.getSubject().sendMessage("添加失败");
                }
                else
                    event.getSubject().sendMessage("权限不足");
            }
            //osucheck
            else if(sentence.startsWith("/osu bind ")){
                String osuid = sentence.substring(sentence.lastIndexOf(" ")+1);
                if(SQL.isPlayerExist(osuid))
                    event.getSubject().sendMessage("此osu! id已被绑定~");
                else{
                    if(!SQL.findPlayerByQQ(id + "").equals("null"))
                        event.getSubject().sendMessage("你的QQ已经绑定过了哦~");
                    else{
                        if(checkProfile(osuid).equals(""))
                            event.getSubject().sendMessage("没有找到osu! id叫" + osuid + "的osu玩家哦~");
                        else{
                            if(SQL.addOsu(osuid, id + ""))
                                event.getSubject().sendMessage("已将你的QQ " + id + " 绑定到osu! id " + osuid);
                            else
                                event.getSubject().sendMessage("绑定失败!");
                        }
                    }
                }
            }
            else if(sentence.equals("/osu profile")){
                String osuid = SQL.findPlayerByQQ(id + "");
                if(osuid.equals("null"))
                    event.getSubject().sendMessage("请先使用/osu bind xxx指令绑定osu! id~");
                else
                    event.getSubject().sendMessage(checkProfile(osuid));
            }
            else if(sentence.equals("/osu recent")){
                String osuid = SQL.findPlayerByQQ(id + "");
                if(osuid.equals("null"))
                    event.getSubject().sendMessage("请先使用/osu bind xxx指令绑定osu! id~");
                else
                    event.getSubject().sendMessage(checkRecent(osuid, event.getGroup()));
            }
            else if(sentence.equals("/osu unbind")){
                String osuid = SQL.findPlayerByQQ(id + "");
                if(osuid.equals("null"))
                    event.getSubject().sendMessage("你还没有绑定osu! id~");
                else
                {
                    if(SQL.delOsu(osuid))
                        event.getSubject().sendMessage("解绑成功~");
                    else
                        event.getSubject().sendMessage("解绑失败~");
                }
            }
            else if(sentence.startsWith("/delserver ")){
                if(isOp(id)){
                    String name = sentence.substring(sentence.lastIndexOf(" ") + 1);
                    if(SQL.delServer(name))
                        event.getSubject().sendMessage("已删除" + name);
                    else
                        event.getSubject().sendMessage("删除失败");
                }
                else
                    event.getSubject().sendMessage("权限不足");
            }
            else if(sentence.startsWith("/addserver ")){
                if(isOp(id)){
                    String ip = sentence.substring(sentence.lastIndexOf(" ") + 1);
                    String s2 = sentence.substring(0, sentence.lastIndexOf(" "));
                    String name = s2.substring(s2.lastIndexOf(" ") + 1);
                    if(SQL.addServer(name, ip))
                        event.getSubject().sendMessage("已添加" + name);
                    else
                        event.getSubject().sendMessage("添加失败");
                }
                else
                    event.getSubject().sendMessage("权限不足");
            }
            else if(sentence.startsWith("/s ")){
                String key = sentence.substring(sentence.indexOf(" ") + 1);
                event.getSubject().sendMessage(searchMcmod(key));
            }
            else if(sentence.startsWith("/wyy ")){
                String key = sentence.substring(sentence.indexOf(" ") + 1);
                event.getSubject().sendMessage(searchCloudMusic(key));
            }
            else if(sentence.startsWith("https://live.bilibili.com/") ||
                    sentence.startsWith("https://www.bilibili.com/video/")){
                String link = sentence;
                if(sentence.contains("?"))
                    link = sentence.substring(0, sentence.indexOf("?"));
                event.getSubject().sendMessage(resolveBLink(link, event.getGroup()));
            }
            else if(sentence.startsWith("#查询") && sentence.endsWith("状态#")){
                String chaid = sentence.replaceAll("#查询", "").replaceAll("状态#", "");
                if(chaid.matches("[a-zA-Z0-9_]+")){
                    String[] allServer = SQL.listServer();
                    for (String server: allServer)
                        if(checkServer(SQL.getIP(server), event.getGroup()).contains(chaid))
                            event.getSubject().sendMessage(chaid + "在" + server + "玩");
                }
            }
            else if(groupId == 485772581 && sentence.contains("Fate")){
                Bot.getInstances().get(0).getGroup(790872710).sendMessage(event.getMessage());
            }
        });
    }

    private void startCheckVtb() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                String[][] checkVtbResults = checkVtbs();
                ArrayList<String> groups = new ArrayList<>();
                for(String[] checkVtbResult : checkVtbResults){
                    //System.out.println("Group:" + checkVtbResult[1] + "\nResult:" + checkVtbResult[0]);
                    String group = checkVtbResult[1];
                    if(!groups.contains(group))
                        groups.add(group);
                }

                String[] resultMessages = new String[groups.size()];
                for(int i = 0; i < resultMessages.length; i++)
                    resultMessages[i] = "";

                for(String[] checkVtbResult: checkVtbResults) {
                    if(checkVtbResult[0] != "")
                        resultMessages[groups.indexOf(checkVtbResult[1])] += checkVtbResult[0] + "\n";
                }

                for(String group: groups){
                    String message = resultMessages[groups.indexOf(group)];
                    if(!message.equals("")){
                        message = message.substring(0, message.lastIndexOf('\n'));
                        Bot.getInstances().get(0).getGroup(Long.parseLong(group)).sendMessage(message);
                    }
                }
            }
        },1000, 1 * 60 * 1000);
    }

    public static void sqlInit(){
        try {
            SQL.connect();
            SQL.createTables();
            System.out.println("[+]SQL Init Success!");
        }
        catch (SQLException e){
            e.printStackTrace();
            System.out.println("[-]SQL Init Failed!");
        }
        catch (ClassNotFoundException e){
            e.printStackTrace();
            System.out.println("[-]SQL Connect Failed!");
        }
    }

    public static boolean isOp(long qqId){
        return qqId == 373852611;
    }


    private static Image getCatPic(Contact contact) {
        String catUrl = "https://api.thecatapi.com/v1/images/search";
        //String catUrl = "https://api.thecatapi.com/v1/images/search?api_key=" + CATKEY;
        Image cat = null;

        Request request = new Request.Builder()
                .url(catUrl)
                .header( "User-Agent" ,  "yuuri" )
                .addHeader( "Accept" ,  "text/html" )
                .build();

        Response response = null;

        try {
            response = HTTPCLIENT.newCall(request).execute();
            JSONArray catJson =(JSONArray)JSONArray.parse(response.body().string());
            String catPicUrl = catJson.getJSONObject(0).getString("url");
            System.out.println("这次获取到的猫猫图的url: " + catPicUrl);
            Image catImage = getImageFrom(catPicUrl, contact);
            cat = catImage;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return cat;
    }

    /*
    public static Image getCatLoaf(int year, int month, int day, Contact contact){
        String loafUrl = "https://www.kittyloaf.com/loaf-of-the-day-"
         + ((month>=10)?"":"0") + month + ((day>=10)?"":"0") + (day-1) + (year - 2000);
        Image catLoaf = null;

        String result = null;
        try{
            Document doc = Jsoup.connect(loafUrl).get();
            Element loafDiv = doc.getElementsByClass("featured-image").first();
            Element loafImg = loafDiv.select("img").get(0);
            String loafLink = loafImg.attr("src");
            catLoaf = getImageFrom(loafLink, contact);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return catLoaf;
    }
    */

    public static String checkVtb(String uid){
        Request request = new Request.Builder()
                .url("https://api.bilibili.com/x/space/acc/info?mid=" + uid)
                .header( "User-Agent" ,  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 Safari/537.36" )
                .addHeader( "Accept" ,  "text/html" )
                .build();

        Response response = null;
        try{
            response = HTTPCLIENT.newCall(request).execute();
            JSONObject jsonObject = JSON.parseObject(response.body().string());
            boolean liveStatus = JSON.parseObject(JSON.parseObject(jsonObject.getString("data")).getString("live_room")).getInteger("liveStatus") == 1;
            String name = JSON.parseObject(jsonObject.getString("data")).getString("name");
            String url = JSON.parseObject(JSON.parseObject(jsonObject.getString("data")).getString("live_room")).getString("url");
            if(url.contains("?"))
                url = url.substring(0, url.indexOf("?"));
            String title = JSON.parseObject(JSON.parseObject(jsonObject.getString("data")).getString("live_room")).getString("title");
            if (liveStatus){
                if(!SQL.isLive(uid)){
                    SQL.setLive(uid, 1);
                    return name + "播了\n" + title + "\n" + url;
                }
                else
                    return "";
            }
            else {
                if (SQL.isLive(uid)){
                    SQL.setLive(uid, 0);
                    return name + "摸了";
                }
                else return "";
            }
        }
        catch (IOException e){
            e.printStackTrace();
            return "";
        }
    }

    public static String[][] checkVtbs(){
        long[][] vtbs = SQL.listVtb();
        String[][] result = new String[vtbs.length][2];
        for (int i = 0; i < vtbs.length; i++) {
            result[i][0] = checkVtb("" + vtbs[i][0]);
            result[i][1] = "" + vtbs[i][1];
        }
        return result;
    }

    public static MessageChain checkServer(String ip, Contact contact){
        try {
            MessageChainBuilder chainBuilder = new MessageChainBuilder();
            Reply reply = PingMCServer.getReply(ip);
            Image icon = null;

            String result = "在线人数: ";
            Reply.Players players = reply.getPlayers();
            String iconBase = reply.getFavicon();
            if(iconBase != null) {
                iconBase = iconBase.replaceAll("\n", "");
                icon = Base64ToImage(iconBase, contact);
            }

            int online = players.getOnline();
            int max = players.getMax();
            result += online + "/" + max;

            if(online != 0) {
                result += '\n';
                for (Reply.Player player : players.getSample()) {
                    result += player.getName() + " ";
                }
            }
            if(icon != null) chainBuilder.append(icon).append(result);
            else chainBuilder.append("这个服务器好像没有图标~\n").append(result);
            return chainBuilder.build();
        }
        catch (Exception e){
            e.printStackTrace();
            return new MessageChainBuilder().append("查询失败喵~").build();
        }
    }

    public static String checkProfile(String id){
        Request request = new Request.Builder()
                .url("https://osu.ppy.sh/api/get_user?u=" + id + "&k=" + OSUKEY)
                .header( "User-Agent" ,  "yuuri" )
                .addHeader( "Accept" ,  "text/html" )
                .build();

        Response response = null;
        try {
            response = HTTPCLIENT.newCall(request).execute();
            String body = response.body().string();
            if(body.length() < 10)
                return "null";
            JSONArray objects = JSON.parseArray(body);
            JSONObject jsonObject = (JSONObject)objects.get(0);
            String username = jsonObject.getString("username");
            String pp_raw = jsonObject.getString("pp_raw");
            String pp_rank = jsonObject.getString("pp_rank");
            String level = jsonObject.getString("level");

            return "玩家ID: " + username + "\n" +
                    "等级: " + level.split("\\.")[0] + "\n" +
                    "PP: " + pp_raw.split("\\.")[0] + "\n" +
                    "排名: " + pp_rank;
        }
        catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static MessageChain checkRecent(String id, Contact contact){
        MessageChain chain = new MessageChainBuilder()
                .append(new PlainText("未查询到最近(24h内)游玩数据~"))
                .build();

        try {
            Document docRecent = Jsoup.connect("https://osu.ppy.sh/api/get_user_recent?u=" + id + "&k=" + OSUKEY).ignoreContentType(true).get();
            Element preRecent = docRecent.body();
            String textRecent = preRecent.text();
            JSONArray objects = JSON.parseArray(textRecent);
            JSONObject object = (JSONObject) objects.get(0);

            String beatmap_id = object.getString("beatmap_id");
            Document docMap = Jsoup.connect("https://osu.ppy.sh/api/get_beatmaps?b=" + beatmap_id + "&k=" + OSUKEY).ignoreContentType(true).get();
            Element preMap = docMap.body();
            String textMap = preMap.text();
            JSONArray objectsMap = JSON.parseArray(textMap);
            JSONObject objectMap = (JSONObject) objectsMap.get(0);
            String title = objectMap.getString("title_unicode");
            if(title == null)
                title = objectMap.getString("title");
            String beatmapset_id = objectMap.getString("beatmapset_id");

            //小孩子不要这么写哦
            double difficultyrating = new BigDecimal(Double.parseDouble(objectMap.getString("difficultyrating"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

            String score = object.getString("score");
            String maxcombo = object.getString("maxcombo");
            String count50 = object.getString("count50");
            String count100 = object.getString("count100");
            String count300 = object.getString("count300");
            String countmiss = object.getString("countmiss");
            String countkatu = object.getString("countkatu");
            String countgeki = object.getString("countgeki");
            String rank = object.getString("rank");

            int a = Integer.parseInt(count50);
            int b = Integer.parseInt(count100);
            int c = Integer.parseInt(count300);
            int d = Integer.parseInt(countmiss);
            double acc = (double)(a * 50 + b * 100 + c * 300) / (a + b + c + d) / 3;
            BigDecimal big = new BigDecimal(acc);
            acc = big.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

            int lenScore = score.length();

            URL url = new URL("https://assets.ppy.sh/beatmaps/" + beatmapset_id + "/covers/cover.jpg");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5 * 1000);
            InputStream inputStream = conn.getInputStream();
            Image cover = Contact.uploadImage(contact, inputStream);

            String result =
                    "歌名: " + title + "\t难度: " + difficultyrating + "*\n" +
                    "总分: " + score + "\t评级: " + rank + "\n" +
                    "300: " + count300 + "\t激: " + countgeki + "\n" +
                    "100: " + count100 + "\t喝: " + countkatu + "\n" +
                    "50:  " + count50 + "\tmiss: " + countmiss + "\n" +
                    "Combo: " + maxcombo + "\tAcc:" + acc + "%" + "\n" +
                    "Generated by Yuuri~";
            chain = new MessageChainBuilder()
                    .append(cover)
                    .append(new PlainText(result))
                    .build();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return chain;
    }

    public static String searchMcmod(String key){
        String result = null;
        try{
            Document doc = Jsoup.connect("https://search.mcmod.cn/s?key=" + key).get();
            Element searchResult = doc.getElementsByClass("search-result").first();
            Element searchResultList = searchResult.getElementsByClass("search-result-list").first();
            Element firstItem = searchResultList.getElementsByClass("result-item").first();
            Element itemHead = firstItem.getElementsByClass("head").first();
            Element itemEle = itemHead.select("a[href]").get(0);
            String itemName = itemEle.text();
            if(itemName.isEmpty()){
                itemEle = itemHead.select("a[href]").get(1);
                itemName = itemEle.text();
            }
            String itemHref = itemEle.attr("href");
            result = itemName + '\n' + itemHref;
            return result;
        }
        catch (Exception e){
            e.printStackTrace();
            return "搜索失败QAQ";
        }
    }

    public static String searchCloudMusic(String key){
        String result = null;
        try{
            Document doc = Jsoup.connect("https://music.163.com/#/search/m/?s=" + key).get();
            System.out.println(doc.html());
            Element searchResult = doc.getElementsByClass("ztag j-flag").first();
            Element itemHref = searchResult.select("a[href]").get(0);
            Element itemTitle = itemHref.selectFirst("b");
            Element itemAuthor = searchResult.select("a[href]").get(1);

            String href = itemHref.attr("href");
            String title = itemTitle.attr("title");
            String author = itemAuthor.text();

            result = "https://music.163.com" + href + '\n' + itemTitle + " " + author;
            return result;
        }
        catch (Exception e){
            e.printStackTrace();
            return "搜索失败QAQ";
        }
    }

    public static MessageChain resolveBLink(String link, Contact contact){
        Image cover = null;
        String title = "";
        String up = "";
        String imageContent = "";

        MessageChain resolveChain = new MessageChainBuilder()
                .append(new PlainText("解析失败QAQ"))
                .build();

        try {
            Document doc = Jsoup.connect(link).get();

            if(link.contains("live")){ //直播链接 形如 https://live.bilibili.com/6 桀桀 解析太复杂了
                String docS = doc.toString();
                //层层恐惧来咯
                String cover1 = docS.substring(docS.indexOf("\"cover\""));
                String cover2 = cover1.substring(cover1.indexOf(":\"") + 2);
                String cover3 = cover2.substring(0, cover2.indexOf("\""));
                //http:\u002F\u002Fi0.hdslb.com\u002Fbfs\u002Flive\u002Fe2d9e72137d9497c6ddecaeb5d66542df316f5de.png
                //https:\u002F\u002Fi0.hdslb.com\u002Fbfs\u002Flive\u002Fnew_room_cover\u002F4c58650422a9fe096141cf0fc93c6386b36f0631.jpg
                int len = "e2d9e72137d9497c6ddecaeb5d66542df316f5de.png".length();
                if(cover3.contains("new"))
                    imageContent = "https://i0.hdslb.com/bfs/live/new_room_cover/" + cover3.substring(cover3.length() - len);
                else
                    imageContent = "https://i0.hdslb.com/bfs/live/" + cover3.substring(cover3.length() - len);

                String title1 = docS.substring(docS.indexOf("\"title\""));
                String title2 = title1.substring(title1.indexOf(":\"") + 2);
                title = title2.substring(0, title2.indexOf("\""));

                String author1 = docS.substring(docS.indexOf("\"uname\""));
                String author2 = author1.substring(author1.indexOf(":\"") + 2);
                up = author2.substring(0, author2.indexOf("\""));
            }
            else { //视频链接 形如https://www.bilibili.com/video/BV1oR4y1u7jn
                Element heads = doc.head();
                Element metaImage = heads.selectFirst("meta[property=og:image]");
                imageContent = metaImage.attr("content");
                if (imageContent.startsWith("http://"))
                    imageContent = imageContent.replaceAll("http://", "https://");

                Element metaTitle = heads.selectFirst("meta[property=og:title]");
                String titleContent = metaTitle.attr("content");
                title = titleContent.replaceAll("_哔哩哔哩_bilibili", "");

                Element metaAuthor = heads.selectFirst("meta[name=author]");
                String authorContent = metaAuthor.attr("content");
                up = authorContent;
            }

            cover = getImageFrom(imageContent, contact);
            //System.out.println("TEST:" + imageContent);
        }
        catch (Exception e){
            e.printStackTrace();
            return resolveChain;
        }

        resolveChain = new MessageChainBuilder()
                .append(cover)
                .append(new PlainText(title))
                .append(new PlainText("\nup: " + up))
                .build();
        return resolveChain;
    }

    public static Image getImageFrom(String imageContent, Contact contact) throws IOException{
        Image img = null;
        URL url = new URL(imageContent);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5 * 1000);
        InputStream inputStream = conn.getInputStream();
        img = Contact.uploadImage(contact, inputStream);
        return img;
    }

    public static Image Base64ToImage(String base, Contact contact){
        Image img = null;
        base = base.substring(base.indexOf(",", 1) + 1);
        System.out.println(base);
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] bytes = decoder.decode(base);
        InputStream input = new ByteArrayInputStream(bytes);
        img = Contact.uploadImage(contact, input);
        return img;
    }
}

