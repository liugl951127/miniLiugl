package com.minimax.ai.tool.builtin.ppt;

import org.apache.poi.xslf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * PPT 渲染器 (V3.0.2 自研)
 *
 * <p>用 Apache POI 5.x (XSLF) 把 OutlineParser 输出渲染为 .pptx 二进制
 *
 * <h3>输出</h3>
 * byte[] — PPTX 文件二进制, 可直接保存或下载
 *
 * <h3>支持的 Slide 类型</h3>
 * <ul>
 *   <li>cover — 封面 (大标题 + 副标题, 居中)</li>
 *   <li>title — 章节扉页 (大标题, 居中)</li>
 *   <li>content — 内容页 (标题 + 要点列表)</li>
 *   <li>closing — 结尾 (Q&amp;A / 谢谢)</li>
 * </ul>
 *
 * <h3>每行注释</h3>
 * Apache POI API 复杂, 关键步骤都有解释
 */
public class PptRenderer {

    private static final Logger log = LoggerFactory.getLogger(PptRenderer.class);

    /**
     * 渲染 PPT → byte[]
     *
     * @param slides 大纲 slides
     * @param theme  主题配色
     * @return .pptx 二进制
     */
    public byte[] render(List<OutlineParser.Slide> slides, PptTheme theme) throws IOException {
        // 1. 创建空白演示文稿
        XMLSlideShow ppt = new XMLSlideShow();
        // 2. 设置幻灯片尺寸 16:9 (默认英寸单位)
        ppt.setPageSize(new Dimension(1280, 720));  // 10 寸 × 5.625 寸 @ 96 DPI
        // 3. 逐个 slide 渲染
        for (OutlineParser.Slide s : slides) {
            XSLFSlide slide = ppt.createSlide();
            applyLayout(ppt, slide, s, theme);   // 应用版式 + 内容
        }
        // 4. 序列化为字节数组
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ppt.write(baos);
            return baos.toByteArray();
        } finally {
            // 5. 释放资源
            ppt.close();
        }
    }

    /**
     * 应用版式 + 渲染内容
     *
     * @param ppt   演示文稿
     * @param slide 当前 slide
     * @param data  数据
     * @param theme 主题
     */
    private void applyLayout(XMLSlideShow ppt, XSLFSlide slide, OutlineParser.Slide data, PptTheme theme) {
        // 1. 设置背景色: 用全屏矩形文本框当背景
        XSLFTextBox bg = slide.createTextBox();
        bg.setAnchor(new Rectangle(0, 0, 1280, 720));  // 全屏
        XSLFTextParagraph bgP = bg.addNewTextParagraph();
        XSLFTextRun bgR = bgP.addNewTextRun();
        bgR.setText(" ");  // 空格占位
        bgR.setFontSize(1.0);
        if (bg instanceof XSLFSimpleShape) {
            XSLFSimpleShape ss = (XSLFSimpleShape) bg;
            ss.setFillColor(theme.getBackground());
            ss.setLineColor(theme.getBackground());
        }
        // 移到最后层级 (作为背景)
        slide.getShapes().remove(bg);  // 先移除
        // 重新添加到末尾 (Z 顺序)
        // 注: 简化处理, 让背景元素在最后画

        // 2. 根据 type 分发渲染逻辑
        switch (data.type) {
            case "cover" -> renderCover(slide, data, theme);
            case "title" -> renderTitle(slide, data, theme);
            case "closing" -> renderClosing(slide, data, theme);
            default -> renderContent(slide, data, theme);
        }
    }

    /**
     * 渲染封面: 大标题 + 副标题, 居中
     */
    private void renderCover(XSLFSlide slide, OutlineParser.Slide data, PptTheme theme) {
        // 1. 标题
        XSLFTextBox title = slide.createTextBox();
        title.setAnchor(new Rectangle(80, 240, 1120, 100));  // x, y, w, h
        XSLFTextParagraph tp = title.addNewTextParagraph();
        XSLFTextRun tr = tp.addNewTextRun();
        tr.setText(safe(data.title, "演示文稿"));
        tr.setFontSize(54.0);
        tr.setFontColor(theme.getTitle());
        tr.setBold(true);
        tr.setFontFamily("Microsoft YaHei");  // 中文字体

        // 2. 副标题
        if (data.subtitle != null) {
            XSLFTextBox sub = slide.createTextBox();
            sub.setAnchor(new Rectangle(80, 380, 1120, 60));
            XSLFTextParagraph sp = sub.addNewTextParagraph();
            XSLFTextRun sr = sp.addNewTextRun();
            sr.setText(data.subtitle);
            sr.setFontSize(24.0);
            sr.setFontColor(theme.getSubtitle());
            sr.setFontFamily("Microsoft YaHei");
        }
    }

    /**
     * 渲染章节扉页: 大标题, 居中, 无副标题
     */
    private void renderTitle(XSLFSlide slide, OutlineParser.Slide data, PptTheme theme) {
        // 复用 cover 渲染逻辑
        renderCover(slide, data, theme);
    }

    /**
     * 渲染内容页: 标题 + 要点列表
     */
    private void renderContent(XSLFSlide slide, OutlineParser.Slide data, PptTheme theme) {
        // 1. 顶部标题
        XSLFTextBox title = slide.createTextBox();
        title.setAnchor(new Rectangle(60, 40, 1160, 80));
        XSLFTextParagraph tp = title.addNewTextParagraph();
        XSLFTextRun tr = tp.addNewTextRun();
        tr.setText(safe(data.title, ""));
        tr.setFontSize(36.0);
        tr.setFontColor(theme.getTitle());
        tr.setBold(true);
        tr.setFontFamily("Microsoft YaHei");

        // 2. 装饰线 (强调色, 在标题下方) - 用矩形代替线 (XSLF 限制)
        XSLFTextBox line = slide.createTextBox();
        line.setAnchor(new Rectangle(60, 130, 200, 4));  // 200x4 细条
        XSLFTextParagraph lp = line.addNewTextParagraph();
        XSLFTextRun lr = lp.addNewTextRun();
        lr.setText(" ");
        lr.setFontSize(1.0);
        if (line instanceof XSLFSimpleShape) {
            XSLFSimpleShape ss = (XSLFSimpleShape) line;
            ss.setFillColor(theme.getAccent());
            ss.setLineColor(theme.getAccent());
        }
        line.setAnchor(new Rectangle(60, 130, 200, 0));  // 200px 宽水平线
        if (line instanceof XSLFSimpleShape) {
            XSLFSimpleShape ss = (XSLFSimpleShape) line;
            ss.setLineColor(theme.getAccent());
            ss.setLineWidth(3.0);
        }

        // 3. 要点列表
        if (data.bullets != null && !data.bullets.isEmpty()) {
            XSLFTextBox bullets = slide.createTextBox();
            bullets.setAnchor(new Rectangle(80, 170, 1120, 480));
            for (int i = 0; i < data.bullets.size(); i++) {
                String text = data.bullets.get(i);
                XSLFTextParagraph bp = bullets.addNewTextParagraph();
                bp.setIndentLevel(0);
                bp.setSpaceAfter(12.0);  // 段后间距
                // 4. 强调前 3 个要点 (前 3 用强调色)
                XSLFTextRun br = bp.addNewTextRun();
                br.setText("•  " + text);
                br.setFontSize(22.0);
                br.setFontColor(i < 3 ? theme.getBody() : theme.getSubtitle());
                br.setFontFamily("Microsoft YaHei");
            }
        } else if (data.subtitle != null) {
            // 5. 没有要点但有副标题: 显示副标题
            XSLFTextBox sub = slide.createTextBox();
            sub.setAnchor(new Rectangle(80, 200, 1120, 60));
            XSLFTextParagraph sp = sub.addNewTextParagraph();
            XSLFTextRun sr = sp.addNewTextRun();
            sr.setText(data.subtitle);
            sr.setFontSize(24.0);
            sr.setFontColor(theme.getSubtitle());
            sr.setFontFamily("Microsoft YaHei");
        }
    }

    /**
     * 渲染结尾页: 谢谢 / Q&A
     */
    private void renderClosing(XSLFSlide slide, OutlineParser.Slide data, PptTheme theme) {
        // 复用 cover 渲染 (大标题居中)
        renderCover(slide, data, theme);
    }

    /**
     * 安全取值 (null → 默认)
     */
    private String safe(String s, String def) {
        return s == null || s.isEmpty() ? def : s;
    }
}
