package com.example.take6server.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class BotProfileService {

    private final List<BotProfile> availableProfiles;
    private final Random random = new Random();

    public BotProfileService() {
        this.availableProfiles = new CopyOnWriteArrayList<>();
        initializeProfiles();
    }

    private void initializeProfiles() {
        // 示例：真实的中文昵称
        String[] names = {
            "快乐小熊", "风一样的男子", "爱吃火锅", "陈大发", "李白白",
            "夜猫子", "阳光彩虹", "吃货小分队", "打工魂", "天天向上",
            "王富贵", "小龙女", "大侠霍元甲", "萌萌哒", "哆啦A梦",
            "蜡笔小新", "樱木花道", "流川枫", "皮卡丘", "杰尼龟",
            "路人甲", "隔壁老王", "不吃香菜", "奶茶续命", "养生达人",
            "熬夜冠军", "铲屎官", "旅行青蛙", "暴躁老哥", "温柔一刀"
        };

        for (String name : names) {
            // 根据名字使用一致的种子，以确保相同的名字总是对应相同的头像
            String avatarUrl = "https://api.dicebear.com/7.x/adventurer/svg?seed=" + name;
            availableProfiles.add(new BotProfile(name, avatarUrl));
        }
    }

    public BotProfile getRandomBotProfile() {
        if (availableProfiles.isEmpty()) {
            // 兜底策略
            String randomName = "Bot-" + random.nextInt(1000);
            return new BotProfile(randomName, "https://api.dicebear.com/7.x/bottts/svg?seed=" + randomName);
        }
        return availableProfiles.get(random.nextInt(availableProfiles.size()));
    }

    public List<BotProfile> getRandomBotProfiles(int count) {
        List<BotProfile> copy = new ArrayList<>(availableProfiles);
        Collections.shuffle(copy, random);
        if (count > copy.size()) {
            // 如果需要的数量超过可用数量，则复用并修改，或者循环使用
            // 目前策略：如果不够，则生成通用的
            List<BotProfile> result = new ArrayList<>(copy);
            for (int i = 0; i < count - copy.size(); i++) {
                 String randomName = "Bot-" + (100 + i);
                 result.add(new BotProfile(randomName, "https://api.dicebear.com/7.x/bottts/svg?seed=" + randomName));
            }
            return result;
        }
        return copy.subList(0, count);
    }

    public static class BotProfile {
        private String name;
        private String avatarUrl;

        public BotProfile(String name, String avatarUrl) {
            this.name = name;
            this.avatarUrl = avatarUrl;
        }

        public String getName() {
            return name;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }
    }
}
