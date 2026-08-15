const pptxgen = require('pptxgenjs');
const pres = new pptxgen();
pres.layout = 'LAYOUT_16x9';
pres.author = 'MiniMax Platform';
pres.title = '大模型技术详解 - LLM 原理 · 训练流程 · RAG · Agent';
pres.subject = '大模型知识点全面讲解';

// Theme
const theme = {
  primary: '1e40af',
  secondary: '475569',
  accent: '0891b2',
  light: 'e0f2fe',
  bg: 'f8fafc'
};

const slideCount = 10;
for (let i = 1; i <= slideCount; i++) {
  const num = String(i).padStart(2, '0');
  try {
    require(`./slide-llm-${num}.js`).createSlide(pres, theme);
    console.log(`✓ Slide ${num} loaded`);
  } catch (e) {
    console.error(`✗ Slide ${num} failed:`, e.message);
  }
}

pres.writeFile({ fileName: './output/llm-knowledge-detail.pptx' })
  .then(() => console.log('\n✅ Done: output/llm-knowledge-detail.pptx'))
  .catch(e => console.error('Write failed:', e));
