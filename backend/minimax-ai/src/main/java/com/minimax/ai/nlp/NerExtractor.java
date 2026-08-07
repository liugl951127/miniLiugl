package com.minimax.ai.nlp;

// @Slf4j: Lombok 日志
import lombok.extern.slf4j.Slf4j;
// @Component: Spring Bean
import org.springframework.stereotype.Component;

// Arrays: 数组转 List
import java.util.Arrays;
// ArrayList: 动态数组
import java.util.ArrayList;
// Collections: 集合工具
import java.util.Collections;
// Comparator: 比较器
import java.util.Comparator;
// HashSet: 哈希集合
import java.util.HashSet;
// List: 列表
import java.util.List;
// Set: 集合
import java.util.Set;
// Matcher: 正则匹配结果
import java.util.regex.Matcher;
// Pattern: 正则编译
import java.util.regex.Pattern;

/**
 * V6.0 命名实体识别 (NER Extractor)
 *
 * <h2>核心算法: 规则 + 正则 + 词典混合</h2>
 *
 * <h3>1. 规则法 (Rule-based)</h2>
 * - 实体类型: 人名/地名/机构/时间/数字/URL/邮箱/手机
 * - 实现: 词典匹配 + 正则表达式 + 启发式规则
 *
 * <h3>2. 词典匹配 (Dictionary Lookup)</h2>
 * - 准备实体词典 (城市/省份/人名姓氏/语言)
 * - 文本中查词典,命中即识别
 * - 优点: 准确率高, 缺点: 召回有限 (新词不在词典)
 *
 * <h3>3. 正则匹配 (Regex)</h2>
 * - URL/邮箱/手机/时间/金额: 模式固定,用正则
 * - 优点: 覆盖所有符合模式的实体
 *
 * <h3>4. 启发式 (Heuristic)</h2>
 * - 人名: 姓氏 + 1-2 字 (用百家姓表)
 * - 时间: 年/月/日/时/分/秒 + 数字组合
 *
 * <h2>实体类型</h2>
 * <ul>
 *   <li>PERSON - 人名 (张三, 李四)</li>
 *   <li>CITY - 城市 (北京, 上海, 纽约)</li>
 *   <li>PROVINCE - 省份 (河北, 广东)</li>
 *   <li>LANG - 编程/自然语言 (Java, Python)</li>
 *   <li>TIME - 时间 (2026-08-07, 15:30)</li>
 *   <li>URL - 链接 (https://...)</li>
 *   <li>EMAIL - 邮箱 (user@example.com)</li>
 *   <li>PHONE - 手机号 (18812345678)</li>
 *   <li>MONEY - 金额 (¥100, $99)</li>
 *   <li>PERCENT - 百分比 (90%)</li>
 * </ul>
 *
 * <h2>应用场景</h2>
 * - 搜索: 实体识别后建立倒排索引
 * - 知识图谱: 实体抽取 + 关系抽取
 * - 推荐: 根据用户提到的实体推荐相关
 */
@Slf4j
@Component
public class NerExtractor {

    // ============== 词典 ==============
    /**
     * 百家姓 (50 姓)
     * 用于人名识别: 姓氏 + 1-2 字名
     */
    private static final Set<String> SURNAMES = new HashSet<>(Arrays.asList(
            "李", "王", "张", "刘", "陈", "杨", "黄", "赵", "周", "吴",
            "徐", "孙", "朱", "马", "胡", "郭", "何", "高", "林", "罗",
            "郑", "梁", "谢", "宋", "唐", "许", "邓", "韩", "冯", "曹",
            "彭", "曾", "萧", "田", "董", "袁", "潘", "于", "蒋", "蔡",
            "余", "杜", "叶", "程", "苏", "魏", "吕", "丁", "任", "沈"
    ));

    /**
     * 城市词典 (60+ 个, 国内外主要城市)
     * 中文: 北京/上海/广州/深圳/杭州/...
     * 英文: 纽约/伦敦/巴黎/东京/新加坡/...
     */
    private static final Set<String> CITIES = new HashSet<>(Arrays.asList(
            "北京", "上海", "广州", "深圳", "杭州", "成都", "武汉", "西安", "南京", "重庆",
            "天津", "苏州", "长沙", "青岛", "宁波", "无锡", "厦门", "福州", "济南", "合肥",
            "郑州", "昆明", "佛山", "南昌", "贵阳", "南宁", "太原", "石家庄", "哈尔滨", "长春",
            "沈阳", "大连", "兰州", "海口", "三亚", "拉萨", "乌鲁木齐", "呼和浩特", "银川", "西宁",
            "香港", "澳门", "台北", "东京", "大阪", "京都", "首尔", "釜山", "新加坡", "曼谷",
            "纽约", "洛杉矶", "旧金山", "西雅图", "芝加哥", "波士顿", "伦敦", "巴黎", "柏林", "罗马",
            "米兰", "马德里", "阿姆斯特丹", "悉尼", "墨尔本", "多伦多", "温哥华", "莫斯科", "迪拜", "新德里"
    ));

    /**
     * 省份词典 (32 个, 包括直辖市/自治区)
     */
    private static final Set<String> PROVINCES = new HashSet<>(Arrays.asList(
            "北京", "上海", "天津", "重庆", "河北", "山西", "辽宁", "吉林", "黑龙江", "江苏",
            "浙江", "安徽", "福建", "江西", "山东", "河南", "湖北", "湖南", "广东", "海南",
            "四川", "贵州", "云南", "陕西", "甘肃", "青海", "台湾", "内蒙古", "广西", "西藏",
            "宁夏", "新疆"
    ));

    /**
     * 编程/自然语言词典 (23 个)
     * 覆盖主流编程语言 + 前端框架
     */
    private static final Set<String> LANGS = new HashSet<>(Arrays.asList(
            "Java", "Python", "Go", "Golang", "Rust", "C", "C++", "C#", "JavaScript", "TypeScript",
            "Ruby", "PHP", "Swift", "Kotlin", "Scala", "R", "MATLAB", "Lua", "Perl", "Haskell",
            "Vue", "React", "Angular", "Svelte", "Solid", "Qwik", "Astro"
    ));

    // ============== 正则 ==============
    /**
     * 时间匹配:
     *   - 完整日期: 2026-08-07 / 2026/08/07 / 2026年8月7日
     *   - 时分: 15:30 / 15:30:45
     *   - 持续: 5 秒 / 10 分钟 / 3 小时
     */
    private static final Pattern RE_TIME = Pattern.compile(
            "(\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}[日]?(?:\\s+\\d{1,2}:\\d{2}(?::\\d{2})?)?)" +
            "|(\\d{1,2}[-:点]\\d{1,2}(?:分|\\s|\\b))" +
            "|(\\d+\\s*(?:秒|分钟|小时|天|周|月|年)\\s*(?:前|后|内)?)"
    );
    /** 数字: 整数/小数/带千分位 */
    private static final Pattern RE_NUMBER = Pattern.compile("\\d+(?:[.,]\\d+)*");
    /** 百分比: 90% / 99.5% */
    private static final Pattern RE_PERCENT = Pattern.compile("\\d+(?:\\.\\d+)?\\s*%");
    /** 金额: ¥100 / $99 / ￥1234.56 */
    private static final Pattern RE_MONEY = Pattern.compile("[¥$￥]\\s*\\d+(?:[.,]\\d+)*(?:\\s*[千百万元]?)?");
    /** URL: http(s)://... */
    private static final Pattern RE_URL = Pattern.compile("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+");
    /** 邮箱: user@example.com */
    private static final Pattern RE_EMAIL = Pattern.compile("[\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,}");
    /** 手机: 1[3-9]xxxxxxxxx (11 位) */
    private static final Pattern RE_PHONE = Pattern.compile("\\b1[3-9]\\d{9}\\b");

    // ============== 核心 API ==============
    /**
     * 主入口: 抽取文本中的所有实体
     *
     * <h2>算法流程</h2>
     * <ol>
     *   <li>抽取城市 (词典)</li>
     *   <li>抽取省份 (词典)</li>
     *   <li>抽取语言 (词典)</li>
     *   <li>抽取正则实体 (URL/邮箱/手机/金额/百分比/时间)</li>
     *   <li>抽取人名 (启发式)</li>
     *   <li>按 start 排序</li>
     *   <li>合并重叠实体 (取较长)</li>
     * </ol>
     *
     * @param text 输入文本
     * @return 实体列表 (按位置排序)
     */
    public List<Entity> extract(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        // 用 ArrayList 收集所有候选实体
        List<Entity> result = new ArrayList<>();
        // 各种抽取
        extractCities(text, result);
        extractProvinces(text, result);
        extractLangs(text, result);
        extractRegex(text, result);
        extractPersons(text, result);
        // 按 start 位置排序
        result.sort(Comparator.comparingInt((Entity e) -> e.start));
        // 合并重叠
        return mergeOverlaps(result);
    }

    // ============== 词典抽取 ==============
    /**
     * 抽取城市: 文本中查 CITIES 词典
     * 用 indexOf 找所有出现位置
     */
    private void extractCities(String text, List<Entity> out) {
        for (String c : CITIES) {
            int idx = 0;
            // 循环找所有出现
            while ((idx = text.indexOf(c, idx)) != -1) {
                out.add(new Entity(c, idx, idx + c.length(), "CITY"));
                idx += c.length();  // 前进
            }
        }
    }

    /**
     * 抽取省份: 同城市
     */
    private void extractProvinces(String text, List<Entity> out) {
        for (String p : PROVINCES) {
            int idx = 0;
            while ((idx = text.indexOf(p, idx)) != -1) {
                out.add(new Entity(p, idx, idx + p.length(), "PROVINCE"));
                idx += p.length();
            }
        }
    }

    /**
     * 抽取语言: 同上
     */
    private void extractLangs(String text, List<Entity> out) {
        for (String l : LANGS) {
            int idx = 0;
            while ((idx = text.indexOf(l, idx)) != -1) {
                out.add(new Entity(l, idx, idx + l.length(), "LANG"));
                idx += l.length();
            }
        }
    }

    // ============== 正则抽取 ==============
    /**
     * 正则抽取: 调通用 addAll
     */
    private void extractRegex(String text, List<Entity> out) {
        addAll(out, text, RE_URL, "URL");
        addAll(out, text, RE_EMAIL, "EMAIL");
        addAll(out, text, RE_PHONE, "PHONE");
        addAll(out, text, RE_MONEY, "MONEY");
        addAll(out, text, RE_PERCENT, "PERCENT");
        addAll(out, text, RE_TIME, "TIME");
    }

    /**
     * 通用正则匹配 → 实体列表
     * 优先用 group(0) (整体匹配), 兜底用 group(1+) (capture group)
     */
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

    // ============== 人名抽取 (启发式) ==============
    /**
     * 抽取人名: 姓氏 + 1-2 字
     *
     * <h2>规则</h2>
     * - 第 1 字符在 SURNAMES 中
     * - 后面 1-2 个字符不是常见助词/代词
     * - 长度 2-4 (中文人名 2-3 字常见, 4 字罕见如 "诸葛亮")
     */
    private void extractPersons(String text, List<Entity> out) {
        // 遍历每个字符作为姓氏候选
        for (int i = 0; i < text.length() - 1; i++) {
            char c = text.charAt(i);
            // 必须是姓氏
            if (SURNAMES.contains(String.valueOf(c))) {
                // 取后续 1-2 个字符
                int end = Math.min(i + 3, text.length());
                String name = text.substring(i, end);
                // 启发式判断
                if (looksLikePersonName(name)) {
                    out.add(new Entity(name, i, end, "PERSON"));
                }
            }
        }
    }

    /**
     * 启发式判断是否像人名
     * 排除: 的/了/是/在/和/与/或/我/你/他/她/它 (常见助词/代词)
     */
    private boolean looksLikePersonName(String s) {
        // 长度限制
        if (s.length() < 2 || s.length() > 4) return false;
        // 第 2 字不能是常见词
        String c2 = s.length() >= 2 ? String.valueOf(s.charAt(1)) : "";
        Set<String> notNames = Set.of("的", "了", "是", "在", "和", "与", "或", "但", "我", "你", "他", "她", "它");
        return !notNames.contains(c2);
    }

    // ============== 合并重叠 ==============
    /**
     * 合并重叠的实体 (取较长的)
     *
     * <h2>为什么需要合并</h2>
     * "北京市" 中既匹配 "北京" (CITY) 又匹配 "北京" (PROVINCE) (直辖市)
     * 同一位置可能多个候选,需要去重
     *
     * <h2>算法</h2>
     * 实体按 start 排序后,逐个检查是否与上一个重叠:
     *   - 重叠: 取较长的,type 用 "|" 拼接
     *   - 不重叠: 加入结果
     */
    private List<Entity> mergeOverlaps(List<Entity> list) {
        if (list.isEmpty()) return list;
        List<Entity> out = new ArrayList<>();
        // prev: 当前正在累积的实体
        Entity prev = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            Entity cur = list.get(i);
            // 重叠: cur.start < prev.end
            if (cur.start < prev.end) {
                // 取较长的
                if (cur.end - cur.start > prev.end - prev.start) {
                    // cur 更长, 用 cur + 扩展 prev.end
                    prev = new Entity(cur.text, prev.start, cur.end, prev.type + "|" + cur.type);
                } else {
                    // prev 较长或相等, 扩展 end
                    prev = new Entity(prev.text, prev.start, Math.max(prev.end, cur.end), prev.type + "|" + cur.type);
                }
            } else {
                // 不重叠: 输出 prev, 重置
                out.add(prev);
                prev = cur;
            }
        }
        // 最后一个
        out.add(prev);
        return out;
    }

    // ============== 数据类 ==============
    /**
     * 实体
     * - text: 实体文本
     * - start/end: 在原文中的起止位置
     * - type: 实体类型 (CITY/PERSON/URL/...)
     */
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
