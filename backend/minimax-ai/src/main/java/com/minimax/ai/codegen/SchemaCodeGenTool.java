package com.minimax.ai.codegen;

import com.minimax.ai.datasource.MultiDataSourceManager;
import com.minimax.ai.entity.AiTool;
import com.minimax.ai.entity.DbDataSource;
import com.minimax.ai.mapper.DataSourceMapper;
import com.minimax.ai.tool.AiToolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.ByteArrayOutputStream;

/**
 * 从表结构生成 Spring Boot CRUD 代码 (V2.7)
 *
 * <p>输入: dataSourceId + table
 * 输出: 完整 Spring Boot 项目 ZIP, 含:
 *   - Entity (含 Lombok + Swagger 注解)
 *   - Mapper (MyBatis)
 *   - Service (含业务方法)
 *   - Controller (RESTful API)
 *   - DTO (请求/响应)
 *   - pom.xml
 *   - application.yml
 *   - README.md
 *   - Dockerfile
 *
 * <h3>算法</h3>
 * 1. 查 schema: 列名 + 类型 + 长度 + 注释 + 主键
 * 2. 类型映射: SQL -> Java (VARCHAR -> String, INT -> Integer, ...)
 * 3. 代码模板 (Freemarker 风格, 但纯 Java 字符串拼接)
 * 4. ZIP 打包返回
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaCodeGenTool implements AiToolExecutor {

    private final DataSourceMapper dataSourceMapper;
    private final MultiDataSourceManager dsManager;

    @Override
    public String getCode() {
        return "code.gen.from-schema";
    }

    @Override
    public Object execute(AiTool tool, Map<String, Object> input) throws Exception {
        Long dataSourceId = ((Number) input.get("dataSourceId")).longValue();
        String table = (String) input.get("table");
        String basePackage = (String) input.getOrDefault("basePackage", "com.example");
        String projectName = (String) input.getOrDefault("projectName", camelCase(table));
        String author = (String) input.getOrDefault("author", "MiniMax AI");
        String className = (String) input.getOrDefault("className", camelCase(table));

        DbDataSource ds = dataSourceMapper.selectById(dataSourceId);
        if (ds == null) throw new IllegalArgumentException("Data source not found");

        long start = System.currentTimeMillis();

        // 1. 查表结构
        List<ColumnInfo> columns = loadSchema(ds, table);
        if (columns.isEmpty()) throw new IllegalArgumentException("Table not found: " + table);

        // 2. 找主键
        ColumnInfo pk = columns.stream().filter(c -> c.isPrimaryKey).findFirst().orElse(columns.get(0));

        // 3. 生成代码
        Map<String, String> files = new LinkedHashMap<>();
        files.put("pom.xml", genPom(projectName));
        files.put("src/main/resources/application.yml", genAppYml(ds));
        files.put("src/main/java/" + basePackage.replace('.', '/') + "/" + className + ".java", genEntity(className, table, columns));
        files.put("src/main/java/" + basePackage.replace('.', '/') + "/mapper/" + className + "Mapper.java", genMapper(className, basePackage, table));
        files.put("src/main/java/" + basePackage.replace('.', '/') + "/service/" + className + "Service.java", genService(className, basePackage));
        files.put("src/main/java/" + basePackage.replace('.', '/') + "/controller/" + className + "Controller.java", genController(className, basePackage));
        files.put("src/main/resources/mapper/" + className + "Mapper.xml", genMapperXml(className, table, columns, pk));
        files.put("src/main/java/" + basePackage.replace('.', '/') + "/" + className + "Application.java", genApplication(basePackage));
        files.put("Dockerfile", genDockerfile());
        files.put("README.md", genReadme(projectName, className, author));
        files.put(".gitignore", genGitignore());

        // 4. ZIP 打包
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map.Entry<String, String> e : files.entrySet()) {
                zos.putNextEntry(new ZipEntry(projectName + "/" + e.getKey()));
                zos.write(e.getValue().getBytes("UTF-8"));
                zos.closeEntry();
            }
        }
        byte[] zip = baos.toByteArray();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("projectName", projectName);
        result.put("className", className);
        result.put("tableName", table);
        result.put("basePackage", basePackage);
        result.put("fileCount", files.size());
        result.put("zipBase64", Base64.getEncoder().encodeToString(zip));
        result.put("zipSize", zip.length);
        result.put("fileList", new ArrayList<>(files.keySet()));
        result.put("durationMs", System.currentTimeMillis() - start);
        return result;
    }

    // ========== Schema 加载 ==========

    private List<ColumnInfo> loadSchema(DbDataSource ds, String table) throws Exception {
        List<ColumnInfo> columns = new ArrayList<>();
        Set<String> pkColumns = new HashSet<>();
        javax.sql.DataSource ds_ = dsManager.getDataSource(ds);
        try (Connection conn = ds_.getConnection()) {
            // 主键
            try (ResultSet rs = conn.getMetaData().getPrimaryKeys(conn.getCatalog(), null, table)) {
                while (rs.next()) {
                    pkColumns.add(rs.getString("COLUMN_NAME"));
                }
            }
            // 列
            try (ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, table, "%")) {
                while (rs.next()) {
                    ColumnInfo c = new ColumnInfo();
                    c.name = rs.getString("COLUMN_NAME");
                    c.javaType = sqlToJava(rs.getString("TYPE_NAME"), rs.getInt("DATA_TYPE"));
                    c.jdbcType = rs.getString("TYPE_NAME");
                    c.size = rs.getInt("COLUMN_SIZE");
                    c.nullable = rs.getInt("NULLABLE") == 1;
                    c.comment = rs.getString("REMARKS");
                    c.isPrimaryKey = pkColumns.contains(c.name);
                    c.javaName = camelCaseLower(c.name);
                    c.javaTypeUpper = upperFirst(c.javaType);
                    columns.add(c);
                }
            }
        }
        return columns;
    }

    private static class ColumnInfo {
        String name;          // user_name
        String javaName;      // userName
        String javaType;      // String
        String javaTypeUpper; // String (保留大写开头, 用于包装类)
        String jdbcType;      // VARCHAR
        int size;
        boolean nullable;
        String comment;
        boolean isPrimaryKey;
    }

    // ========== 类型映射 ==========

    private String sqlToJava(String jdbcType, int dataType) {
        if (jdbcType == null) return "String";
        String t = jdbcType.toUpperCase();
        if (t.contains("VARCHAR") || t.contains("CHAR") || t.contains("TEXT")) return "String";
        if (t.contains("BIGINT")) return "Long";
        if (t.contains("INT")) return "Integer";
        if (t.contains("TINYINT") || t.contains("SMALLINT")) return "Integer";
        if (t.contains("DECIMAL") || t.contains("NUMERIC")) return "java.math.BigDecimal";
        if (t.contains("FLOAT") || t.contains("DOUBLE")) return "Double";
        if (t.contains("REAL")) return "Float";
        if (t.contains("BIT") || t.contains("BOOL")) return "Boolean";
        if (t.contains("DATE") || t.contains("TIME")) return "java.time.LocalDateTime";
        if (t.contains("TIMESTAMP")) return "java.time.LocalDateTime";
        if (t.contains("BLOB") || t.contains("BINARY")) return "byte[]";
        return "String";
    }

    // ========== 命名工具 ==========

    private String camelCase(String s) {
        if (s == null || s.isEmpty()) return s;
        String[] parts = s.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(upperFirst(p.toLowerCase()));
        }
        return sb.toString();
    }

    private String camelCaseLower(String s) {
        if (s == null || s.isEmpty()) return s;
        String[] parts = s.split("_");
        StringBuilder sb = new StringBuilder(parts[0].toLowerCase());
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            sb.append(upperFirst(parts[i].toLowerCase()));
        }
        return sb.toString();
    }

    private String upperFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ========== 代码生成 ==========

    private String genPom(String projectName) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project>\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>com.example</groupId>\n" +
                "    <artifactId>" + projectName + "</artifactId>\n" +
                "    <version>1.0.0</version>\n" +
                "    <parent>\n" +
                "        <groupId>org.springframework.boot</groupId>\n" +
                "        <artifactId>spring-boot-starter-parent</artifactId>\n" +
                "        <version>3.2.0</version>\n" +
                "    </parent>\n" +
                "    <dependencies>\n" +
                "        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>\n" +
                "        <dependency><groupId>org.mybatis.spring.boot</groupId><artifactId>mybatis-spring-boot-starter</artifactId><version>3.0.3</version></dependency>\n" +
                "        <dependency><groupId>com.mysql</groupId><artifactId>mysql-connector-j</artifactId></dependency>\n" +
                "        <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>\n" +
                "        <dependency><groupId>io.springfox</groupId><artifactId>springfox-boot-starter</artifactId><version>3.0.0</version></dependency>\n" +
                "    </dependencies>\n" +
                "    <build>\n" +
                "        <plugins><plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin></plugins>\n" +
                "    </build>\n" +
                "</project>\n";
    }

    private String genAppYml(DbDataSource ds) {
        return "server:\n  port: 8080\nspring:\n  datasource:\n    url: " + (ds.getJdbcUrl() != null ? ds.getJdbcUrl() : "jdbc:mysql://localhost:3306/demo") + "\n    username: " + ds.getUsername() + "\n    password: " + ds.getPassword() + "\nmybatis:\n  mapper-locations: classpath:mapper/*.xml\n";
    }

    private String genEntity(String className, String table, List<ColumnInfo> cols) {
        StringBuilder sb = new StringBuilder();
        sb.append("@lombok.Data @lombok.NoArgsConstructor @lombok.AllArgsConstructor @io.swagger.annotations.ApiModel(\"").append(className).append("\")\n");
        sb.append("public class ").append(className).append(" {\n\n");
        for (ColumnInfo c : cols) {
            if (c.comment != null && !c.comment.isEmpty()) {
                sb.append("    @io.swagger.annotations.ApiModelProperty(\"").append(c.comment).append("\")\n");
            }
            sb.append("    private ").append(c.javaType).append(" ").append(c.javaName).append(";\n\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private String genMapper(String className, String pkg, String table) {
        return "@org.apache.ibatis.annotations.Mapper\npublic interface " + className + "Mapper {\n" +
                "    java.util.List<" + className + "> list();\n" +
                "    " + className + " getById(Integer id);\n" +
                "    int insert(" + className + " entity);\n" +
                "    int update(" + className + " entity);\n" +
                "    int deleteById(Integer id);\n" +
                "}\n";
    }

    private String genMapperXml(String className, String table, List<ColumnInfo> cols, ColumnInfo pk) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">\n");
        sb.append("<mapper namespace=\"").append(className).append("Mapper\">\n");
        sb.append("    <resultMap id=\"").append(className).append("Map\" type=\"").append(className).append("\">\n");
        for (ColumnInfo c : cols) {
            sb.append("        <result column=\"").append(c.name).append("\" property=\"").append(c.javaName).append("\"/>\n");
        }
        sb.append("    </resultMap>\n\n");
        sb.append("    <select id=\"list\" resultMap=\"").append(className).append("Map\">SELECT * FROM ").append(table).append(" ORDER BY ").append(pk.name).append(" DESC</select>\n");
        sb.append("    <select id=\"getById\" resultMap=\"").append(className).append("Map\">SELECT * FROM ").append(table).append(" WHERE ").append(pk.name).append("=#{id}</select>\n");
        sb.append("    <insert id=\"insert\" parameterType=\"").append(className).append("\">INSERT INTO ").append(table);
        sb.append("(");
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(cols.get(i).name);
        }
        sb.append(") VALUES (");
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("#{").append(cols.get(i).javaName).append("}");
        }
        sb.append(")</insert>\n");
        sb.append("    <update id=\"update\" parameterType=\"").append(className).append("\">UPDATE ").append(table).append(" SET ");
        for (int i = 0; i < cols.size(); i++) {
            if (cols.get(i).isPrimaryKey) continue;
            if (i > 1) sb.append(",");
            sb.append(cols.get(i).name).append("=#{").append(cols.get(i).javaName).append("}");
        }
        sb.append(" WHERE ").append(pk.name).append("=#{").append(pk.javaName).append("}</update>\n");
        sb.append("    <delete id=\"deleteById\">DELETE FROM ").append(table).append(" WHERE ").append(pk.name).append("=#{id}</delete>\n");
        sb.append("</mapper>\n");
        return sb.toString();
    }

    private String genService(String className, String pkg) {
        return "@org.springframework.stereotype.Service\npublic class " + className + "Service {\n" +
                "    @org.springframework.beans.factory.annotation.Autowired private " + className + "Mapper mapper;\n" +
                "    public java.util.List<" + className + "> list() { return mapper.list(); }\n" +
                "    public " + className + " get(Integer id) { return mapper.getById(id); }\n" +
                "    public int create(" + className + " e) { return mapper.insert(e); }\n" +
                "    public int update(" + className + " e) { return mapper.update(e); }\n" +
                "    public int delete(Integer id) { return mapper.deleteById(id); }\n" +
                "}\n";
    }

    private String genController(String className, String pkg) {
        return "@RestController\n@RequestMapping(\"/api/" + camelCaseLower(className) + "s\")\n@Api(tags = \"" + className + " 管理\")\npublic class " + className + "Controller {\n" +
                "    @org.springframework.beans.factory.annotation.Autowired private " + className + "Service service;\n" +
                "    @GetMapping public com.minimax.common.result.Result<List<" + className + ">> list() { return com.minimax.common.result.Result.ok(service.list()); }\n" +
                "    @GetMapping(\"/{id}\") public com.minimax.common.result.Result<" + className + "> get(@org.springframework.web.bind.annotation.PathVariable Integer id) { return com.minimax.common.result.Result.ok(service.get(id)); }\n" +
                "    @PostMapping public com.minimax.common.result.Result<Integer> create(@org.springframework.web.bind.annotation.RequestBody " + className + " e) { return com.minimax.common.result.Result.ok(service.create(e)); }\n" +
                "    @PutMapping public com.minimax.common.result.Result<Integer> update(@org.springframework.web.bind.annotation.RequestBody " + className + " e) { return com.minimax.common.result.Result.ok(service.update(e)); }\n" +
                "    @DeleteMapping(\"/{id}\") public com.minimax.common.result.Result<Integer> delete(@org.springframework.web.bind.annotation.PathVariable Integer id) { return com.minimax.common.result.Result.ok(service.delete(id)); }\n" +
                "}\n";
    }

    private String genApplication(String pkg) {
        String name = pkg.split("\\.")[pkg.split("\\.").length - 1];
        return "package " + pkg + ";\nimport org.mybatis.spring.annotation.MapperScan;\nimport org.springframework.boot.SpringApplication;\nimport org.springframework.boot.autoconfigure.SpringBootApplication;\n@MapperScan(\"" + pkg + ".mapper\")\n@SpringBootApplication\npublic class " + upperFirst(name) + "Application {\n    public static void main(String[] args) { SpringApplication.run(" + upperFirst(name) + "Application.class, args); }\n}\n";
    }

    private String genDockerfile() {
        return "FROM openjdk:17-jdk-slim\nWORKDIR /app\nCOPY target/*.jar app.jar\nEXPOSE 8080\nENTRYPOINT [\"java\", \"-jar\", \"app.jar\"]\n";
    }

    private String genReadme(String projectName, String className, String author) {
        return "# " + projectName + "\n\n由 MiniMax AI 自动生成 (作者: " + author + ")\n\n## 启动\n\n```bash\nmvn spring-boot:run\n```\n\n## API\n\n- `GET /api/" + camelCaseLower(className) + "s` - 列表\n- `GET /api/" + camelCaseLower(className) + "s/{id}` - 详情\n- `POST /api/" + camelCaseLower(className) + "s` - 新增\n- `PUT /api/" + camelCaseLower(className) + "s` - 更新\n- `DELETE /api/" + camelCaseLower(className) + "s/{id}` - 删除\n";
    }

    private String genGitignore() {
        return "target/\n*.class\n*.jar\n.idea/\n.vscode/\n*.iml\n.DS_Store\n";
    }
}
