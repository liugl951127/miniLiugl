// Compile all slides into a single PPTX
const pptxgen = require("pptxgenjs");

const pres = new pptxgen();
pres.layout = "LAYOUT_16x9";
pres.author = "Mavis";
pres.title = "线上线下双录一体化方案";
pres.subject = "合规升级 · 痛点驱动设计";

// Theme: Business & Authority (dark blue + red accent)
const theme = {
  primary:   "2b2d42",  // deep blue - main text/headers
  secondary: "8d99ae",  // medium gray - secondary text
  accent:    "d90429",  // strong red - highlight/key data
  light:     "edf2f4",  // light gray - card backgrounds
  bg:        "ffffff"   // white - slide background
};

// Load and create slides in order
for (let i = 1; i <= 16; i++) {
  const num = String(i).padStart(2, "0");
  const slideModule = require(`./slide-${num}.js`);
  slideModule.createSlide(pres, theme);
}

pres.writeFile({ fileName: "./output/double-record-integration-design.pptx" })
  .then(fn => console.log("Written: " + fn));
