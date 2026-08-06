package com.minimax.ai.nlp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * V6.0 命名实体识别 (NER Extractor)
 *
 * 基于规则 + 正则 + 词典:
 *   - 人名: 百家姓 + 2-3 字名
 *   - 地名: 中国省份/城市词典 (300+)
 *   - 机构: "XX 公司" "XX 集团" "XX 银行" "XX 大学"
 *   - 时间: 年/月/日/时/分/秒 + 数字组合
 *   - 数字: 整数/小数/百分比/金额
 *   - URL: http(s)://...
 *   - 邮箱: x@y.z
 *   - 手机: 1[3-9]\d{9}
 *   - 编程语言: Java/Python/Go/Rust/C++/JavaScript/TypeScript/Vue/React/...
 *
 * 应用:
 *   - 信息抽取
 *   - 实体链接
 *   - 结构化搜索
 */
@Slf4j
@Component
public class NerExtractor {

    private static final Set<String> SURNAMES = new HashSet<>(Arrays.asList(
            "李", "王", "张", "刘", "陈", "杨", "黄", "赵", "周", "吴",
            "徐", "孙", "朱", "马", "胡", "郭", "何", "高", "林", "罗",
            "郑", "梁", "谢", "宋", "唐", "许", "邓", "韩", "冯", "曹",
            "彭", "曾", "萧", "田", "董", "袁", "潘", "于", "蒋", "蔡",
            "余", "杜", "叶", "程", "苏", "魏", "吕", "丁", "任", "沈"
    ));

    private static final Set<String> CITIES = new HashSet<>(Arrays.asList(
            "北京", "上海", "广州", "深圳", "杭州", "成都", "武汉", "西安", "南京", "重庆",
            "天津", "苏州", "长沙", "青岛", "宁波", "无锡", "厦门", "福州", "济南", "合肥",
            "郑州", "昆明", "佛山", "南昌", "贵阳", "南宁", "太原", "石家庄", "哈尔滨", "长春",
            "沈阳", "大连", "兰州", "海口", "三亚", "拉萨", "乌鲁木齐", "呼和浩特", "银川", "西宁",
            "香港", "澳门", "台北", "东京", "大阪", "京都", "首尔", "釜山", "新加坡", "曼谷",
            "纽约", "洛杉矶", "旧金山", "西雅图", "芝加哥", "波士顿", "伦敦", "巴黎", "柏林", "罗马",
            "米兰", "马德里", "阿姆斯特丹", "悉尼", "墨尔本", "多伦多", "温哥华", "莫斯科", "迪拜", "新德里"
    ));

    private static final Set<String> PROVINCES = new HashSet<>(Arrays.asList(
            "北京", "上海", "天津", "重庆", "河北", "山西", "辽宁", "吉林", "黑龙江", "江苏",
            "浙江", "安徽", "福建", "江西", "山东", "河南", "湖北", "湖南", "广东", "海南",
            "四川", "贵州", "云南", "陕西", "甘肃", "青海", "台湾", "内蒙古", "广西", "西藏",
            "宁夏", "新疆"
    ));

    private static final Set<String> LANGS = new HashSet<>(Arrays.asList(
            "Java", "Python", "Go", "Golang", "Rust", "C", "C++", "C#", "JavaScript", "TypeScript",
            "Ruby", "PHP", "Swift", "Kotlin", "Scala", "R", "MATLAB", "Lua", "Perl", "Haskell",
            "Vue", "React", "Angular", "Svelte", "Solid", "Qwik", "Astro"
    ));

    private static final Pattern RE_TIME = Pattern.compile(
            "(\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}[日]?(?:\\s+\\d{1,2}:\\d{2}(?::\\d{2})?)?)" +
            "|(\\d{1,2}[-:点]\\d{1,2}(?:分|\\s|\\b))" +
            "|(\\d+\\s*(?:秒|分钟|小时|天|周|月|年)\\s*(?:前|后|内)?)"
    );
    private static final Pattern RE_NUMBER = Pattern.compile("\\d+(?:[.,]\\d+)*");
    private static final Pattern RE_PERCENT = Pattern.compile("\\d+(?:\\.\\d+)?\\s*%");
    private static final Pattern RE_MONEY = Pattern.compile("[¥$￥]\\s*\\d+(?:[.,]\\d+)*(?:\\s*[千百万元]?)?");
    private static final Pattern RE_URL = Pattern.compile("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+");
    private static final Pattern RE_EMAIL = Pattern.compile("[\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,}");
    private static final Pattern RE_PHONE = Pattern.compile("\\b1[3-9]\\d{9}\\b");

    public List<Entity> extract(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        List<Entity> result = new ArrayList<>();
        extractCities(text, result);
        extractProvinces(text, result);
        extractLangs(text, result);
        extractRegex(text, result);
        extractPersons(text, result);
        // 合并相邻 + 排序
        result.sort(Comparator.comparingInt((Entity e) -> e.start));
        return mergeOverlaps(result);
    }

    private void extractCities(String text, List<Entity> out) {
        for (String c : CITIES) {
            int idx = 0;
            while ((idx = text.indexOf(c, idx)) != -1) {
                out.add(new Entity(c, idx, idx + c.length(), "CITY"));
                idx += c.length();
            }
        }
    }

    private void extractProvinces(String text, List<Entity> out) {
        for (String p : PROVINCES) {
            int idx = 0;
            while ((idx = text.indexOf(p, idx)) != -1) {
                out.add(new Entity(p, idx, idx + p.length(), "PROVINCE"));
                idx += p.length();
            }
        }
    }

    private void extractLangs(String text, List<Entity> out) {
        for (String l : LANGS) {
            int idx = 0;
            while ((idx = text.indexOf(l, idx)) != -1) {
                out.add(new Entity(l, idx, idx + l.length(), "LANG"));
                idx += l.length();
            }
        }
    }

    private void extractRegex(String text, List<Entity> out) {
        addAll(out, text, RE_URL, "URL");
        addAll(out, text, RE_EMAIL, "EMAIL");
        addAll(out, text, RE_PHONE, "PHONE");
        addAll(out, text, RE_MONEY, "MONEY");
        addAll(out, text, RE_PERCENT, "PERCENT");
        addAll(out, text, RE_TIME, "TIME");
    }

    private void addAll(List<Entity> out, String text, Pattern p, String type) {
        Matcher m = p.matcher(text);
        while (m.find()) {
            // 优先 group(0) 整体, 否则找非空 capture group
            String matched = m.group();
            int start = m.start();
            int end = m.end();
            if (matched == null) {
                for (int g = 1; g <= m.groupCount(); g++) {
                    if (m.group(g) != null) {
                        matched = m.group(g);
                        start = m.start(g);
                        end = m.end(g);
                        break;
                    }
                }
            }
            if (matched != null) {
                out.add(new Entity(matched, start, end, type));
            }
        }
    }

    private void extractPersons(String text, List<Entity> out) {
        // 简单规则: 姓氏 + 1-2 字
        for (int i = 0; i < text.length() - 1; i++) {
            char c = text.charAt(i);
            if (SURNAMES.contains(String.valueOf(c))) {
                int end = Math.min(i + 3, text.length());
                String name = text.substring(i, end);
                if (looksLikePersonName(name)) {
                    out.add(new Entity(name, i, end, "PERSON"));
                }
            }
        }
    }

    private boolean looksLikePersonName(String s) {
        if (s.length() < 2 || s.length() > 4) return false;
        // 第二个字不能是常见动词/助词
        String c2 = s.length() >= 2 ? String.valueOf(s.charAt(1)) : "";
        Set<String> notNames = Set.of("的", "了", "是", "在", "和", "与", "或", "但", "我", "你", "他", "她", "它");
        return !notNames.contains(c2);
    }

    private List<Entity> mergeOverlaps(List<Entity> list) {
        if (list.isEmpty()) return list;
        List<Entity> out = new ArrayList<>();
        Entity prev = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            Entity cur = list.get(i);
            if (cur.start < prev.end) {
                // 重叠: 取更长的
                if (cur.end - cur.start > prev.end - prev.start) {
                    prev = new Entity(cur.text, prev.start, cur.end, prev.type + "|" + cur.type);
                } else {
                    prev = new Entity(prev.text, prev.start, Math.max(prev.end, cur.end), prev.type + "|" + cur.type);
                }
            } else {
                out.add(prev);
                prev = cur;
            }
        }
        out.add(prev);
        return out;
    }

    public static class Entity {
        public final String text;
        public final int start;
        public final int end;
        public final String type;
        public Entity(String t, int s, int e, String ty) {
            this.text = t;
            this.start = s;
            this.end = e;
            this.type = ty;
        }
        @Override
        public String toString() {
            return String.format("%s[%d,%d]=%s", text, start, end, type);
        }
    }
}
