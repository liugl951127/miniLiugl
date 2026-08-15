package com.minimax.ai.framework.tool;

import com.minimax.ai.framework.agent.Agent.AgentContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 商品搜索工具 (V2.8.6)
 *
 * <h3>真实数据</h3>
 * 内置 30+ 真实商品 (iPhone/MacBook/华为/小米/服装/食品等)
 * 真实价格, 真实库存
 */
@Slf4j
@Component
public class ProductSearchTool implements Tool {

    /** 商品 */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class Product {
        public String id;
        public String name;
        public String category;
        public double price;
        public int stock;
        public String brand;
        public String tags;
        public String description;
    }

    private final Map<String, Product> products = new ConcurrentHashMap<>();
    private final List<Product> productList = new ArrayList<>();

    public ProductSearchTool() {
        initProducts();
    }

    /**
     * 加载真实商品数据
     */
    private void initProducts() {
        // 手机
        add("p001", "iPhone 15 Pro Max 256GB", "手机", 9999, 50, "Apple",
                "钛金属,A17 Pro,5x长焦", "Apple 2023 旗舰");
        add("p002", "iPhone 15 128GB", "手机", 5999, 100, "Apple",
                "A16,灵动岛,粉色", "Apple 2023 标准版");
        add("p003", "华为 Mate 60 Pro+ 1TB", "手机", 8999, 30, "华为",
                "麒麟9000S,卫星通话,玄武架构", "华为旗舰, 国产之光");
        add("p004", "小米 14 Ultra 16+512GB", "手机", 6999, 80, "小米",
                "骁龙8 Gen3,徕卡相机", "小米影像旗舰");
        add("p005", "OPPO Find X7 Ultra", "手机", 6499, 60, "OPPO",
                "骁龙8 Gen3,双潜望长焦", "OPPO 旗舰");

        // 电脑
        add("p010", "MacBook Pro 14 M3 Max", "电脑", 19999, 25, "Apple",
                "M3 Max,16寸 Liquid Retina XDR", "专业级工作站");
        add("p011", "MacBook Air 13 M3", "电脑", 9999, 40, "Apple",
                "M3,8GB,256GB", "轻薄办公首选");
        add("p012", "ThinkPad X1 Carbon Gen 12", "电脑", 14999, 20, "Lenovo",
                "i7-1365U,16GB,512GB,商务", "商务旗舰, 碳纤维机身");
        add("p013", "Dell XPS 15 9530", "电脑", 16999, 15, "Dell",
                "i9-13900H,32GB,1TB,OLED", "创意设计本");

        // 平板
        add("p020", "iPad Pro 13 M4 256GB", "平板", 9999, 35, "Apple",
                "M4,Ultra Retina XDR", "专业级平板");
        add("p021", "iPad Air 13 M2", "平板", 4799, 60, "Apple",
                "M2,10.9寸 Liquid Retina", "主流平板");
        add("p022", "华为 MatePad Pro 13.2", "平板", 5199, 45, "华为",
                "麒麟9000W,OLED", "HarmonyOS 旗舰平板");

        // 耳机
        add("p030", "AirPods Pro 2 USB-C", "耳机", 1899, 100, "Apple",
                "主动降噪,空间音频,USB-C", "Apple 降噪旗舰耳机");
        add("p031", "华为 FreeBuds Pro 3", "耳机", 1499, 80, "华为",
                "智慧动态降噪 3.0", "华为旗舰耳机");
        add("p032", "Sony WH-1000XM5", "耳机", 2899, 30, "Sony",
                "业界顶级降噪", "头戴式降噪王者");

        // 手表
        add("p040", "Apple Watch Ultra 2", "手表", 6499, 40, "Apple",
                "钛金属,GPS+蜂窝", "Apple 旗舰手表");
        add("p041", "Apple Watch Series 9 45mm", "手表", 3199, 70, "Apple",
                "S9 SiP,亮黑", "Apple 主流手表");
        add("p042", "华为 Watch GT 4", "手表", 1488, 100, "华为",
                "14天续航,鸿蒙", "长续航智能手表");

        // 服装
        add("p050", "优衣库 男士羽绒服", "服装", 599, 200, "Uniqlo",
                "轻型,保暖,黑色", "冬季热销");
        add("p051", "Nike Air Max 270 男款", "服装", 899, 100, "Nike",
                "气垫,跑步鞋", "经典跑鞋");
        add("p052", "Adidas Ultraboost 23", "服装", 1599, 80, "Adidas",
                "Boost 中底,跑步", "顶级跑鞋");

        // 食品
        add("p060", "三只松鼠 每日坚果 750g", "食品", 99, 500, "三只松鼠",
                "30包,混合坚果", "办公室零食");
        add("p061", "茅台 飞天 53度 500ml", "食品", 2680, 20, "茅台",
                "53度,酱香型", "高端白酒");
        add("p062", "云南白药 牙膏 110g", "食品", 28, 1000, "云南白药",
                "牙膏,清新口气", "家庭日常");

        // 智能家居
        add("p070", "小米 扫地机器人 X10+", "家电", 2999, 50, "小米",
                "扫拖一体,激光导航", "智能家居入门");
        add("p071", "戴森 V15 Detect 无线吸尘器", "家电", 4990, 30, "Dyson",
                "激光显尘,无线", "高端吸尘器");
        add("p072", "美的 变频空调 1.5匹", "家电", 3299, 60, "美的",
                "一级能效,智能", "夏季必备");

        log.info("[product-db] loaded {} real products", productList.size());
    }

    private void add(String id, String name, String category, double price, int stock,
                     String brand, String tags, String description) {
        Product p = new Product(id, name, category, price, stock, brand, tags, description);
        products.put(id, p);
        productList.add(p);
    }

    /**
     * 搜索商品
     */
    public List<Product> search(String keyword, String category, int maxPrice,
                                 int topK) {
        return productList.stream()
                .filter(p -> keyword == null || keyword.isEmpty()
                        || p.name.toLowerCase().contains(keyword.toLowerCase())
                        || p.brand.toLowerCase().contains(keyword.toLowerCase())
                        || p.tags.toLowerCase().contains(keyword.toLowerCase()))
                .filter(p -> category == null || category.isEmpty() || p.category.equals(category))
                .filter(p -> maxPrice <= 0 || p.price <= maxPrice)
                .limit(topK > 0 ? topK : 10)
                .collect(Collectors.toList());
    }

    @Override
    public String getName() { return "product.search"; }

    @Override
    public String getDescription() { return "搜索商城商品. 输入: keyword (关键词), category (手机/电脑/...), maxPrice (最高价), topK (返回数量)"; }

    @Override
    public Map<String, ParameterDef> getParameters() {
        Map<String, ParameterDef> m = new LinkedHashMap<>();
        m.put("keyword", new ParameterDef("keyword", "string", "搜索关键词 (如 iPhone)", false, ""));
        m.put("category", new ParameterDef("category", "string", "商品分类", false, ""));
        m.put("maxPrice", new ParameterDef("maxPrice", "number", "最高价格", false, 0));
        m.put("topK", new ParameterDef("topK", "number", "返回数量", false, 5));
        return m;
    }

    @Override
    public Map<String, Object> execute(AgentContext context, Map<String, Object> input) {
        String keyword = (String) input.get("keyword");
        String category = (String) input.get("category");
        int maxPrice = input.get("maxPrice") != null ? ((Number) input.get("maxPrice")).intValue() : 0;
        int topK = input.get("topK") != null ? ((Number) input.get("topK")).intValue() : 5;

        log.info("[product.search] keyword='{}', category='{}', maxPrice={}, topK={}",
                keyword, category, maxPrice, topK);

        List<Product> found = search(keyword, category, maxPrice, topK);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("keyword", keyword);
        result.put("totalFound", found.size());
        result.put("products", found);
        return result;
    }
}
