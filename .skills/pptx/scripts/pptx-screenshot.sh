#!/bin/bash
# pptx-screenshot.sh — Screenshot PPTX slides as PNG images
# CLOUD-OVERLAY: cloud sandbox-specific version. Base at
#   packages/daemon/skills/pptx/scripts/pptx-screenshot.sh
# Adds a Linux branch (soffice + pdftoppm) so the cloud sandbox can run the
# Template Imitation Workflow without macOS-only swiftc tooling. macOS
# branch is preserved verbatim for local-daemon parity.
set -euo pipefail

SKILL_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SWIFT_SRC="$SKILL_DIR/scripts/pdf2png.swift"
BINARY="$SKILL_DIR/scripts/pdf2png"

PAGES="1"
SCALE="2.0"
OUTDIR=""

usage() {
  cat >&2 <<EOF
Usage: pptx-screenshot.sh <pptx_file> [options]

Options:
  --pages <spec>    Page specification: "1", "1,3,5", "2-5", "all" (default: "1")
  --outdir <dir>    Output directory (default: same as input file)
  --scale <factor>  Rendering scale factor (default: 2.0)
  -h, --help        Show this help

Examples:
  pptx-screenshot.sh presentation.pptx
  pptx-screenshot.sh presentation.pptx --pages "1-3" --outdir /tmp/slides
  pptx-screenshot.sh presentation.pptx --pages all --scale 3.0
EOF
  exit 1
}

if [ $# -lt 1 ]; then
  usage
fi

PPTX_FILE="$1"
shift

while [ $# -gt 0 ]; do
  case "$1" in
    --pages)
      PAGES="$2"
      shift 2
      ;;
    --outdir)
      OUTDIR="$2"
      shift 2
      ;;
    --scale)
      SCALE="$2"
      shift 2
      ;;
    -h|--help)
      usage
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      ;;
  esac
done

if [ ! -f "$PPTX_FILE" ]; then
  echo "Error: file not found: $PPTX_FILE" >&2
  exit 1
fi

if [ -z "$OUTDIR" ]; then
  OUTDIR="$(dirname "$PPTX_FILE")"
fi

OS="$(uname -s)"

if ! command -v soffice &>/dev/null; then
  echo "Error: LibreOffice (soffice) not found." >&2
  case "$OS" in
    Darwin) echo "Install with: brew install --cask libreoffice" >&2 ;;
    Linux)  echo "Install with: apt-get install -y libreoffice-impress libreoffice-core" >&2 ;;
    *)      echo "Install LibreOffice for your platform." >&2 ;;
  esac
  exit 1
fi

TMPDIR=$(mktemp -d)
trap 'rm -rf "$TMPDIR"' EXIT

BASENAME="$(basename "$PPTX_FILE" | sed 's/\.[^.]*$//')"

echo "Converting PPTX to PDF..." >&2
soffice --headless --convert-to pdf --outdir "$TMPDIR" "$PPTX_FILE" >/dev/null 2>&1

PDF_FILE="$TMPDIR/$BASENAME.pdf"
if [ ! -f "$PDF_FILE" ]; then
  echo "Error: LibreOffice failed to convert $PPTX_FILE to PDF" >&2
  exit 1
fi

mkdir -p "$OUTDIR"

case "$OS" in
  Darwin)
    # macOS branch — Swift PDFKit renderer (legacy path, identical to base).
    if ! command -v swiftc &>/dev/null; then
      echo "Error: Swift compiler not found. Install Xcode Command Line Tools:" >&2
      echo "  xcode-select --install" >&2
      exit 1
    fi
    if [ ! -f "$BINARY" ] || [ "$SWIFT_SRC" -nt "$BINARY" ]; then
      echo "Compiling PDF renderer..." >&2
      swiftc "$SWIFT_SRC" -o "$BINARY" -O 2>&1
      echo "Compilation done." >&2
    fi
    "$BINARY" "$PDF_FILE" "$OUTDIR" --pages "$PAGES" --scale "$SCALE"
    ;;
  Linux|*)
    # Linux / cloud sandbox branch — poppler pdftoppm. Maps SCALE×72 to a DPI
    # so 2.0 → 144 dpi matches the default Swift PDFKit pixel density.
    if ! command -v pdftoppm &>/dev/null; then
      echo "Error: pdftoppm not found. Install poppler-utils:" >&2
      echo "  apt-get install -y poppler-utils" >&2
      exit 1
    fi
    DPI=$(awk -v s="$SCALE" 'BEGIN { printf "%d", s * 72 }')
    PREFIX="$OUTDIR/$BASENAME"
    case "$PAGES" in
      all)
        pdftoppm -r "$DPI" -png "$PDF_FILE" "$PREFIX"
        ;;
      *)
        # Support "1", "1,3,5", "2-5" — pdftoppm uses -f/-l for ranges, so
        # split comma-separated parts and dispatch each.
        IFS=',' read -ra PARTS <<< "$PAGES"
        for part in "${PARTS[@]}"; do
          if [[ "$part" == *-* ]]; then
            f="${part%-*}"
            l="${part##*-}"
            pdftoppm -f "$f" -l "$l" -r "$DPI" -png "$PDF_FILE" "$PREFIX"
          else
            pdftoppm -f "$part" -l "$part" -r "$DPI" -png "$PDF_FILE" "$PREFIX"
          fi
        done
        ;;
    esac
    ;;
esac
