const pptxgen = require('pptxgenjs');
const pres = new pptxgen();
pres.layout = 'LAYOUT_16x9';
pres.author = 'MiniMax Platform';
pres.title = 'MiniMax Platform V6.8 - 项目功能与架构概览';
pres.subject = '14 微服务 · 29 前端页面 · 模块关联 · 执行流程';

// Theme: Deep Blue professional
const theme = {
  primary: '1e40af',    // Deep blue
  secondary: '475569',  // Slate gray
  accent: '0891b2',     // Cyan/teal
  light: 'e0f2fe',      // Light blue
  bg: 'f8fafc'         // Off-white
};

// Load all slides
const slideCount = 15;
for (let i = 1; i <= slideCount; i++) {
  const num = String(i).padStart(2, '0');
  try {
    require(`./slide-${num}.js`).createSlide(pres, theme);
    console.log(`✓ Slide ${num} loaded`);
  } catch (e) {
    console.error(`✗ Slide ${num} failed:`, e.message);
  }
}

pres.writeFile({ fileName: './output/minimax-v68-overview.pptx' })
  .then(() => console.log('\n✅ Done: output/minimax-v68-overview.pptx'))
  .catch(e => console.error('Write failed:', e));
