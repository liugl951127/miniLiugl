// Compile all slides into a single PPTX
const pptxgen = require("pptxgenjs");

const pres = new pptxgen();
pres.layout = "LAYOUT_16x9";
pres.author = "Mavis";
pres.title = "区块链 + 话术统一技术架构方案";
pres.subject = "双录一体化核心技术方案";

// Theme (same as before for consistency)
const theme = {
  primary:   "2b2d42",   // deep blue
  secondary: "8d99ae",   // medium gray
  accent:    "d90429",   // red highlight
  light:     "edf2f4",   // light gray
  bg:        "ffffff"   // white
};

// Load and create slides in order
for (let i = 1; i <= 19; i++) {
  const num = String(i).padStart(2, "0");
  const slideModule = require(`./slide-${num}.js`);
  slideModule.createSlide(pres, theme);
}

pres.writeFile({ fileName: "./output/blockchain-script-design.pptx" })
  .then(fn => console.log("Written: " + fn));
