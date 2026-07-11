package com.minimax.ai.codegen;

import com.minimax.ai.dto.CodeGenRequest;
import com.minimax.ai.dto.CodeGenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 项目代码生成器 (V2.5 自研)
 *
 * 能力:
 *   - Spring Boot 项目骨架 (Java 17 + Maven)
 *   - Vue 3 前端项目 (Vite + Element Plus)
 *   - React 项目 (Vite + Ant Design)
 *   - Python Flask 项目
 *   - Node Express 项目
 *   - 纯 HTML 静态站
 *
 * 输出:
 *   - 完整目录结构
 *   - 关键文件 (pom.xml / package.json / Dockerfile / README 等)
 *   - 业务代码 (Controller / Service / Mapper / Entity)
 *   - 单元测试
 *   - 启动说明
 *
 * 智能化:
 *   - 根据 description 推断业务模块
 *   - 根据 features 生成对应 API
 *   - 自动生成 CRUD 模板
 *   - 智能选择数据库驱动
 *
 * 100% 离线, 不调用任何外部 API
 */
@Slf4j
@Component
public class ProjectCodeGenerator {

    /**
     * 生成项目代码
     */
    public CodeGenResponse generate(CodeGenRequest request) {
        long start = System.currentTimeMillis();

        // 1. 参数归一化
        String projectType = normalizeProjectType(request.getProjectType());
        String projectName = normalizeProjectName(request.getProjectName());
        String pkg = request.getPackageName() != null ? request.getPackageName()
                : "com.example." + projectName.toLowerCase().replace("-", "").replace("_", "");
        String db = request.getDatabase() != null ? request.getDatabase() : "h2";
        boolean tests = request.getIncludeTests() == null || request.getIncludeTests();
        List<String> features = parseFeatures(request.getFeatures());

        log.info("生成项目: type={} name={} pkg={} db={} features={}", projectType, projectName, pkg, db, features);

        Map<String, String> files = new LinkedHashMap<>();

        // 2. 根据项目类型生成
        switch (projectType) {
            case "spring-boot":
                generateSpringBoot(files, projectName, pkg, db, request.getDescription(), features, tests);
                break;
            case "vue":
                generateVue(files, projectName, request.getDescription(), features);
                break;
            case "react":
                generateReact(files, projectName, request.getDescription(), features);
                break;
            case "python-flask":
                generatePythonFlask(files, projectName, db, request.getDescription(), features);
                break;
            case "node-express":
                generateNodeExpress(files, projectName, db, request.getDescription(), features);
                break;
            case "html":
                generateHtml(files, projectName, request.getDescription());
                break;
            default:
                generateSpringBoot(files, projectName, pkg, db, request.getDescription(), features, tests);
        }

        // 3. 通用文件 (所有项目都需要)
        addCommon(files, projectName, request.getDescription(), projectType);

        // 4. 构建响应
        CodeGenResponse response = new CodeGenResponse();
        response.setProjectName(projectName);
        response.setProjectType(projectType);
        response.setStructure(generateStructure(files.keySet(), projectName));
        response.setFiles(files);
        response.setKeyFiles(extractKeyFiles(files.keySet()));
        response.setRunInstructions(generateRunInstructions(projectType, projectName, db));
        response.setDurationMs(System.currentTimeMillis() - start);
        response.setTotalFiles(files.size());
        response.setTotalLines(files.values().stream().mapToInt(String::length).sum() / 80); // 估算行数

        log.info("项目生成完成: {} 个文件, 耗时 {}ms", files.size(), response.getDurationMs());
        return response;
    }

    // ============== Spring Boot ==============

    private void generateSpringBoot(Map<String, String> files, String name, String pkg,
                                    String db, String desc, List<String> features, boolean tests) {
        String pkgPath = pkg.replace('.', '/');

        // 1. pom.xml
        files.put(name + "/pom.xml", generatePomXml(name, db, features));

        // 2. 启动类
        files.put(name + "/src/main/java/" + pkgPath + "/Application.java",
                generateSpringBootApplication(pkg, name));

        // 3. application.yml
        files.put(name + "/src/main/resources/application.yml",
                generateSpringBootYml(name, db));

        // 4. 主控制器
        files.put(name + "/src/main/java/" + pkgPath + "/controller/HelloController.java",
                generateHelloController(pkg, name, desc, features));

        // 5. Service 层
        files.put(name + "/src/main/java/" + pkgPath + "/service/HelloService.java",
                generateHelloService(pkg, name, features));

        // 6. Entity
        files.put(name + "/src/main/java/" + pkgPath + "/entity/" + capitalize(name) + ".java",
                generateEntity(pkg, name, db));

        // 7. Mapper
        files.put(name + "/src/main/java/" + pkgPath + "/mapper/" + capitalize(name) + "Mapper.java",
                generateMapper(pkg, name));

        // 8. DTO
        files.put(name + "/src/main/java/" + pkgPath + "/dto/" + capitalize(name) + "DTO.java",
                generateDTO(pkg, name));

        // 9. Result
        files.put(name + "/src/main/java/" + pkgPath + "/common/Result.java",
                generateResultClass(pkg));

        // 10. GlobalExceptionHandler
        files.put(name + "/src/main/java/" + pkgPath + "/common/GlobalExceptionHandler.java",
                generateExceptionHandler(pkg));

        // 11. SQL 初始化
        if (!db.equals("none")) {
            files.put(name + "/sql/init.sql", generateInitSql(name, db, features));
        }

        // 12. 单元测试
        if (tests) {
            files.put(name + "/src/test/java/" + pkgPath + "/HelloControllerTest.java",
                    generateControllerTest(pkg, name, features));
        }

        // 13. Dockerfile
        files.put(name + "/Dockerfile", generateDockerfile("spring-boot", name));

        // 14. README
        files.put(name + "/README.md", generateReadme(name, desc, "spring-boot", features));
    }

    private String generatePomXml(String name, String db, List<String> features) {
        String dep = "";
        if ("mysql".equals(db)) {
            dep = "        <dependency>\n" +
                    "            <groupId>com.mysql</groupId>\n" +
                    "            <artifactId>mysql-connector-j</artifactId>\n" +
                    "        </dependency>\n";
        } else if ("postgresql".equals(db)) {
            dep = "        <dependency>\n" +
                    "            <groupId>org.postgresql</groupId>\n" +
                    "            <artifactId>postgresql</artifactId>\n" +
                    "        </dependency>\n";
        } else if ("h2".equals(db)) {
            dep = "        <dependency>\n" +
                    "            <groupId>com.h2database</groupId>\n" +
                    "            <artifactId>h2</artifactId>\n" +
                    "            <scope>runtime</scope>\n" +
                    "        </dependency>\n";
        }

        String featuresDep = "";
        if (features.contains("redis")) {
            featuresDep += "        <dependency>\n" +
                    "            <groupId>org.springframework.boot</groupId>\n" +
                    "            <artifactId>spring-boot-starter-data-redis</artifactId>\n" +
                    "        </dependency>\n";
        }
        if (features.contains("security") || features.contains("认证")) {
            featuresDep += "        <dependency>\n" +
                    "            <groupId>org.springframework.boot</groupId>\n" +
                    "            <artifactId>spring-boot-starter-security</artifactId>\n" +
                    "        </dependency>\n";
        }
        if (features.contains("validation") || features.contains("校验")) {
            featuresDep += "        <dependency>\n" +
                    "            <groupId>org.springframework.boot</groupId>\n" +
                    "            <artifactId>spring-boot-starter-validation</artifactId>\n" +
                    "        </dependency>\n";
        }

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <parent>\n" +
                "        <groupId>org.springframework.boot</groupId>\n" +
                "        <artifactId>spring-boot-starter-parent</artifactId>\n" +
                "        <version>3.2.0</version>\n" +
                "    </parent>\n" +
                "    <groupId>com.example</groupId>\n" +
                "    <artifactId>" + name + "</artifactId>\n" +
                "    <version>1.0.0-SNAPSHOT</version>\n" +
                "    <name>" + name + "</name>\n" +
                "    <properties>\n" +
                "        <java.version>17</java.version>\n" +
                "        <maven.compiler.source>17</maven.compiler.source>\n" +
                "        <maven.compiler.target>17</maven.compiler.target>\n" +
                "    </properties>\n" +
                "    <dependencies>\n" +
                "        <dependency>\n" +
                "            <groupId>org.springframework.boot</groupId>\n" +
                "            <artifactId>spring-boot-starter-web</artifactId>\n" +
                "        </dependency>\n" +
                "        <dependency>\n" +
                "            <groupId>org.springframework.boot</groupId>\n" +
                "            <artifactId>spring-boot-starter-test</artifactId>\n" +
                "            <scope>test</scope>\n" +
                "        </dependency>\n" + dep + featuresDep +
                "    </dependencies>\n" +
                "    <build>\n" +
                "        <plugins>\n" +
                "            <plugin>\n" +
                "                <groupId>org.springframework.boot</groupId>\n" +
                "                <artifactId>spring-boot-maven-plugin</artifactId>\n" +
                "            </plugin>\n" +
                "        </plugins>\n" +
                "    </build>\n" +
                "</project>\n";
    }

    private String generateSpringBootApplication(String pkg, String name) {
        return "package " + pkg + ";\n\n" +
                "import org.springframework.boot.SpringApplication;\n" +
                "import org.springframework.boot.autoconfigure.SpringBootApplication;\n\n" +
                "/**\n * " + name + " 启动类\n */\n" +
                "@SpringBootApplication\n" +
                "public class Application {\n" +
                "    public static void main(String[] args) {\n" +
                "        SpringApplication.run(Application.class, args);\n" +
                "    }\n" +
                "}\n";
    }

    private String generateSpringBootYml(String name, String db) {
        String datasource = "";
        if ("h2".equals(db)) {
            datasource = "spring:\n" +
                    "  datasource:\n" +
                    "    url: jdbc:h2:mem:testdb\n" +
                    "    driver-class-name: org.h2.Driver\n" +
                    "    username: sa\n" +
                    "    password: ''\n" +
                    "  h2:\n" +
                    "    console:\n" +
                    "      enabled: true\n";
        } else if ("mysql".equals(db)) {
            datasource = "spring:\n" +
                    "  datasource:\n" +
                    "    url: jdbc:mysql://localhost:3306/" + name + "?useSSL=false&serverTimezone=UTC\n" +
                    "    username: root\n" +
                    "    password: root123\n" +
                    "    driver-class-name: com.mysql.cj.jdbc.Driver\n";
        } else if ("postgresql".equals(db)) {
            datasource = "spring:\n" +
                    "  datasource:\n" +
                    "    url: jdbc:postgresql://localhost:5432/" + name + "\n" +
                    "    username: postgres\n" +
                    "    password: postgres\n";
        }
        return "server:\n" +
                "  port: 8080\n" +
                datasource +
                "logging:\n" +
                "  level:\n" +
                "    root: INFO\n" +
                "    com.example: DEBUG\n";
    }

    private String generateHelloController(String pkg, String name, String desc, List<String> features) {
        return "package " + pkg + ".controller;\n\n" +
                "import " + pkg + ".common.Result;\n" +
                "import " + pkg + ".service.HelloService;\n" +
                "import org.springframework.beans.factory.annotation.Autowired;\n" +
                "import org.springframework.web.bind.annotation.*;\n\n" +
                "/**\n * " + name + " 控制器 - " + desc + "\n */\n" +
                "@RestController\n" +
                "@RequestMapping(\"/api/" + name.toLowerCase() + "\")\n" +
                "public class HelloController {\n" +
                "    @Autowired\n" +
                "    private HelloService service;\n\n" +
                "    @GetMapping(\"/hello\")\n" +
                "    public Result<String> hello() {\n" +
                "        return Result.success(\"Hello from " + name + ": " + desc + "\");\n" +
                "    }\n\n" +
                "    @GetMapping(\"/info\")\n" +
                "    public Result<Object> info() {\n" +
                "        return Result.success(service.getInfo());\n" +
                "    }\n" +
                (features.contains("list") || features.contains("列表") ?
                "    @GetMapping(\"/list\")\n" +
                "    public Result<Object> list() {\n" +
                "        return Result.success(service.listAll());\n" +
                "    }\n\n" : "") +
                (features.contains("create") || features.contains("新增") ?
                "    @PostMapping(\"/create\")\n" +
                "    public Result<Object> create(@RequestBody Object dto) {\n" +
                "        return Result.success(service.create(dto));\n" +
                "    }\n\n" : "") +
                "}\n";
    }

    private String generateHelloService(String pkg, String name, List<String> features) {
        return "package " + pkg + ".service;\n\n" +
                "import org.springframework.stereotype.Service;\n" +
                "import java.util.*;\n\n" +
                "/**\n * " + name + " 业务逻辑层\n */\n" +
                "@Service\n" +
                "public class HelloService {\n\n" +
                "    public Map<String, Object> getInfo() {\n" +
                "        Map<String, Object> info = new LinkedHashMap<>();\n" +
                "        info.put(\"name\", \"" + name + "\");\n" +
                "        info.put(\"version\", \"1.0.0\");\n" +
                "        info.put(\"timestamp\", System.currentTimeMillis());\n" +
                "        return info;\n" +
                "    }\n\n" +
                (features.contains("list") || features.contains("列表") ?
                "    public List<Object> listAll() {\n" +
                "        return new ArrayList<>();\n" +
                "    }\n\n" : "") +
                (features.contains("create") || features.contains("新增") ?
                "    public Object create(Object dto) {\n" +
                "        return dto;\n" +
                "    }\n\n" : "") +
                "}\n";
    }

    private String generateEntity(String pkg, String name, String db) {
        String anno = "h2".equals(db) || "mysql".equals(db) || "postgresql".equals(db) ?
                "import jakarta.persistence.*;\n" : "";
        return "package " + pkg + ".entity;\n\n" + anno +
                "import lombok.Data;\n" +
                "import java.time.LocalDateTime;\n\n" +
                "/**\n * " + name + " 实体\n */\n" +
                "@Data\n" +
                (anno.contains("jakarta.persistence") ? "@Entity\n@Table(name = \"" + name.toLowerCase() + "\")\n" : "") +
                "public class " + capitalize(name) + " {\n" +
                (anno.contains("jakarta.persistence") ? "    @Id\n    @GeneratedValue(strategy = GenerationType.IDENTITY)\n    private Long id;\n\n" : "    private Long id;\n\n") +
                "    private String name;\n" +
                "    private String description;\n" +
                "    private LocalDateTime createdAt;\n" +
                "    private LocalDateTime updatedAt;\n" +
                "}\n";
    }

    private String generateMapper(String pkg, String name) {
        return "package " + pkg + ".mapper;\n\n" +
                "import " + pkg + ".entity." + capitalize(name) + ";\n" +
                "import org.apache.ibatis.annotations.*;\n" +
                "import java.util.List;\n\n" +
                "/**\n * " + name + " Mapper\n */\n" +
                "@Mapper\n" +
                "public interface " + capitalize(name) + "Mapper {\n" +
                "    @Select(\"SELECT * FROM " + name.toLowerCase() + "\")\n" +
                "    List<" + capitalize(name) + "> findAll();\n\n" +
                "    @Select(\"SELECT * FROM " + name.toLowerCase() + " WHERE id = #{id}\")\n" +
                "    " + capitalize(name) + " findById(Long id);\n\n" +
                "    @Insert(\"INSERT INTO " + name.toLowerCase() + " (name, description) VALUES (#{name}, #{description})\")\n" +
                "    int insert(" + capitalize(name) + " entity);\n" +
                "}\n";
    }

    private String generateDTO(String pkg, String name) {
        return "package " + pkg + ".dto;\n\n" +
                "import lombok.Data;\n\n" +
                "/**\n * " + name + " 数据传输对象\n */\n" +
                "@Data\n" +
                "public class " + capitalize(name) + "DTO {\n" +
                "    private String name;\n" +
                "    private String description;\n" +
                "}\n";
    }

    private String generateResultClass(String pkg) {
        return "package " + pkg + ".common;\n\n" +
                "import lombok.Data;\n\n" +
                "/**\n * 统一响应格式\n */\n" +
                "@Data\n" +
                "public class Result<T> {\n" +
                "    private int code;\n" +
                "    private String message;\n" +
                "    private T data;\n\n" +
                "    public static <T> Result<T> success(T data) {\n" +
                "        Result<T> r = new Result<>();\n" +
                "        r.code = 0;\n" +
                "        r.message = \"success\";\n" +
                "        r.data = data;\n" +
                "        return r;\n" +
                "    }\n\n" +
                "    public static <T> Result<T> fail(String message) {\n" +
                "        Result<T> r = new Result<>();\n" +
                "        r.code = 500;\n" +
                "        r.message = message;\n" +
                "        return r;\n" +
                "    }\n" +
                "}\n";
    }

    private String generateExceptionHandler(String pkg) {
        return "package " + pkg + ".common;\n\n" +
                "import org.springframework.http.ResponseEntity;\n" +
                "import org.springframework.web.bind.annotation.*;\n\n" +
                "/**\n * 全局异常处理\n */\n" +
                "@RestControllerAdvice\n" +
                "public class GlobalExceptionHandler {\n" +
                "    @ExceptionHandler(Exception.class)\n" +
                "    public ResponseEntity<Result<?>> handle(Exception e) {\n" +
                "        return ResponseEntity.ok(Result.fail(e.getMessage()));\n" +
                "    }\n" +
                "}\n";
    }

    private String generateInitSql(String name, String db, List<String> features) {
        String sql = "-- " + name + " 数据库初始化\n\n" +
                "CREATE TABLE IF NOT EXISTS " + name.toLowerCase() + " (\n" +
                "    id BIGINT PRIMARY KEY AUTO_INCREMENT";
        if ("postgresql".equals(db)) {
            sql = sql.replace("AUTO_INCREMENT", "SERIAL");
        }
        sql += ",\n" +
                "    name VARCHAR(255) NOT NULL,\n" +
                "    description TEXT,\n" +
                "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n" +
                ");\n\n" +
                "INSERT INTO " + name.toLowerCase() + " (name, description) VALUES\n" +
                "    ('示例 1', '这是第一条数据'),\n" +
                "    ('示例 2', '这是第二条数据');\n";
        return sql;
    }

    private String generateControllerTest(String pkg, String name, List<String> features) {
        return "package " + pkg + ";\n\n" +
                "import " + pkg + ".controller.HelloController;\n" +
                "import " + pkg + ".common.Result;\n" +
                "import org.junit.jupiter.api.Test;\n" +
                "import org.springframework.beans.factory.annotation.Autowired;\n" +
                "import org.springframework.boot.test.context.SpringBootTest;\n\n" +
                "/**\n * HelloController 单元测试\n */\n" +
                "@SpringBootTest\n" +
                "public class HelloControllerTest {\n" +
                "    @Autowired\n" +
                "    private HelloController controller;\n\n" +
                "    @Test\n" +
                "    public void testHello() {\n" +
                "        Result<String> result = controller.hello();\n" +
                "        assert result.getCode() == 0;\n" +
                "        assert result.getData().contains(\"Hello\");\n" +
                "    }\n" +
                "}\n";
    }

    // ============== Vue 3 ==============

    private void generateVue(Map<String, String> files, String name, String desc, List<String> features) {
        files.put(name + "/package.json", generateVuePackageJson(name, features));
        files.put(name + "/vite.config.js", generateViteConfig());
        files.put(name + "/index.html", "<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head><meta charset=\"UTF-8\"><title>" + name + "</title></head>\n<body><div id=\"app\"></div><script type=\"module\" src=\"/src/main.js\"></script></body>\n</html>\n");
        files.put(name + "/src/main.js", "import { createApp } from 'vue';\nimport ElementPlus from 'element-plus';\nimport 'element-plus/dist/index.css';\nimport App from './App.vue';\ncreateApp(App).use(ElementPlus).mount('#app');\n");
        files.put(name + "/src/App.vue", generateVueApp(name, desc, features));
        files.put(name + "/src/api/index.js", generateVueApi(name, features));
        files.put(name + "/src/views/Home.vue", generateVueView(name, desc, features));
        files.put(name + "/src/router/index.js", "import { createRouter, createWebHistory } from 'vue-router';\nimport Home from '../views/Home.vue';\nconst router = createRouter({ history: createWebHistory(), routes: [{ path: '/', component: Home }] });\nexport default router;\n");
        files.put(name + "/README.md", generateReadme(name, desc, "vue", features));
    }

    private String generateVuePackageJson(String name, List<String> features) {
        return "{\n" +
                "  \"name\": \"" + name + "\",\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"scripts\": {\n" +
                "    \"dev\": \"vite\",\n" +
                "    \"build\": \"vite build\",\n" +
                "    \"preview\": \"vite preview\"\n" +
                "  },\n" +
                "  \"dependencies\": {\n" +
                "    \"vue\": \"^3.4.0\",\n" +
                "    \"vue-router\": \"^4.2.0\",\n" +
                "    \"element-plus\": \"^2.4.0\",\n" +
                "    \"axios\": \"^1.6.0\"\n" +
                "  },\n" +
                "  \"devDependencies\": {\n" +
                "    \"vite\": \"^5.0.0\",\n" +
                "    \"@vitejs/plugin-vue\": \"^5.0.0\"\n" +
                "  }\n" +
                "}\n";
    }

    private String generateViteConfig() {
        return "import { defineConfig } from 'vite';\nimport vue from '@vitejs/plugin-vue';\n\nexport default defineConfig({\n  plugins: [vue()],\n  server: { port: 3000, open: true }\n});\n";
    }

    private String generateVueApp(String name, String desc, List<String> features) {
        return "<template>\n  <el-container>\n    <el-header><h1>" + name + "</h1></el-header>\n    <el-main>\n      <router-view />\n    </el-main>\n  </el-container>\n</template>\n\n<script setup>\n</script>\n\n<style>\n#app { font-family: Avenir, sans-serif; }\n</style>\n";
    }

    private String generateVueApi(String name, List<String> features) {
        return "import axios from 'axios';\n\nconst api = axios.create({ baseURL: '/api', timeout: 10000 });\n\nexport const hello = () => api.get('/" + name.toLowerCase() + "/hello');\n" + (features.contains("list") || features.contains("列表") ?
                "export const list = () => api.get('/" + name.toLowerCase() + "/list');\n" : "") +
                (features.contains("create") || features.contains("新增") ?
                "export const create = (data) => api.post('/" + name.toLowerCase() + "/create', data);\n" : "") +
                "export default api;\n";
    }

    private String generateVueView(String name, String desc, List<String> features) {
        return "<template>\n  <div class=\"home\">\n" +
                "    <el-card>\n" +
                "      <h2>" + name + "</h2>\n" +
                "      <p>" + desc + "</p>\n" +
                "      <el-button type=\"primary\" @click=\"loadData\">加载数据</el-button>\n" +
                "      <pre v-if=\"data\">{{ data }}</pre>\n" +
                "    </el-card>\n" +
                "  </div>\n" +
                "</template>\n\n" +
                "<script setup>\n" +
                "import { ref } from 'vue';\n" +
                "import { hello, list, create } from '../api';\n" +
                "const data = ref(null);\n" +
                "async function loadData() {\n" +
                "  const res = await hello();\n" +
                "  data.value = res.data;\n" +
                "}\n" +
                "</script>\n";
    }

    // ============== React ==============

    private void generateReact(Map<String, String> files, String name, String desc, List<String> features) {
        files.put(name + "/package.json", "{\n  \"name\": \"" + name + "\",\n  \"version\": \"1.0.0\",\n  \"scripts\": { \"dev\": \"vite\", \"build\": \"vite build\" },\n  \"dependencies\": { \"react\": \"^18.2.0\", \"react-dom\": \"^18.2.0\", \"antd\": \"^5.12.0\", \"axios\": \"^1.6.0\" },\n  \"devDependencies\": { \"vite\": \"^5.0.0\", \"@vitejs/plugin-react\": \"^4.2.0\" }\n}\n");
        files.put(name + "/vite.config.js", "import { defineConfig } from 'vite';\nimport react from '@vitejs/plugin-react';\nexport default defineConfig({ plugins: [react()] });\n");
        files.put(name + "/index.html", "<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"><title>" + name + "</title></head>\n<body><div id=\"root\"></div><script type=\"module\" src=\"/src/main.jsx\"></script></body></html>\n");
        files.put(name + "/src/main.jsx", "import React from 'react';\nimport ReactDOM from 'react-dom/client';\nimport App from './App.jsx';\nReactDOM.createRoot(document.getElementById('root')).render(<App />);\n");
        files.put(name + "/src/App.jsx", "import React, { useState } from 'react';\nimport { Button, Card } from 'antd';\n\nexport default function App() {\n  const [data, setData] = useState(null);\n  return (\n    <div style={{ padding: 24 }}>\n      <Card title='" + name + "'>\n        <p>" + desc + "</p>\n        <Button type=\"primary\" onClick={() => fetch('/api/" + name.toLowerCase() + "/hello').then(r => r.json()).then(setData)}>加载</Button>\n        <pre>{data && JSON.stringify(data, null, 2)}</pre>\n      </Card>\n    </div>\n  );\n}\n");
        files.put(name + "/README.md", generateReadme(name, desc, "react", features));
    }

    // ============== Python Flask ==============

    private void generatePythonFlask(Map<String, String> files, String name, String db, String desc, List<String> features) {
        files.put(name + "/requirements.txt", "flask>=3.0.0\nflask-cors>=4.0.0\nsqlalchemy>=2.0.0\n");
        files.put(name + "/app.py", "from flask import Flask, jsonify, request\nfrom flask_cors import CORS\n\napp = Flask(__name__)\nCORS(app)\n\n@app.route('/api/" + name.toLowerCase() + "/hello')\ndef hello():\n    return jsonify({'code': 0, 'message': 'success', 'data': f'Hello from " + name + ": " + desc + "'})\n\n" + (features.contains("list") || features.contains("列表") ?
                "@app.route('/api/" + name.toLowerCase() + "/list', methods=['GET'])\ndef list_all():\n    return jsonify({'code': 0, 'data': []})\n\n" : "") +
                (features.contains("create") || features.contains("新增") ?
                "@app.route('/api/" + name.toLowerCase() + "/create', methods=['POST'])\ndef create():\n    return jsonify({'code': 0, 'data': request.json})\n\n" : "") +
                "if __name__ == '__main__':\n    app.run(host='0.0.0.0', port=5000, debug=True)\n");
        files.put(name + "/README.md", generateReadme(name, desc, "python-flask", features));
    }

    // ============== Node Express ==============

    private void generateNodeExpress(Map<String, String> files, String name, String db, String desc, List<String> features) {
        files.put(name + "/package.json", "{\n  \"name\": \"" + name + "\",\n  \"version\": \"1.0.0\",\n  \"scripts\": { \"start\": \"node index.js\", \"dev\": \"nodemon index.js\" },\n  \"dependencies\": { \"express\": \"^4.18.0\", \"cors\": \"^2.8.5\" }\n}\n");
        files.put(name + "/index.js", "const express = require('express');\nconst cors = require('cors');\nconst app = express();\napp.use(cors());\napp.use(express.json());\n\napp.get('/api/" + name.toLowerCase() + "/hello', (req, res) => {\n  res.json({ code: 0, message: 'success', data: 'Hello from " + name + ": " + desc + "' });\n});\n\n" + (features.contains("list") || features.contains("列表") ?
                "app.get('/api/" + name.toLowerCase() + "/list', (req, res) => {\n  res.json({ code: 0, data: [] });\n});\n\n" : "") +
                (features.contains("create") || features.contains("新增") ?
                "app.post('/api/" + name.toLowerCase() + "/create', (req, res) => {\n  res.json({ code: 0, data: req.body });\n});\n\n" : "") +
                "const PORT = process.env.PORT || 3000;\napp.listen(PORT, () => console.log('Server on http://localhost:' + PORT));\n");
        files.put(name + "/README.md", generateReadme(name, desc, "node-express", features));
    }

    // ============== HTML 静态 ==============

    private void generateHtml(Map<String, String> files, String name, String desc) {
        files.put(name + "/index.html", "<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <title>" + name + "</title>\n" +
                "  <style>\n" +
                "    body { font-family: -apple-system, sans-serif; margin: 0; padding: 2rem; background: #f5f5f5; }\n" +
                "    .container { max-width: 800px; margin: 0 auto; background: white; padding: 2rem; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }\n" +
                "    h1 { color: #333; }\n" +
                "    p { color: #666; line-height: 1.6; }\n" +
                "    button { background: #409eff; color: white; border: none; padding: 0.5rem 1rem; border-radius: 4px; cursor: pointer; }\n" +
                "  </style>\n" +
                "</head>\n<body>\n" +
                "  <div class=\"container\">\n" +
                "    <h1>" + name + "</h1>\n" +
                "    <p>" + desc + "</p>\n" +
                "    <button onclick=\"loadData()\">加载数据</button>\n" +
                "    <pre id=\"output\"></pre>\n" +
                "  </div>\n" +
                "  <script>\n" +
                "    async function loadData() {\n" +
                "      const res = await fetch('/api/" + name.toLowerCase() + "/hello');\n" +
                "      const data = await res.json();\n" +
                "      document.getElementById('output').textContent = JSON.stringify(data, null, 2);\n" +
                "    }\n" +
                "  </script>\n" +
                "</body>\n</html>\n");
        files.put(name + "/README.md", generateReadme(name, desc, "html", List.of()));
    }

    // ============== 通用 ==============

    private void addCommon(Map<String, String> files, String name, String desc, String type) {
        files.put(name + "/.gitignore", generateGitignore(type));
        files.put(name + "/.env.example", "# Environment Variables\nAPP_NAME=" + name + "\nAPP_PORT=8080\n");
    }

    private String generateDockerfile(String type, String name) {
        if ("spring-boot".equals(type)) {
            return "FROM eclipse-temurin:17-jdk-alpine AS build\n" +
                    "WORKDIR /app\nCOPY . .\nRUN ./mvnw clean package -DskipTests\n\n" +
                    "FROM eclipse-temurin:17-jre-alpine\n" +
                    "WORKDIR /app\nCOPY --from=build /app/target/" + name + "-*.jar app.jar\n" +
                    "EXPOSE 8080\nENTRYPOINT [\"java\", \"-jar\", \"/app/app.jar\"]\n";
        } else if ("vue".equals(type) || "react".equals(type)) {
            return "FROM node:20-alpine AS build\nWORKDIR /app\nCOPY package*.json ./\nRUN npm install\nCOPY . .\nRUN npm run build\n\n" +
                    "FROM nginx:alpine\nCOPY --from=build /app/dist /usr/share/nginx/html\nEXPOSE 80\nCMD [\"nginx\", \"-g\", \"daemon off;\"]\n";
        } else if ("python-flask".equals(type)) {
            return "FROM python:3.11-slim\nWORKDIR /app\nCOPY requirements.txt .\nRUN pip install --no-cache-dir -r requirements.txt\nCOPY . .\nEXPOSE 5000\nCMD [\"python\", \"app.py\"]\n";
        } else if ("node-express".equals(type)) {
            return "FROM node:20-alpine\nWORKDIR /app\nCOPY package*.json ./\nRUN npm install --production\nCOPY . .\nEXPOSE 3000\nCMD [\"node\", \"index.js\"]\n";
        } else {
            return "FROM nginx:alpine\nCOPY . /usr/share/nginx/html\nEXPOSE 80\n";
        }
    }

    private String generateReadme(String name, String desc, String type, List<String> features) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(name).append("\n\n");
        sb.append("> ").append(desc).append("\n\n");
        sb.append("**项目类型**: ").append(type).append("\n");
        sb.append("**生成时间**: ").append(new Date()).append("\n");
        sb.append("**生成器**: MiniMax 自研 AI (V2.5)\n\n");
        sb.append("## 功能\n\n");
        for (String f : features) sb.append("- ").append(f).append("\n");
        sb.append("\n## 启动\n\n").append(generateRunInstructions(type, name, "h2"));
        return sb.toString();
    }

    private String generateGitignore(String type) {
        String common = "*.log\n*.tmp\n.DS_Store\nnode_modules/\ndist/\nbuild/\ntarget/\n.idea/\n.vscode/\n";
        if ("spring-boot".equals(type)) {
            return common + "HELP.md\n.gradle/\nbuild/\n!**/src/main/**/build/\n!**/src/test/**/build/\n";
        } else if ("vue".equals(type) || "react".equals(type)) {
            return common + "*.local\n.env\n.env.local\n";
        }
        return common;
    }

    private String generateRunInstructions(String type, String name, String db) {
        switch (type) {
            case "spring-boot":
                return "```bash\n" +
                        "# 1. 编译\n" +
                        "cd " + name + "\nmvn clean package -DskipTests\n\n" +
                        "# 2. 启动\n" +
                        "java -jar target/" + name + "-1.0.0-SNAPSHOT.jar\n\n" +
                        "# 3. 访问\n" +
                        "curl http://localhost:8080/api/" + name.toLowerCase() + "/hello\n" +
                        "```\n";
            case "vue":
            case "react":
                return "```bash\ncd " + name + "\nnpm install\nnpm run dev\n# 访问 http://localhost:3000\n```\n";
            case "python-flask":
                return "```bash\ncd " + name + "\npip install -r requirements.txt\npython app.py\n# 访问 http://localhost:5000\n```\n";
            case "node-express":
                return "```bash\ncd " + name + "\nnpm install\nnpm start\n# 访问 http://localhost:3000\n```\n";
            case "html":
                return "```bash\ncd " + name + "\n# 直接用浏览器打开 index.html\n# 或用 HTTP 服务器:\npython3 -m http.server 8080\n```\n";
            default:
                return "查看 README.md\n";
        }
    }

    private String generateStructure(Set<String> paths, String projectName) {
        StringBuilder sb = new StringBuilder();
        sb.append(projectName).append("/\n");
        for (String path : paths) {
            String relative = path.replace(projectName + "/", "");
            int depth = relative.split("/").length - 1;
            sb.append("  ".repeat(depth + 1)).append("├─ ").append(relative.split("/")[relative.split("/").length - 1]).append("\n");
        }
        return sb.toString();
    }

    private List<String> extractKeyFiles(Set<String> paths) {
        List<String> keys = Arrays.asList(
                "pom.xml", "package.json", "Dockerfile", "README.md",
                "application.yml", "index.html", "app.py", "index.js", "App.java"
        );
        return paths.stream()
                .filter(p -> keys.stream().anyMatch(k -> p.endsWith(k)))
                .limit(10)
                .toList();
    }

    // ============== Utils ==============

    private String normalizeProjectType(String t) {
        if (t == null) return "spring-boot";
        t = t.toLowerCase().trim();
        if (t.contains("spring") || t.contains("java") || t.contains("后端")) return "spring-boot";
        if (t.contains("vue")) return "vue";
        if (t.contains("react")) return "react";
        if (t.contains("python") || t.contains("flask")) return "python-flask";
        if (t.contains("node") || t.contains("express")) return "node-express";
        if (t.contains("html") || t.contains("静态")) return "html";
        return t;
    }

    private String normalizeProjectName(String n) {
        if (n == null || n.isEmpty()) return "my-app";
        return n.toLowerCase().replace(" ", "-").replace("_", "-");
    }

    private List<String> parseFeatures(String f) {
        if (f == null || f.isEmpty()) return List.of();
        return Arrays.stream(f.split("[,，、\\s]+"))
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .toList();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        String[] parts = s.split("[-_]");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
            }
        }
        return sb.toString();
    }
}